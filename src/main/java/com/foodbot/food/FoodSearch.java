package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class FoodSearch {

    public static List<Food> search(List<Food> foods, String query) {
        List<Food> byName = searchByName(foods, query);
        if (!byName.isEmpty()) {
            return byName;
        }
        return searchByIngredient(foods, query);
    }

    public static List<Food> searchByName(List<Food> foods, String query) {
        String needle = query.trim().toLowerCase();
        List<Food> result = new ArrayList<>();
        for (Food food : foods) {
            if (food.getName().toLowerCase().contains(needle)) {
                result.add(food);
            }
        }
        return result;
    }

    public static List<Food> searchByIngredient(List<Food> foods, String query) {
        String needle = query.trim().toLowerCase();
        List<Food> result = new ArrayList<>();
        for (Food food : foods) {
            boolean matches = food.getIngredients().stream()
                    .anyMatch(ingredient -> ingredient.toLowerCase().contains(needle));
            if (matches) {
                result.add(food);
            }
        }
        return result;
    }

    public static Optional<Food> fuzzyNameMatch(List<Food> foods, String query) {
        String needle = query.trim().toLowerCase();
        if (needle.isEmpty()) {
            return Optional.empty();
        }
        Food best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Food food : foods) {
            int distance = bestWordDistance(food.getName().toLowerCase(), needle);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = food;
            }
        }
        int threshold = needle.length() <= 5 ? 1 : 2;
        if (best != null && bestDistance > 0 && bestDistance <= threshold) {
            return Optional.of(best);
        }
        return Optional.empty();
    }

    private static int bestWordDistance(String name, String needle) {
        int best = Levenshtein.distance(name, needle);
        for (String word : name.split("\\s+")) {
            best = Math.min(best, Levenshtein.distance(word, needle));
        }
        return best;
    }

    private FoodSearch() {
    }
}
