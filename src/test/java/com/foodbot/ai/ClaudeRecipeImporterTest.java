package com.foodbot.ai;

import com.foodbot.lang.Lang;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeRecipeImporterTest {
    private static final String DRAFT = "{\"is_recipe\":true,\"name\":\"Lentil soup\",\"prep_time_minutes\":40,"
            + "\"category\":\"MainCourse\",\"ingredients\":[{\"name\":\"lentil\",\"amount\":\"200 g\"},"
            + "{\"name\":\"onion\",\"amount\":\"\"}],\"steps\":[\"Fry the onion.\",\"Add lentils and water.\"]}";

    private static String response(String stopReason, String text) {
        JsonObject thinking = new JsonObject();
        thinking.addProperty("type", "thinking");
        thinking.addProperty("thinking", "");
        JsonObject textBlock = new JsonObject();
        textBlock.addProperty("type", "text");
        textBlock.addProperty("text", text);
        JsonArray content = new JsonArray();
        content.add(thinking);
        content.add(textBlock);
        JsonObject root = new JsonObject();
        root.add("content", content);
        root.addProperty("stop_reason", stopReason);
        return root.toString();
    }

    @Test
    void disabledWithoutApiKey() {
        ClaudeRecipeImporter importer = new ClaudeRecipeImporter(" ");
        assertFalse(importer.isEnabled());
        assertEquals(Optional.empty(), importer.fromText("recipe", Lang.EN, List.of()));
        assertTrue(new ClaudeRecipeImporter("sk-test").isEnabled());
    }

    @Test
    void parsesTheDraftAfterAThinkingBlock() {
        ImportedRecipe r = ClaudeRecipeImporter.parseResponse(response("end_turn", DRAFT)).orElseThrow();

        assertEquals("Lentil soup", r.name());
        assertEquals(40, r.prepTimeMinutes());
        assertEquals("MainCourse", r.category());
        assertEquals(List.of(new ImportedRecipe.Ingredient("lentil", "200 g"),
                new ImportedRecipe.Ingredient("onion", "")), r.ingredients());
        assertEquals(List.of("Fry the onion.", "Add lentils and water."), r.steps());
    }

    @Test
    void rejectsRefusalsCutOffRepliesAndNonRecipes() {
        assertTrue(ClaudeRecipeImporter.parseResponse(response("refusal", DRAFT)).isEmpty());
        assertTrue(ClaudeRecipeImporter.parseResponse(response("max_tokens", "{\"is_recipe\":tr")).isEmpty());
        assertTrue(ClaudeRecipeImporter.parseResponse(response("end_turn",
                "{\"is_recipe\":false,\"name\":\"\",\"prep_time_minutes\":0,\"category\":\"Other\","
                        + "\"ingredients\":[],\"steps\":[]}")).isEmpty());
        assertTrue(ClaudeRecipeImporter.parseResponse(response("end_turn",
                DRAFT.replace("\"Lentil soup\"", "\"\""))).isEmpty());
        assertTrue(ClaudeRecipeImporter.parseResponse("not json").isEmpty());
    }

    @Test
    void requestUsesStructuredOutputWithTheCategoryList() {
        JsonObject body = new ClaudeRecipeImporter("sk-test").buildRequest(new JsonArray(), Lang.EN, List.of());

        assertEquals("claude-opus-5-5", body.get("model").getAsString());
        JsonObject outputConfig = body.getAsJsonObject("output_config");
        assertEquals("low", outputConfig.get("effort").getAsString());
        JsonObject format = outputConfig.getAsJsonObject("format");
        assertEquals("json_schema", format.get("type").getAsString());
        JsonObject schema = format.getAsJsonObject("schema");
        assertFalse(schema.get("additionalProperties").getAsBoolean());
        assertEquals(6, schema.getAsJsonArray("required").size());
        assertEquals(6, schema.getAsJsonObject("properties").getAsJsonObject("category")
                .getAsJsonArray("enum").size());
        assertEquals("default", body.get("fallbacks").getAsString());
    }

    @Test
    void systemPromptSetsTheLanguageAndListsKnownIngredients() {
        String fa = ClaudeRecipeImporter.systemPrompt(Lang.FA, List.of("پیاز", "برنج"));
        assertTrue(fa.contains("Persian"));
        assertTrue(fa.contains("پیاز, برنج"));

        String en = ClaudeRecipeImporter.systemPrompt(Lang.EN, List.of());
        assertTrue(en.contains("English"));
        assertFalse(en.contains("already used"));
    }
}
