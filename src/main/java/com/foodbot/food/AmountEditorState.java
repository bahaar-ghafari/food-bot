package com.foodbot.food;

import java.util.Map;

public interface AmountEditorState {
    Map<String, String> getIngredientAmounts();

    String getAmountEditingIngredient();

    void setAmountEditingIngredient(String ingredient);
}
