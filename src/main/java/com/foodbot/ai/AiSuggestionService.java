package com.foodbot.ai;

import com.foodbot.lang.Lang;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Last-resort fallback for /cook: when nothing in foods.json matches what the user has,
 * ask Claude for a one-off suggestion instead of just telling them nothing fits.
 */
public class AiSuggestionService {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public AiSuggestionService(String apiKey) {
        this(apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
    }

    AiSuggestionService(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> suggest(List<String> haveIngredients, int timeMinutes, boolean canShop, Lang lang) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            String prompt = buildPrompt(haveIngredients, timeMinutes, canShop, lang);
            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("max_tokens", 400);
            body.addProperty("system", systemPrompt(lang));
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
            messages.add(message);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            return parseText(response.body());
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    static String systemPrompt(Lang lang) {
        String languageInstruction = lang == Lang.FA
                ? "Respond only in Persian (Farsi)."
                : "Respond only in English.";
        return "You are a home-cooking assistant inside a Telegram bot. " + languageInstruction
                + " Suggest exactly one simple, realistic dish. Keep the whole reply under 80 words, "
                + "plain text only (no markdown, no headings, no asterisks). Give the dish name and a "
                + "short numbered list of steps.";
    }

    static String buildPrompt(List<String> haveIngredients, int timeMinutes, boolean canShop, Lang lang) {
        String haveList = haveIngredients.isEmpty()
                ? (lang == Lang.FA ? "هیچ‌چیز خاصی" : "nothing in particular")
                : haveIngredients.stream().collect(Collectors.joining(", "));
        StringBuilder prompt = new StringBuilder();
        prompt.append("I have about ").append(timeMinutes).append(" minutes to cook. ");
        prompt.append("Ingredients I currently have: ").append(haveList).append(". ");
        if (canShop) {
            prompt.append("I can go buy a few missing things if needed, but keep the shopping list minimal. ");
        } else {
            prompt.append("I cannot go shopping, so only use what I already have. ");
        }
        prompt.append("What's one thing I can cook?");
        return prompt.toString();
    }

    private Optional<String> parseText(String responseBody) {
        JsonObject root = gson.fromJson(responseBody, JsonObject.class);
        if (root == null || !root.has("content") || !root.get("content").isJsonArray()) {
            return Optional.empty();
        }
        com.google.gson.JsonArray content = root.getAsJsonArray("content");
        if (content.isEmpty()) {
            return Optional.empty();
        }
        JsonObject firstBlock = content.get(0).getAsJsonObject();
        if (!firstBlock.has("text")) {
            return Optional.empty();
        }
        String text = firstBlock.get("text").getAsString().trim();
        return text.isEmpty() ? Optional.empty() : Optional.of(text);
    }
}
