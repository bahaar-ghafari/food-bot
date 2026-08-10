package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodTest {

    @Test
    void fourArgConstructorDefaultsToEmptyIngredientAmounts() {
        Food food = new Food("id-1", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN);
        assertTrue(food.getIngredientAmounts().isEmpty());
    }

    @Test
    void explicitNullIngredientAmountsIsTreatedAsEmpty() {
        Food food = new Food("id-2", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN, null);
        assertTrue(food.getIngredientAmounts().isEmpty());
    }

    @Test
    void ingredientAmountsAreReturnedAsGiven() {
        Food food = new Food("id-3", "Omelette", 10, "Breakfast", List.of("egg"), null, 111L, null, Lang.EN,
                Map.of("egg", "2"));
        assertEquals("2", food.getIngredientAmounts().get("egg"));
    }

    @Test
    void toStringIncludesNameCategoryTimeAndIngredients() {
        Food food = new Food("id-4", "Pasta", 15, "MainCourse", List.of("pasta", "beef"), null, 111L, null, Lang.EN);
        String text = food.toString();
        assertTrue(text.contains("Pasta"));
        assertTrue(text.contains("MainCourse"));
        assertTrue(text.contains("15"));
        assertTrue(text.contains("pasta, beef"));
    }
}
