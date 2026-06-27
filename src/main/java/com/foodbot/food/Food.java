package com.foodbot.food;

import java.util.List;

public class Food {
    private final String id;
    private final String name;
    private final int prepTimeMinutes;
    private final String category;
    private final List<String> ingredients;
    private final Long ownerChatId;
    private final Long createdByChatId;

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId) {
        this.id = id;
        this.name = name;
        this.prepTimeMinutes = prepTimeMinutes;
        this.category = category;
        this.ingredients = ingredients;
        this.ownerChatId = ownerChatId;
        this.createdByChatId = createdByChatId;
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

    @Override
    public String toString() {
        return name + " [" + category + "] (" + prepTimeMinutes + " min) - " + String.join(", ", ingredients);
    }
}
