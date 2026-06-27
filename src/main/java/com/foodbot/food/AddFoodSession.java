package com.foodbot.food;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddFoodSession implements IngredientPickerState {
    public enum Step { ASK_SCOPE, AWAITING_NAME, AWAITING_PREP_TIME, SELECTING_CATEGORY, SELECTING_INGREDIENTS }

    private Step step = Step.ASK_SCOPE;
    private String name;
    private int prepTimeMinutes;
    private String category;
    private Long ownerChatId;
    private final List<String> candidateIngredients = new ArrayList<>();
    private final Set<String> selectedIngredients = new LinkedHashSet<>();
    private String ingredientFilter = "";
    private Integer keyboardMessageId;

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getOwnerChatId() {
        return ownerChatId;
    }

    public void setOwnerChatId(Long ownerChatId) {
        this.ownerChatId = ownerChatId;
    }

    public List<String> getCandidateIngredients() {
        return candidateIngredients;
    }

    public Set<String> getSelectedIngredients() {
        return selectedIngredients;
    }

    public String getIngredientFilter() {
        return ingredientFilter;
    }

    public void setIngredientFilter(String ingredientFilter) {
        this.ingredientFilter = ingredientFilter;
    }

    public Integer getKeyboardMessageId() {
        return keyboardMessageId;
    }

    public void setKeyboardMessageId(Integer keyboardMessageId) {
        this.keyboardMessageId = keyboardMessageId;
    }
}
