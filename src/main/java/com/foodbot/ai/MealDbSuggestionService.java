package com.foodbot.ai;

import com.foodbot.food.IngredientCategories;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Free, no-signup last-resort /cook suggestion: looks up a real recipe from TheMealDB's
 * public API using its shared "1" test key (documented at themealdb.com/api.php as free
 * for personal/low-volume use, no registration needed) built around one ingredient the
 * user already has.
 */
public class MealDbSuggestionService {
    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";
    private static final int MAX_INSTRUCTIONS_LENGTH = 600;

    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public MealDbSuggestionService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
    }

    MealDbSuggestionService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Optional<String> suggest(List<String> haveIngredients) {
        Optional<String> queryIngredient = pickQueryIngredient(haveIngredients);
        if (queryIngredient.isEmpty()) {
            return Optional.empty();
        }
        try {
            return findMealId(queryIngredient.get()).flatMap(this::lookupMeal);
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    static Optional<String> pickQueryIngredient(List<String> haveIngredients) {
        return IngredientCategories.pickProteinOrCarb(haveIngredients)
                .or(() -> haveIngredients.stream().findFirst());
    }

    private Optional<String> findMealId(String ingredient) throws IOException, InterruptedException {
        String normalized = ingredient.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        JsonObject root = get(BASE_URL + "filter.php?i=" + URLEncoder.encode(normalized, StandardCharsets.UTF_8));
        JsonArray meals = mealsArray(root);
        if (meals == null || meals.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(meals.get(0).getAsJsonObject().get("idMeal").getAsString());
    }

    private Optional<String> lookupMeal(String id) {
        try {
            JsonObject root = get(BASE_URL + "lookup.php?i=" + URLEncoder.encode(id, StandardCharsets.UTF_8));
            JsonArray meals = mealsArray(root);
            if (meals == null || meals.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(formatMeal(meals.get(0).getAsJsonObject()));
        } catch (IOException | InterruptedException e) {
            if (Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    private JsonArray mealsArray(JsonObject root) {
        if (root == null || !root.has("meals") || root.get("meals").isJsonNull()) {
            return null;
        }
        return root.getAsJsonArray("meals");
    }

    private JsonObject get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        return gson.fromJson(response.body(), JsonObject.class);
    }

    static String formatMeal(JsonObject meal) {
        StringBuilder sb = new StringBuilder(meal.get("strMeal").getAsString()).append("\n\n");
        for (int i = 1; i <= 20; i++) {
            String ingredient = textOf(meal, "strIngredient" + i);
            if (ingredient == null || ingredient.isBlank()) {
                continue;
            }
            String measure = textOf(meal, "strMeasure" + i);
            sb.append("- ").append(ingredient.trim());
            if (measure != null && !measure.isBlank()) {
                sb.append(" (").append(measure.trim()).append(")");
            }
            sb.append("\n");
        }
        String instructions = textOf(meal, "strInstructions");
        if (instructions != null && !instructions.isBlank()) {
            String trimmed = instructions.trim();
            if (trimmed.length() > MAX_INSTRUCTIONS_LENGTH) {
                trimmed = trimmed.substring(0, MAX_INSTRUCTIONS_LENGTH) + "...";
            }
            sb.append("\n").append(trimmed);
        }
        String source = textOf(meal, "strSource");
        if (source == null || source.isBlank()) {
            source = textOf(meal, "strYoutube");
        }
        if (source != null && !source.isBlank()) {
            sb.append("\n\n").append(source.trim());
        }
        return sb.toString().trim();
    }

    private static String textOf(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
