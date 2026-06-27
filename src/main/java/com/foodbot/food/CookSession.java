package com.foodbot.food;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CookSession implements IngredientPickerState {
    public enum Step { AWAITING_TIME, SELECTING_INGREDIENTS, ASK_SHOPPING, SELECTING_CATEGORY }

    private Step step = Step.AWAITING_TIME;
    private int timeMinutes;
    private final List<String> candidateIngredients = new ArrayList<>();
    private final Set<String> haveIngredients = new LinkedHashSet<>();
    private String ingredientFilter = "";
    private boolean canShop;
    private String category;
    private Integer keyboardMessageId;

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(int timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    public List<String> getCandidateIngredients() {
        return candidateIngredients;
    }

    public Set<String> getHaveIngredients() {
        return haveIngredients;
    }

    @Override
    public Set<String> getSelectedIngredients() {
        return haveIngredients;
    }

    @Override
    public String getIngredientFilter() {
        return ingredientFilter;
    }

    @Override
    public void setIngredientFilter(String ingredientFilter) {
        this.ingredientFilter = ingredientFilter;
    }

    public boolean isCanShop() {
        return canShop;
    }

    public void setCanShop(boolean canShop) {
        this.canShop = canShop;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getKeyboardMessageId() {
        return keyboardMessageId;
    }

    public void setKeyboardMessageId(Integer keyboardMessageId) {
        this.keyboardMessageId = keyboardMessageId;
    }
}
