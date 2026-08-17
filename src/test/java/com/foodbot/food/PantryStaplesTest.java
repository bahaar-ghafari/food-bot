package com.foodbot.food;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PantryStaplesTest {

    @Test
    void waterAndSaltAreStaplesInBothLanguages() {
        assertTrue(PantryStaples.isStaple("water"));
        assertTrue(PantryStaples.isStaple("آب"));
        assertTrue(PantryStaples.isStaple("salt"));
        assertTrue(PantryStaples.isStaple("نمک"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertTrue(PantryStaples.isStaple("Water"));
        assertTrue(PantryStaples.isStaple("SALT"));
    }

    @Test
    void oilAndSpicesAreStaplesInBothLanguages() {
        assertTrue(PantryStaples.isStaple("oil"));
        assertTrue(PantryStaples.isStaple("روغن"));
        assertTrue(PantryStaples.isStaple("spice"));
        assertTrue(PantryStaples.isStaple("spices"));
        assertTrue(PantryStaples.isStaple("ادویه"));
    }

    @Test
    void unrelatedIngredientsAreNotStaples() {
        assertFalse(PantryStaples.isStaple("chicken"));
        assertFalse(PantryStaples.isStaple("rice"));
    }
}
