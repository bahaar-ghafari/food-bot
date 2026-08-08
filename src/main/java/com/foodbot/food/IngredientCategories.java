package com.foodbot.food;

import java.util.Optional;
import java.util.Set;

/**
 * Classifies ingredients as a protein or a carbohydrate/starch base, so /cook can suggest
 * "just cook the chicken you have" instead of a full recipe when nothing else matches.
 */
public final class IngredientCategories {
    private static final Set<String> PROTEINS = Set.of(
            "beef", "meat", "گوشت",
            "chicken", "مرغ",
            "fish", "ماهی",
            "tuna", "ماهی تن", "تن ماهی",
            "salmon", "ماهی سالمون",
            "shrimp",
            "egg", "eggs", "تخم مرغ",
            "beans", "kidney beans", "لوبیا قرمز",
            "lentil", "عدس",
            "chickpeas", "نخود",
            "لپه");

    private static final Set<String> CARBS = Set.of(
            "rice", "برنج",
            "bread", "نان",
            "pasta",
            "noodle", "noodles", "رشته",
            "potato", "potatoes", "سیب زمینی",
            "flour", "آرد",
            "oats",
            "tortilla",
            "corn",
            "جو");

    public static boolean isProtein(String ingredient) {
        return matches(PROTEINS, ingredient);
    }

    public static boolean isCarb(String ingredient) {
        return matches(CARBS, ingredient);
    }

    /**
     * Picks one protein first, falling back to a carb, from the given ingredients -
     * preserving their iteration order so the result is deterministic for a given input.
     */
    public static Optional<String> pickProteinOrCarb(Iterable<String> ingredients) {
        for (String ingredient : ingredients) {
            if (isProtein(ingredient)) {
                return Optional.of(ingredient);
            }
        }
        for (String ingredient : ingredients) {
            if (isCarb(ingredient)) {
                return Optional.of(ingredient);
            }
        }
        return Optional.empty();
    }

    private static boolean matches(Set<String> set, String ingredient) {
        String trimmed = ingredient.trim();
        return set.stream().anyMatch(s -> s.equalsIgnoreCase(trimmed));
    }

    private IngredientCategories() {
    }
}
