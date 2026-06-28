package com.foodbot.food;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevenshteinTest {

    @Test
    void identicalStringsHaveZeroDistance() {
        assertEquals(0, Levenshtein.distance("sibtokhm", "sibtokhm"));
    }

    @Test
    void missingTrailingCharacterIsOneEdit() {
        assertEquals(1, Levenshtein.distance("sibtokh", "sibtokhm"));
    }

    @Test
    void missingMiddleCharacterIsOneEdit() {
        assertEquals(1, Levenshtein.distance("sibtokm", "sibtokhm"));
    }

    @Test
    void substitutionIsOneEdit() {
        assertEquals(1, Levenshtein.distance("kitten", "kitten".replace('k', 'm')));
    }

    @Test
    void completelyDifferentStringsHaveHighDistance() {
        assertEquals(Math.max("kitten".length(), "soup".length()), Levenshtein.distance("kitten", "soup"));
    }

    @Test
    void emptyStringDistanceEqualsOtherLength() {
        assertEquals(5, Levenshtein.distance("", "apple"));
    }
}
