package com.foodbot.food;

import com.foodbot.lang.Lang;

import java.util.List;
import java.util.Map;

public class Food {
    private final String id;
    private final String name;
    private final int prepTimeMinutes;
    private final String category;
    private final List<String> ingredients;
    private final Long ownerChatId;
    private final Long createdByChatId;
    private final String recipe;
    private final Lang language;
    private final Map<String, String> ingredientAmounts;

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language) {
        this(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe, language,
                Map.of());
    }

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language,
                Map<String, String> ingredientAmounts) {
        this.id = id;
        this.name = name;
        this.prepTimeMinutes = prepTimeMinutes;
        this.category = category;
        this.ingredients = ingredients;
        this.ownerChatId = ownerChatId;
        this.createdByChatId = createdByChatId;
        this.recipe = recipe;
        this.language = language;
        this.ingredientAmounts = ingredientAmounts != null ? ingredientAmounts : Map.of();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public Long getOwnerChatId() {
        return ownerChatId;
    }

    public Long getCreatedByChatId() {
        return createdByChatId;
    }

    public String getRecipe() {
        return recipe;
    }

    public Lang getLanguage() {
        return language;
    }

    public Map<String, String> getIngredientAmounts() {
        return ingredientAmounts != null ? ingredientAmounts : Map.of();
    }

    @Override
    public String toString() {
        return name + " [" + category + "] (" + prepTimeMinutes + " min) - " + String.join(", ", ingredients);
    }
}
