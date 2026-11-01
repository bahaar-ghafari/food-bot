package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void legacyFoodWithCreatorCountsAsBotAndWithoutCreatorAsManual() {
        Food viaBot = new Food("id-5", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN);
        Food manual = new Food("id-6", "Soup", 20, "MainCourse", List.of("water"), null, null, null, Lang.EN);
        assertTrue(viaBot.isAddedViaBot());
        assertFalse(manual.isAddedViaBot());
    }

    @Test
    void explicitSourceWinsOverCreator() {
        Food manual = new Food("id-7", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN)
                .withProvenance(Food.SOURCE_MANUAL, "Bahar");
        assertFalse(manual.isAddedViaBot());
        assertEquals("Bahar", manual.getAuthorName());
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
