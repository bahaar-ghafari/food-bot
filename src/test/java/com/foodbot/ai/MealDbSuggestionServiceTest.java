package com.foodbot.ai;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealDbSuggestionServiceTest {

    @Test
    void picksProteinOverCarbAndOtherIngredients() {
        assertEquals(Optional.of("chicken"),
                MealDbSuggestionService.pickQueryIngredient(List.of("onion", "rice", "chicken")));
    }

    @Test
    void fallsBackToCarbWhenNoProtein() {
        assertEquals(Optional.of("rice"), MealDbSuggestionService.pickQueryIngredient(List.of("onion", "rice")));
    }

    @Test
    void fallsBackToFirstIngredientWhenNeitherProteinNorCarb() {
        assertEquals(Optional.of("onion"), MealDbSuggestionService.pickQueryIngredient(List.of("onion", "turmeric")));
    }

    @Test
    void emptyWhenNoIngredientsAtAll() {
        assertEquals(Optional.empty(), MealDbSuggestionService.pickQueryIngredient(List.of()));
    }

    @Test
    void formatMealIncludesNameIngredientsAndInstructions() {
        JsonObject meal = new JsonObject();
        meal.addProperty("strMeal", "Chicken Handi");
        meal.addProperty("strIngredient1", "Chicken");
        meal.addProperty("strMeasure1", "500g");
        meal.addProperty("strIngredient2", "");
        meal.addProperty("strInstructions", "Cook the chicken until done.");
        meal.addProperty("strSource", "https://example.com/recipe");

        String formatted = MealDbSuggestionService.formatMeal(meal);

        assertTrue(formatted.contains("Chicken Handi"));
        assertTrue(formatted.contains("- Chicken (500g)"));
        assertTrue(formatted.contains("Cook the chicken until done."));
        assertTrue(formatted.contains("https://example.com/recipe"));
    }

    @Test
    void formatMealTruncatesLongInstructions() {
        JsonObject meal = new JsonObject();
        meal.addProperty("strMeal", "Long Recipe");
        meal.addProperty("strInstructions", "x".repeat(1000));

        String formatted = MealDbSuggestionService.formatMeal(meal);

        assertTrue(formatted.contains("..."));
        assertTrue(formatted.length() < 1000);
    }
}
