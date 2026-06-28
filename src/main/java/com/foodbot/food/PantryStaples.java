package com.foodbot.food;

import java.util.Set;

/**
 * Ingredients common enough that almost everyone already has them, so /cook shouldn't
 * ask whether the user has them or list them as something to buy.
 */
public final class PantryStaples {
    private static final Set<String> STAPLES = Set.of("water", "آب", "salt", "نمک");

    public static boolean isStaple(String ingredient) {
        return STAPLES.stream().anyMatch(staple -> staple.equalsIgnoreCase(ingredient.trim()));
    }

    private PantryStaples() {
    }
}
