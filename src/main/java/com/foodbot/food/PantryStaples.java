package com.foodbot.food;

import java.util.Set;

/**
 * Ingredients common enough that almost everyone already has them, so /cook shouldn't
 * ask whether the user has them or list them as something to buy. Oil and spices are
 * included here too: they're assumed to be in almost every kitchen, and even on the rare
 * chance one's missing it's optional (doesn't stop a dish from being a good suggestion).
 */
public final class PantryStaples {
    private static final Set<String> STAPLES = Set.of(
            "water", "آب",
            "salt", "نمک",
            "oil", "روغن",
            "spice", "spices", "ادویه", "ادویه جات");

    public static boolean isStaple(String ingredient) {
        return STAPLES.stream().anyMatch(staple -> staple.equalsIgnoreCase(ingredient.trim()));
    }

    private PantryStaples() {
    }
}
