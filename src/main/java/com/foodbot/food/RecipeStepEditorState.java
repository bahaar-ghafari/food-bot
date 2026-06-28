package com.foodbot.food;

import java.util.List;

public interface RecipeStepEditorState {
    List<String> getRecipeSteps();

    Integer getEditingRecipeStepIndex();

    void setEditingRecipeStepIndex(Integer index);
}
