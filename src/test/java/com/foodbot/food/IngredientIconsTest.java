package com.foodbot.food;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IngredientIconsTest {

    @Test
    void knownIngredientsHaveExpectedIcons() {
        assertEquals("🥚", IngredientIcons.iconFor("egg"));
        assertEquals("🥔", IngredientIcons.iconFor("potato"));
        assertEquals("🍚", IngredientIcons.iconFor("Rice"));
        assertEquals("🐟", IngredientIcons.iconFor("Tuna"));
    }

    @Test
    void persianCuisineIngredientsHaveIcons() {
        assertEquals("🌿", IngredientIcons.iconFor("parsley"));
        assertEquals("🌰", IngredientIcons.iconFor("walnut"));
        assertEquals("🔴", IngredientIcons.iconFor("barberries"));
        assertEquals("🫘", IngredientIcons.iconFor("kidney beans"));
    }

    @Test
    void persianNamedIngredientsAddedForSeedDishesHaveIcons() {
        assertEquals("🍜", IngredientIcons.iconFor("رشته"));
        assertEquals("🌾", IngredientIcons.iconFor("آرد"));
        assertEquals("🧈", IngredientIcons.iconFor("کره"));
        assertEquals("🧂", IngredientIcons.iconFor("نمک"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertEquals(IngredientIcons.iconFor("egg"), IngredientIcons.iconFor("EGG"));
    }

    @Test
    void unknownIngredientFallsBackToDefaultIcon() {
        String icon = IngredientIcons.iconFor("some-made-up-ingredient-xyz");
        assertNotNull(icon);
        assertEquals("🧂", icon);
    }
}
