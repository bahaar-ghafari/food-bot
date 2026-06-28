package com.foodbot.food;

import java.util.List;
import java.util.Set;

public interface IngredientPickerState {
    List<String> getCandidateIngredients();

    Set<String> getSelectedIngredients();

    String getIngredientFilter();

    void setIngredientFilter(String filter);

    int getIngredientPage();

    void setIngredientPage(int page);

    Integer getKeyboardMessageId();

    void setKeyboardMessageId(Integer keyboardMessageId);
}
