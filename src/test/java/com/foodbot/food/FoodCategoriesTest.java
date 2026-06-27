package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodCategoriesTest {

    @Test
    void categoriesHaveNoDuplicates() {
        Set<String> unique = new HashSet<>(FoodCategories.ALL);
        assertEquals(unique.size(), FoodCategories.ALL.size());
    }

    @Test
    void lunchAndDinnerWereMergedIntoMainCourse() {
        assertFalse(FoodCategories.ALL.contains("Lunch"));
        assertFalse(FoodCategories.ALL.contains("Dinner"));
        assertTrue(FoodCategories.ALL.contains("MainCourse"));
    }
}
