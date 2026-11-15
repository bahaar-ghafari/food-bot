package com.foodbot.importer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportedRecipeTest {
    private static ImportedRecipe recipe(String name, int minutes, String category,
                                         List<ImportedRecipe.Ingredient> ingredients, List<String> steps) {
        return new ImportedRecipe(name, minutes, category, ingredients, steps);
    }

    private static ImportedRecipe.Ingredient ing(String name, String amount) {
        return new ImportedRecipe.Ingredient(name, amount);
    }

    @Test
    void reusesKnownIngredientSpellingAndDropsDuplicatesAndBlanks() {
        ImportedRecipe r = recipe(" Soup ", 20, "MainCourse",
                List.of(ing("Onion", "1"), ing("onion", "2"), ing(" ", "x"), ing("leek", null)), List.of("Cook."))
                .normalized(List.of("onion", "rice"), 150, 30);

        assertEquals("Soup", r.name());
        assertEquals(List.of(ing("onion", "1"), ing("leek", "")), r.ingredients());
    }

    @Test
    void unknownCategoryBecomesOtherAndCaseIsFixed() {
        assertEquals("Other", recipe("A", 5, "Lunch", List.of(), List.of()).normalized(List.of(), 150, 30).category());
        assertEquals("Dessert", recipe("A", 5, "dessert", List.of(), List.of()).normalized(List.of(), 150, 30)
                .category());
    }

    @Test
    void prepTimeIsDefaultedAndCapped() {
        assertEquals(ImportedRecipe.DEFAULT_PREP_MINUTES,
                recipe("A", 0, "Other", List.of(), List.of()).normalized(List.of(), 150, 30).prepTimeMinutes());
        assertEquals(ImportedRecipe.MAX_PREP_MINUTES,
                recipe("A", 99999, "Other", List.of(), List.of()).normalized(List.of(), 150, 30).prepTimeMinutes());
    }

    @Test
    void longAmountsAndNamesAreTrimmedToTheLimits() {
        ImportedRecipe r = recipe("N".repeat(100), 5, "Other", List.of(ing("salt", "a".repeat(50))), List.of())
                .normalized(List.of(), 150, 30);
        assertEquals(ImportedRecipe.MAX_NAME_LENGTH, r.name().length());
        assertEquals(30, r.ingredients().get(0).amount().length());
    }

    @Test
    void longStepsAreSplitAtSentenceEndsWithinTheLimit() {
        String step = "Chop the onions finely. " + "Fry them slowly in butter until golden and soft. "
                + "Add the rice.";
        List<String> parts = ImportedRecipe.splitStep(step, 60);

        assertEquals(List.of("Chop the onions finely.", "Fry them slowly in butter until golden and soft.",
                "Add the rice."), parts);
    }

    @Test
    void stepsWithoutPunctuationAreSplitAtSpacesAndNeverExceedTheLimit() {
        String step = "word ".repeat(80).trim();
        List<String> parts = ImportedRecipe.splitStep(step, 150);

        assertTrue(parts.size() > 1);
        parts.forEach(p -> assertTrue(p.length() <= 150, p));
        assertEquals(step, String.join(" ", parts));
    }

    @Test
    void blankStepsAreDropped() {
        ImportedRecipe r = recipe("A", 5, "Other", List.of(), java.util.Arrays.asList("Boil.", " ", null))
                .normalized(List.of(), 150, 30);
        assertEquals(List.of("Boil."), r.steps());
    }
}
