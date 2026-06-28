package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodSearchTest {

    private Food food(String name, List<String> ingredients) {
        return new Food(name, name, 10, "MainCourse", ingredients, null, 1L, null, Lang.EN);
    }

    @Test
    void searchByNameMatchesPartialAndIsCaseInsensitive() {
        Food ghormeh = food("Ghormeh Sabzi", List.of("lamb", "herbs"));
        Food other = food("Pasta", List.of("pasta", "tomato"));
        List<Food> results = FoodSearch.search(List.of(ghormeh, other), "GHOR");
        assertEquals(List.of(ghormeh), results);
    }

    @Test
    void searchFallsBackToIngredientsWhenNoNameMatches() {
        Food omelette = food("Omelette", List.of("egg", "cheese"));
        Food salad = food("Salad", List.of("lettuce", "egg"));
        Food soup = food("Soup", List.of("carrot", "onion"));
        List<Food> results = FoodSearch.search(List.of(omelette, salad, soup), "egg");
        assertEquals(2, results.size());
        assertTrue(results.contains(omelette));
        assertTrue(results.contains(salad));
    }

    @Test
    void searchPrefersNameMatchesOverIngredientMatches() {
        Food eggDish = food("Egg Curry", List.of("egg", "curry powder"));
        Food otherWithEgg = food("Fried Rice", List.of("rice", "egg"));
        List<Food> results = FoodSearch.search(List.of(eggDish, otherWithEgg), "egg");
        assertEquals(List.of(eggDish), results);
    }

    @Test
    void searchReturnsEmptyWhenNothingMatchesEither() {
        Food soup = food("Soup", List.of("carrot", "onion"));
        assertTrue(FoodSearch.search(List.of(soup), "pineapple").isEmpty());
    }

    @Test
    void searchByIngredientIsCaseInsensitive() {
        Food dish = food("Stew", List.of("Beef", "Potato"));
        assertEquals(List.of(dish), FoodSearch.searchByIngredient(List.of(dish), "beef"));
    }

    @Test
    void fuzzyNameMatchSuggestsClosestNameWithMissingTrailingLetter() {
        Food dish = food("sibtokhm", List.of("apple", "egg"));
        assertEquals(Optional.of(dish), FoodSearch.fuzzyNameMatch(List.of(dish), "sibtokh"));
    }

    @Test
    void fuzzyNameMatchSuggestsClosestNameWithMissingMiddleLetter() {
        Food dish = food("sibtokhm", List.of("apple", "egg"));
        assertEquals(Optional.of(dish), FoodSearch.fuzzyNameMatch(List.of(dish), "sibtokm"));
    }

    @Test
    void fuzzyNameMatchIgnoresExactMatches() {
        Food dish = food("Soup", List.of("carrot"));
        assertEquals(Optional.empty(), FoodSearch.fuzzyNameMatch(List.of(dish), "soup"));
    }

    @Test
    void fuzzyNameMatchRejectsTooDifferentNames() {
        Food dish = food("Pasta", List.of("pasta"));
        assertEquals(Optional.empty(), FoodSearch.fuzzyNameMatch(List.of(dish), "pineapple"));
    }
}
