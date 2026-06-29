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
    void cocktailIngredientsHaveIcons() {
        assertEquals("🥃", IngredientIcons.iconFor("rum"));
        assertEquals("🌵", IngredientIcons.iconFor("tequila"));
        assertEquals("🍍", IngredientIcons.iconFor("pineapple"));
        assertEquals("🥥", IngredientIcons.iconFor("coconut milk"));
    }

    @Test
    void persianStapleIngredientsAddedForExpandedMenuHaveIcons() {
        assertEquals("🍆", IngredientIcons.iconFor("eggplant"));
        assertEquals("🍆", IngredientIcons.iconFor("بادمجان"));
        assertEquals("🟡", IngredientIcons.iconFor("turmeric"));
        assertEquals("🌼", IngredientIcons.iconFor("saffron"));
        assertEquals("🫛", IngredientIcons.iconFor("chickpeas"));
        assertEquals("🍫", IngredientIcons.iconFor("chocolate"));
        assertEquals("🌻", IngredientIcons.iconFor("تخمه"));
        assertEquals("🌻", IngredientIcons.iconFor("sunflower seeds"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertEquals(IngredientIcons.iconFor("egg"), IngredientIcons.iconFor("EGG"));
    }

    @Test
    void unknownIngredientHasNoIcon() {
        String icon = IngredientIcons.iconFor("some-made-up-ingredient-xyz");
        assertNotNull(icon);
        assertEquals("", icon);
    }
}
