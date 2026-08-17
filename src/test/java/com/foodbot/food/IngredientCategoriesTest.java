package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientCategoriesTest {

    @Test
    void recognizesProteinsInBothLanguages() {
        assertTrue(IngredientCategories.isProtein("chicken"));
        assertTrue(IngredientCategories.isProtein("مرغ"));
        assertTrue(IngredientCategories.isProtein("Beef"));
    }

    @Test
    void recognizesCarbsInBothLanguages() {
        assertTrue(IngredientCategories.isCarb("rice"));
        assertTrue(IngredientCategories.isCarb("برنج"));
        assertTrue(IngredientCategories.isCarb("Bread"));
    }

    @Test
    void vegetablesAndSeasoningsAreNeither() {
        assertFalse(IngredientCategories.isProtein("onion"));
        assertFalse(IngredientCategories.isCarb("onion"));
        assertFalse(IngredientCategories.isProtein("turmeric"));
        assertFalse(IngredientCategories.isCarb("turmeric"));
    }

    @Test
    void pickPrefersProteinOverCarb() {
        Set<String> have = new LinkedHashSet<>(Set.of("rice", "onion", "chicken"));
        assertEquals(Optional.of("chicken"), IngredientCategories.pickProteinOrCarb(have));
    }

    @Test
    void pickFallsBackToCarbWhenNoProtein() {
        Set<String> have = new LinkedHashSet<>(Set.of("onion", "rice"));
        assertEquals(Optional.of("rice"), IngredientCategories.pickProteinOrCarb(have));
    }

    @Test
    void pickIsEmptyWhenNeitherPresent() {
        Set<String> have = new LinkedHashSet<>(Set.of("onion", "turmeric"));
        assertEquals(Optional.empty(), IngredientCategories.pickProteinOrCarb(have));
    }

    @Test
    void pickProteinAndCarbFindsBothWhenPresent() {
        // e.g. "I have tuna and rice" - should surface both, not just the tuna.
        Set<String> have = new LinkedHashSet<>(Set.of("tuna", "rice"));
        assertEquals(Optional.of(new IngredientCategories.Combo("tuna", "rice")),
                IngredientCategories.pickProteinAndCarb(have));
    }

    @Test
    void pickProteinAndCarbIsEmptyWhenOnlyOneSidePresent() {
        Set<String> proteinOnly = new LinkedHashSet<>(Set.of("tuna", "onion"));
        assertEquals(Optional.empty(), IngredientCategories.pickProteinAndCarb(proteinOnly));

        Set<String> carbOnly = new LinkedHashSet<>(Set.of("rice", "onion"));
        assertEquals(Optional.empty(), IngredientCategories.pickProteinAndCarb(carbOnly));
    }
}
