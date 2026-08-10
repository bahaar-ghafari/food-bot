package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class CookResultContextTest {

    @Test
    void exposesTheConstructorArguments() {
        Set<String> have = new LinkedHashSet<>(Set.of("rice", "chicken"));
        CookResultContext ctx = new CookResultContext(30, have, true, "MainCourse");

        assertEquals(30, ctx.getTimeMinutes());
        assertEquals(have, ctx.getHaveIngredients());
        assertEquals(true, ctx.isCanShop());
        assertEquals("MainCourse", ctx.getCategory());
    }

    @Test
    void copiesHaveIngredientsSoLaterMutationOfTheOriginalIsNotVisible() {
        Set<String> have = new LinkedHashSet<>(Set.of("rice"));
        CookResultContext ctx = new CookResultContext(30, have, false, null);

        have.add("chicken");

        assertFalse(ctx.getHaveIngredients().contains("chicken"));
    }

    @Test
    void preservesInsertionOrder() {
        Set<String> have = new LinkedHashSet<>();
        have.add("rice");
        have.add("chicken");
        have.add("onion");

        CookResultContext ctx = new CookResultContext(30, have, false, null);

        assertIterableEquals(have, ctx.getHaveIngredients());
    }
}
