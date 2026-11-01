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
    /** {@link #SOURCE_BOT} or {@link #SOURCE_MANUAL}; null on foods saved before this field existed. */
    private final String source;
    /** Display name of whoever the recipe belongs to; null when unknown. */
    private final String authorName;
    /** Telegram {@code file_id} of the recipe's photo; null when it has none. */
    private final String photoFileId;

    public static final String SOURCE_BOT = "bot";
    public static final String SOURCE_MANUAL = "manual";

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language) {
        this(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe, language,
                Map.of());
    }

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language,
                Map<String, String> ingredientAmounts) {
        this(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe, language,
                ingredientAmounts, null, null);
    }

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language,
                Map<String, String> ingredientAmounts, String source, String authorName) {
        this(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe, language,
                ingredientAmounts, source, authorName, null);
    }

    public Food(String id, String name, int prepTimeMinutes, String category, List<String> ingredients,
                Long ownerChatId, Long createdByChatId, String recipe, Lang language,
                Map<String, String> ingredientAmounts, String source, String authorName, String photoFileId) {
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
        this.source = source;
        this.authorName = authorName;
        this.photoFileId = photoFileId;
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

    public String getSource() {
        return source;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    /**
     * Whether this food was added through the bot's /addfood flow. Foods saved before {@code source}
     * existed fall back on {@code createdByChatId}, which only the bot ever sets - foods typed
     * straight into foods.json have none.
     */
    public boolean isAddedViaBot() {
        return source != null ? SOURCE_BOT.equals(source) : createdByChatId != null;
    }

    public Food withProvenance(String source, String authorName) {
        return new Food(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe,
                language, ingredientAmounts, source, authorName, photoFileId);
    }

    public Food withPhoto(String photoFileId) {
        return new Food(id, name, prepTimeMinutes, category, ingredients, ownerChatId, createdByChatId, recipe,
                language, ingredientAmounts, source, authorName, photoFileId);
    }

    @Override
    public String toString() {
        return name + " [" + category + "] (" + prepTimeMinutes + " min) - " + String.join(", ", ingredients);
    }
}
