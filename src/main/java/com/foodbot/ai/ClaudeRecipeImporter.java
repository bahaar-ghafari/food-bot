package com.foodbot.ai;

import com.foodbot.food.FoodCategories;
import com.foodbot.lang.Lang;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Reads a recipe the user pasted or screenshotted and returns it as structured fields, using Claude with a
 * JSON-schema constrained response so the reply is always parseable.
 */
public class ClaudeRecipeImporter implements RecipeImporter {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-opus-5-5";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    /** Lets the API retry a safety-classifier decline on another model instead of failing outright. */
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";
    /** Caps how many existing ingredient names go into the prompt, to keep it small. */
    static final int MAX_KNOWN_INGREDIENTS = 300;

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public ClaudeRecipeImporter(String apiKey) {
        this(apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    ClaudeRecipeImporter(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public Optional<ImportedRecipe> fromText(String text, Lang lang, List<String> knownIngredients) {
        JsonArray content = new JsonArray();
        content.add(textBlock("Here is the recipe:\n\n" + text));
        return call(content, lang, knownIngredients);
    }

    @Override
    public Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                              List<String> knownIngredients) {
        JsonObject source = new JsonObject();
        source.addProperty("type", "base64");
        source.addProperty("media_type", mediaType);
        source.addProperty("data", Base64.getEncoder().encodeToString(image));
        JsonObject imageBlock = new JsonObject();
        imageBlock.addProperty("type", "image");
        imageBlock.add("source", source);

        JsonArray content = new JsonArray();
        content.add(imageBlock);
        String prompt = "The recipe is in this image.";
        if (caption != null && !caption.isBlank()) {
            prompt += " The user also wrote: " + caption;
        }
        content.add(textBlock(prompt));
        return call(content, lang, knownIngredients);
    }

    private Optional<ImportedRecipe> call(JsonArray userContent, Lang lang, List<String> knownIngredients) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(120))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("anthropic-beta", FALLBACK_BETA)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(buildRequest(userContent, lang,
                            knownIngredients))))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Recipe import failed: HTTP " + response.statusCode() + " " + response.body());
                return Optional.empty();
            }
            return parseResponse(response.body());
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("Recipe import failed: " + e);
            return Optional.empty();
        }
    }

    JsonObject buildRequest(JsonArray userContent, Lang lang, List<String> knownIngredients) {
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.add("schema", schema());
        JsonObject outputConfig = new JsonObject();
        outputConfig.addProperty("effort", "low");
        outputConfig.add("format", format);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.add("content", userContent);
        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("max_tokens", 16000);
        body.addProperty("system", systemPrompt(lang, knownIngredients));
        body.add("output_config", outputConfig);
        body.addProperty("fallbacks", "default");
        body.add("messages", messages);
        return body;
    }

    static String systemPrompt(Lang lang, List<String> knownIngredients) {
        String language = lang == Lang.FA ? "Persian (Farsi)" : "English";
        StringBuilder prompt = new StringBuilder();
        prompt.append("You extract recipes for a home-cooking Telegram bot. The user pasted a recipe or sent a ")
                .append("screenshot of one (it may come from a website, a chat, a social media post or a cookbook). ")
                .append("Fill in the schema from it.\n\n")
                .append("- Write every field (name, ingredient names, amounts, steps) in ").append(language)
                .append(", translating if the source is in another language.\n")
                .append("- name: the dish's short, common name, without words like \"recipe\" or \"easy\".\n")
                .append("- prep_time_minutes: total time to make it, in minutes. If the source doesn't say, ")
                .append("give a realistic estimate.\n")
                .append("- category: the best fit.\n")
                .append("- ingredients: one entry per ingredient, name in singular base form (\"onion\", not ")
                .append("\"2 chopped onions\"), quantity and unit in amount (e.g. \"2\", \"200 g\", \"1 tbsp\"), ")
                .append("or an empty amount when none is given. Skip garnishes the source calls optional only if ")
                .append("they are not in the ingredient list.\n")
                .append("- steps: the method as short, separate steps in order, each under 150 characters, ")
                .append("without numbering. Keep the source's instructions; don't invent extra steps.\n")
                .append("- is_recipe: false if there is no recipe in the input (then leave the other fields empty).");
        List<String> sample = knownIngredients.size() > MAX_KNOWN_INGREDIENTS
                ? knownIngredients.subList(0, MAX_KNOWN_INGREDIENTS) : knownIngredients;
        if (!sample.isEmpty()) {
            prompt.append("\n\nIngredient names already used in the bot. When an ingredient is one of these, use ")
                    .append("exactly this spelling: ").append(String.join(", ", sample));
        }
        return prompt.toString();
    }

    static JsonObject schema() {
        JsonObject ingredient = object();
        ingredient.getAsJsonObject("properties").add("name", type("string"));
        ingredient.getAsJsonObject("properties").add("amount", type("string"));
        ingredient.add("required", strings(List.of("name", "amount")));

        JsonObject category = type("string");
        category.add("enum", strings(FoodCategories.ALL));

        JsonObject root = object();
        JsonObject properties = root.getAsJsonObject("properties");
        properties.add("is_recipe", type("boolean"));
        properties.add("name", type("string"));
        properties.add("prep_time_minutes", type("integer"));
        properties.add("category", category);
        properties.add("ingredients", arrayOf(ingredient));
        properties.add("steps", arrayOf(type("string")));
        root.add("required", strings(List.of("is_recipe", "name", "prep_time_minutes", "category", "ingredients",
                "steps")));
        return root;
    }

    /** Empty on a refusal, a cut-off reply, "not a recipe", or a draft with no name or ingredients. */
    static Optional<ImportedRecipe> parseResponse(String responseBody) {
        try {
            JsonObject root = new Gson().fromJson(responseBody, JsonObject.class);
            if (root == null || !root.has("content") || !root.get("content").isJsonArray()) {
                return Optional.empty();
            }
            String stopReason = root.has("stop_reason") && !root.get("stop_reason").isJsonNull()
                    ? root.get("stop_reason").getAsString() : "";
            if (stopReason.equals("refusal") || stopReason.equals("max_tokens")) {
                return Optional.empty();
            }
            String json = null;
            for (JsonElement block : root.getAsJsonArray("content")) {
                JsonObject b = block.getAsJsonObject();
                if (b.has("type") && b.get("type").getAsString().equals("text")) {
                    json = b.get("text").getAsString();
                    break;
                }
            }
            if (json == null) {
                return Optional.empty();
            }
            JsonObject draft = new Gson().fromJson(json, JsonObject.class);
            if (draft == null || !draft.has("is_recipe") || !draft.get("is_recipe").getAsBoolean()) {
                return Optional.empty();
            }
            List<ImportedRecipe.Ingredient> ingredients = new ArrayList<>();
            for (JsonElement e : draft.getAsJsonArray("ingredients")) {
                JsonObject i = e.getAsJsonObject();
                ingredients.add(new ImportedRecipe.Ingredient(i.get("name").getAsString(),
                        i.has("amount") ? i.get("amount").getAsString() : ""));
            }
            List<String> steps = new ArrayList<>();
            for (JsonElement e : draft.getAsJsonArray("steps")) {
                steps.add(e.getAsString());
            }
            String name = draft.get("name").getAsString();
            if (name.isBlank() || ingredients.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ImportedRecipe(name, draft.get("prep_time_minutes").getAsInt(),
                    draft.get("category").getAsString(), ingredients, steps));
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException
                 | NullPointerException | ClassCastException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static JsonObject textBlock(String text) {
        JsonObject block = new JsonObject();
        block.addProperty("type", "text");
        block.addProperty("text", text);
        return block;
    }

    private static JsonObject object() {
        JsonObject o = type("object");
        o.add("properties", new JsonObject());
        o.addProperty("additionalProperties", false);
        return o;
    }

    private static JsonObject arrayOf(JsonObject items) {
        JsonObject a = type("array");
        a.add("items", items);
        return a;
    }

    private static JsonObject type(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        return o;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }
}
