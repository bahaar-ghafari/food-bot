package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;

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

    private FoodSearch() {
    }
}
