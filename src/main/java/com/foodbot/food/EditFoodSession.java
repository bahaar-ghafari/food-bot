package com.foodbot.food;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditFoodSession implements IngredientPickerState, RecipeStepEditorState, AmountEditorState {
    public enum Step {
        REVIEW, EDITING_NAME, EDITING_TIME, EDITING_CATEGORY, EDITING_INGREDIENTS, EDITING_AMOUNTS,
        EDITING_RECIPE, EDITING_PHOTO
    }

    private final String foodId;
    private Step step = Step.REVIEW;
    private String name;
    private int prepTimeMinutes;
    private String category;
    private String photoFileId;
    /** Callback that leaves the editor without saving (the Settings screen it was opened from). */
    private String exitCallback;
    private final List<String> recipeSteps = new ArrayList<>();
    private Integer editingRecipeStepIndex;
    private final List<String> candidateIngredients = new ArrayList<>();
    private final Set<String> selectedIngredients = new LinkedHashSet<>();
    private final Map<String, String> ingredientAmounts = new LinkedHashMap<>();
    private String amountEditingIngredient;
    private String ingredientFilter = "";
    private int ingredientPage;
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

    public String getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
        this.photoFileId = photoFileId;
    }

    public String getExitCallback() {
        return exitCallback;
    }

    public void setExitCallback(String exitCallback) {
        this.exitCallback = exitCallback;
    }

    public List<String> getRecipeSteps() {
        return recipeSteps;
    }

    public Integer getEditingRecipeStepIndex() {
        return editingRecipeStepIndex;
    }

    public void setEditingRecipeStepIndex(Integer editingRecipeStepIndex) {
        this.editingRecipeStepIndex = editingRecipeStepIndex;
    }

    public List<String> getCandidateIngredients() {
        return candidateIngredients;
    }

    public Set<String> getSelectedIngredients() {
        return selectedIngredients;
    }

    public Map<String, String> getIngredientAmounts() {
        return ingredientAmounts;
    }

    public String getAmountEditingIngredient() {
        return amountEditingIngredient;
    }

    public void setAmountEditingIngredient(String amountEditingIngredient) {
        this.amountEditingIngredient = amountEditingIngredient;
    }

    public String getIngredientFilter() {
        return ingredientFilter;
    }

    public void setIngredientFilter(String ingredientFilter) {
        this.ingredientFilter = ingredientFilter;
    }

    public int getIngredientPage() {
        return ingredientPage;
    }

    public void setIngredientPage(int ingredientPage) {
        this.ingredientPage = ingredientPage;
    }

    public Integer getKeyboardMessageId() {
        return keyboardMessageId;
    }

    public void setKeyboardMessageId(Integer keyboardMessageId) {
        this.keyboardMessageId = keyboardMessageId;
    }
}
