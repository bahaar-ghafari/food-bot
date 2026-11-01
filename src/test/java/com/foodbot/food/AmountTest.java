package com.foodbot.food;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmountTest {

    @Test
    void scalesWholeNumbersWithUnit() {
        assertEquals("4 cups", Amount.scale("2 cups", 2));
        assertEquals("1 cups", Amount.scale("2 cups", 0.5));
    }

    @Test
    void scalesDecimalsFractionsAndMixedNumbers() {
        assertEquals("3", Amount.scale("1.5", 2));
        assertEquals("1 tbsp", Amount.scale("1/2 tbsp", 2));
        assertEquals("3 tbsp", Amount.scale("1 1/2 tbsp", 2));
        assertEquals("1 kg", Amount.scale("½ kg", 2));
        assertEquals("3 cups", Amount.scale("1½ cups", 2));
    }

    @Test
    void scalesRanges() {
        assertEquals("4-6 cloves", Amount.scale("2-3 cloves", 2));
        assertEquals("4–6 cloves", Amount.scale("2 – 3 cloves", 2));
    }

    @Test
    void leavesNonNumericAmountsAlone() {
        assertEquals("a pinch", Amount.scale("a pinch", 2));
        assertEquals("to taste", Amount.scale("to taste", 3));
        assertNull(Amount.scale(null, 2));
    }

    @Test
    void factorOfOneReturnsInputUnchanged() {
        assertEquals("2 cups", Amount.scale("2 cups", 1));
    }

    @Test
    void roundsToTwoDecimals() {
        assertEquals("0.67 cups", Amount.scale("1/3 cups", 2));
    }

    @Test
    void keepsPersianDigits() {
        assertEquals("۴ عدد", Amount.scale("۲ عدد", 2));
    }
}
