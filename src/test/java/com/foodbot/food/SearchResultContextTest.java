package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultContextTest {

    @Test
    void exposesFoodsAndQuery() {
        Food food = new Food("id-1", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN);
        SearchResultContext ctx = new SearchResultContext(List.of(food), "soup");

        assertEquals(List.of(food), ctx.getFoods());
        assertEquals("soup", ctx.getQuery());
    }

    @Test
    void copiesFoodsSoLaterMutationOfTheOriginalIsNotVisible() {
        Food food = new Food("id-1", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN);
        List<Food> foods = new ArrayList<>(List.of(food));
        SearchResultContext ctx = new SearchResultContext(foods, "soup");

        foods.clear();

        assertTrue(ctx.getFoods().contains(food));
    }
}
