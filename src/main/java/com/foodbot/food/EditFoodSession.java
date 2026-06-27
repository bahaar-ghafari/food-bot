package com.foodbot.food;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EditFoodSession implements IngredientPickerState {
    public enum Step { CHOOSING_FIELD, EDITING_NAME, EDITING_TIME, EDITING_CATEGORY, EDITING_INGREDIENTS }

    private final String foodId;
    private Step step = Step.CHOOSING_FIELD;
    private final List<String> candidateIngredients = new ArrayList<>();
    private final Set<String> selectedIngredients = new LinkedHashSet<>();
    private String ingredientFilter = "";
    private Integer keyboardMessageId;

    public EditFoodSession(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodId() {
        return foodId;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
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
