package com.foodbot.food;

import java.util.List;

public final class FoodCategories {
    public static final List<String> ALL = List.of(
            "Breakfast", "MainCourse", "Snack", "Dessert", "Drink", "Other"
    );

    private FoodCategories() {
    }
}
