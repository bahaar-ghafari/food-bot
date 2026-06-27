package com.foodbot.food;

import java.util.List;

public class Food {
    private final String name;
    private final int prepTimeMinutes;
    private final List<String> ingredients;

    public Food(String name, int prepTimeMinutes, List<String> ingredients) {
        this.name = name;
        this.prepTimeMinutes = prepTimeMinutes;
        this.ingredients = ingredients;
    }

    public String getName() {
        return name;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    @Override
    public String toString() {
        return name + " (" + prepTimeMinutes + " min) - " + String.join(", ", ingredients);
    }
}
