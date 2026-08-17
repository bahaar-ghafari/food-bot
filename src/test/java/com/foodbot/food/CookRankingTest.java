package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookRankingTest {

    private Food food(String name, List<String> ingredients) {
        return new Food(name, name, 10, "MainCourse", ingredients, null, 1L, null, Lang.EN);
    }

    @Test
    void ordersByFewestMissingIngredientsFirst() {
        Food needsTwo = food("Needs Two", List.of("chicken", "rice", "cumin", "paprika"));
        Food needsOne = food("Needs One", List.of("chicken", "rice", "onion"));
        List<Food> foods = new ArrayList<>(List.of(needsTwo, needsOne));

        CookRanking.sortByBestMatch(foods, Set.of("chicken", "rice"));

        assertEquals(List.of(needsOne, needsTwo), foods);
    }

    @Test
    void whenMissingCountTiesPrefersTheDishReusingMoreOfWhatUserHas() {
        // Regression test for: with lots of chicken on hand, a chicken-based dish that's
        // missing one ingredient should outrank an unrelated dish (e.g. Ghormeh Sabzi) that
        // also only needs one more ingredient but doesn't use the chicken (or anything else)
        // the user already has.
        Food chickenDish = food("Chicken Rice", List.of("chicken", "rice", "onion", "garlic"));
        Food ghormehSabzi = food("Ghormeh Sabzi", List.of("herbs", "dried lime"));
        List<Food> foods = new ArrayList<>(List.of(ghormehSabzi, chickenDish));

        Set<String> have = Set.of("chicken", "rice", "onion", "herbs");
        // Both dishes are missing exactly one ingredient (garlic / dried lime)...
        assertEquals(1, CookRanking.missingCount(chickenDish, have));
        assertEquals(1, CookRanking.missingCount(ghormehSabzi, have));
        // ...but the chicken dish reuses more of what's on hand (3 matches vs. 1), so it
        // should rank first even though both need exactly one more ingredient bought.
        CookRanking.sortByBestMatch(foods, have);

        assertEquals(List.of(chickenDish, ghormehSabzi), foods);
    }

    @Test
    void readyToCookDishesAreRankedByHowMuchOfThePantryTheyUse() {
        // Both are fully cookable right now (missingCount == 0 for each), but the chicken
        // dish makes use of more of the user's selected ingredients.
        Food chickenDish = food("Chicken Stir Fry", List.of("chicken", "rice", "soy sauce"));
        Food simpleSalad = food("Simple Salad", List.of("lettuce", "tomato"));
        List<Food> foods = new ArrayList<>(List.of(simpleSalad, chickenDish));

        Set<String> have = Set.of("chicken", "rice", "soy sauce", "lettuce", "tomato");
        CookRanking.sortByBestMatch(foods, have);

        assertEquals(List.of(chickenDish, simpleSalad), foods);
    }

    @Test
    void staplesDoNotCountTowardMatchedIngredients() {
        Food dish = food("Boiled Rice", List.of("rice", "water", "salt"));
        assertEquals(1, CookRanking.matchedCount(dish, Set.of("rice")));
    }

    @Test
    void staplesAreNeverCountedAsMissing() {
        Food dish = food("Boiled Rice", List.of("rice", "water", "salt"));
        assertEquals(0, CookRanking.missingCount(dish, Set.of("rice")));
    }

    @Test
    void matchIsCaseInsensitive() {
        Food dish = food("Omelette", List.of("Egg", "Cheese"));
        assertEquals(1, CookRanking.matchedCount(dish, Set.of("egg")));
        assertEquals(1, CookRanking.missingCount(dish, Set.of("egg")));
    }

    @Test
    void doesNotBuildOnWhatUserHasWhenThereIsNoOverlapAtAll() {
        // Regression test for: "I have chicken" shouldn't keep surfacing an unrelated fish
        // or beef dish that would need every ingredient bought from scratch.
        Food fishDish = food("Grilled Salmon", List.of("salmon", "lemon", "dill"));
        assertFalse(CookRanking.buildsOnWhatUserHas(fishDish, Set.of("chicken")));
    }

    @Test
    void buildsOnWhatUserHasWhenAtLeastOneIngredientOverlaps() {
        Food chickenDish = food("Chicken Curry", List.of("chicken", "curry powder", "onion"));
        assertTrue(CookRanking.buildsOnWhatUserHas(chickenDish, Set.of("chicken")));
    }

    @Test
    void staplesDoNotCountAsBuildingOnWhatUserHas() {
        // Sharing only a staple (assumed available to everyone) isn't a real connection to
        // the user's actual selected ingredients.
        Food dish = food("Plain Rice", List.of("rice", "water", "salt"));
        assertFalse(CookRanking.buildsOnWhatUserHas(dish, Set.of("chicken")));
    }
}
