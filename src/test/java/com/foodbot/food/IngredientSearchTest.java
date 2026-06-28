package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientSearchTest {

    @Test
    void findExactMatchIsCaseInsensitive() {
        List<String> candidates = List.of("Egg", "Potato", "Rice");
        assertEquals("Egg", IngredientSearch.findExactMatch(candidates, "egg"));
        assertNull(IngredientSearch.findExactMatch(candidates, "eggplant"));
    }

    @Test
    void hasPartialMatchFindsSubstringCaseInsensitively() {
        List<String> candidates = List.of("Egg", "Eggplant", "Rice");
        assertTrue(IngredientSearch.hasPartialMatch(candidates, "egg"));
        assertFalse(IngredientSearch.hasPartialMatch(candidates, "banana"));
    }

    @Test
    void visibleCandidatesAlwaysIncludesSelectedEvenWhenFilteredOut() {
        List<String> candidates = List.of("Egg", "Potato", "Rice");
        Set<String> selected = new LinkedHashSet<>(List.of("Rice"));

        List<String> visible = IngredientSearch.visibleCandidates(candidates, selected, "egg", 10);

        assertTrue(visible.contains("Rice"));
        assertTrue(visible.contains("Egg"));
        assertFalse(visible.contains("Potato"));
    }

    @Test
    void visibleCandidatesRespectsCapAfterSelected() {
        List<String> candidates = List.of("A", "B", "C", "D", "E");
        Set<String> selected = new LinkedHashSet<>(List.of("A", "B"));

        List<String> visible = IngredientSearch.visibleCandidates(candidates, selected, "", 3);

        assertEquals(3, visible.size());
        assertTrue(visible.containsAll(List.of("A", "B")));
    }

    @Test
    void emptyFilterShowsUpToCapWithoutFiltering() {
        List<String> candidates = List.of("A", "B", "C");
        Set<String> selected = Set.of();

        List<String> visible = IngredientSearch.visibleCandidates(candidates, selected, "", 100);

        assertEquals(3, visible.size());
    }

    @Test
    void orderedCandidatesPutsSelectedFirstThenFilterMatches() {
        List<String> candidates = List.of("Egg", "Eggplant", "Rice", "Potato");
        Set<String> selected = new LinkedHashSet<>(List.of("Rice"));

        List<String> ordered = IngredientSearch.orderedCandidates(candidates, selected, "egg");

        assertEquals(List.of("Rice", "Egg", "Eggplant"), ordered);
    }

    @Test
    void orderedCandidatesIsUncappedSoCallersCanPaginate() {
        List<String> candidates = List.of("A", "B", "C", "D", "E", "F");
        Set<String> selected = Set.of();

        List<String> ordered = IngredientSearch.orderedCandidates(candidates, selected, "");

        assertEquals(6, ordered.size());
    }
}
