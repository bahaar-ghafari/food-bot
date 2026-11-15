package com.foodbot.importer;

import com.foodbot.food.FoodCategories;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recipe draft extracted by {@link RecipeImporter}. {@link #normalized} makes it safe to drop straight into
 * the add-food review screen: a known category, a sane prep time, steps and amounts within the bot's limits,
 * and ingredient names that reuse the spelling of ones already in the list.
 */
public record ImportedRecipe(String name, int prepTimeMinutes, String category, List<Ingredient> ingredients,
                             List<String> steps) {

    public record Ingredient(String name, String amount) {
    }

    static final int DEFAULT_PREP_MINUTES = 30;
    static final int MAX_PREP_MINUTES = 24 * 60;
    static final int MAX_NAME_LENGTH = 60;

    /** Ingredients in order, with their (possibly empty) amount, deduplicated case-insensitively. */
    public Map<String, String> ingredientAmounts() {
        Map<String, String> amounts = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            amounts.put(ingredient.name(), ingredient.amount());
        }
        return amounts;
    }

    public ImportedRecipe normalized(List<String> knownIngredients, int stepCharLimit, int amountCharLimit) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            cleanName = cleanName.substring(0, MAX_NAME_LENGTH).trim();
        }
        int minutes = prepTimeMinutes <= 0 ? DEFAULT_PREP_MINUTES : Math.min(prepTimeMinutes, MAX_PREP_MINUTES);
        String cleanCategory = FoodCategories.ALL.stream()
                .filter(c -> c.equalsIgnoreCase(category == null ? "" : category.trim()))
                .findFirst().orElse("Other");

        List<Ingredient> cleanIngredients = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Ingredient ingredient : ingredients == null ? List.<Ingredient>of() : ingredients) {
            if (ingredient == null || ingredient.name() == null || ingredient.name().isBlank()) {
                continue;
            }
            String ingredientName = canonical(ingredient.name().trim(), knownIngredients);
            if (seen.stream().anyMatch(s -> s.equalsIgnoreCase(ingredientName))) {
                continue;
            }
            seen.add(ingredientName);
            String amount = ingredient.amount() == null ? "" : ingredient.amount().trim();
            if (amount.length() > amountCharLimit) {
                amount = amount.substring(0, amountCharLimit).trim();
            }
            cleanIngredients.add(new Ingredient(ingredientName, amount));
        }

        List<String> cleanSteps = new ArrayList<>();
        for (String step : steps == null ? List.<String>of() : steps) {
            if (step != null && !step.isBlank()) {
                cleanSteps.addAll(splitStep(step.trim(), stepCharLimit));
            }
        }
        return new ImportedRecipe(cleanName, minutes, cleanCategory, cleanIngredients, cleanSteps);
    }

    private static String canonical(String ingredient, List<String> knownIngredients) {
        return knownIngredients.stream().filter(k -> k.equalsIgnoreCase(ingredient)).findFirst().orElse(ingredient);
    }

    /** Splits an over-long step at the last sentence end, or failing that the last space, within the limit. */
    static List<String> splitStep(String step, int limit) {
        List<String> parts = new ArrayList<>();
        String rest = step;
        while (rest.length() > limit) {
            String window = rest.substring(0, limit + 1);
            // Prefer the end of a sentence; only split at a comma when there's none in reach.
            int cut = -1;
            for (String end : List.of(". ", "! ", "? ", "؟ ")) {
                cut = Math.max(cut, window.lastIndexOf(end));
            }
            if (cut <= 0) {
                for (String end : List.of("، ", ", ")) {
                    cut = Math.max(cut, window.lastIndexOf(end));
                }
            }
            if (cut > 0) {
                cut += 1; // keep the punctuation with the first part
            } else {
                cut = window.lastIndexOf(' ');
            }
            if (cut <= 0) {
                cut = limit;
            }
            parts.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }
}
