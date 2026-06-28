package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeStepsTest {

    @Test
    void moveUpSwapsWithPreviousStep() {
        List<String> steps = new ArrayList<>(List.of("a", "b", "c"));
        RecipeSteps.moveUp(steps, 1);
        assertEquals(List.of("b", "a", "c"), steps);
    }

    @Test
    void moveUpAtFirstIndexIsNoOp() {
        List<String> steps = new ArrayList<>(List.of("a", "b", "c"));
        RecipeSteps.moveUp(steps, 0);
        assertEquals(List.of("a", "b", "c"), steps);
    }

    @Test
    void moveDownSwapsWithNextStep() {
        List<String> steps = new ArrayList<>(List.of("a", "b", "c"));
        RecipeSteps.moveDown(steps, 1);
        assertEquals(List.of("a", "c", "b"), steps);
    }

    @Test
    void moveDownAtLastIndexIsNoOp() {
        List<String> steps = new ArrayList<>(List.of("a", "b", "c"));
        RecipeSteps.moveDown(steps, 2);
        assertEquals(List.of("a", "b", "c"), steps);
    }

    @Test
    void moveOutOfBoundsIndexIsNoOp() {
        List<String> steps = new ArrayList<>(List.of("a", "b"));
        RecipeSteps.moveUp(steps, 5);
        RecipeSteps.moveDown(steps, -1);
        assertEquals(List.of("a", "b"), steps);
    }

    @Test
    void joinNumbersEachStep() {
        assertEquals("1. Boil water\n2. Add pasta", RecipeSteps.join(List.of("Boil water", "Add pasta")));
    }

    @Test
    void joinEmptyListReturnsNull() {
        assertNull(RecipeSteps.join(List.of()));
    }

    @Test
    void parseStripsStepNumbers() {
        assertEquals(List.of("Boil water", "Add pasta"), RecipeSteps.parse("1. Boil water\n2. Add pasta"));
    }

    @Test
    void parseRoundTripsWithJoin() {
        List<String> original = List.of("Boil water", "Add pasta", "Drain and serve");
        assertEquals(original, RecipeSteps.parse(RecipeSteps.join(original)));
    }

    @Test
    void parseLegacyFreeTextWithoutNumbersKeepsItAsOneStep() {
        assertEquals(List.of("Boil water, add salt."), RecipeSteps.parse("Boil water, add salt."));
    }

    @Test
    void parseNullOrBlankReturnsEmptyList() {
        assertEquals(List.of(), RecipeSteps.parse(null));
        assertEquals(List.of(), RecipeSteps.parse("  "));
    }

    @Test
    void editingIndexAfterMoveUpFollowsTheMovedStep() {
        assertEquals(1, RecipeSteps.editingIndexAfterMoveUp(2, 2));
        assertEquals(2, RecipeSteps.editingIndexAfterMoveUp(2, 1));
        assertEquals(0, RecipeSteps.editingIndexAfterMoveUp(2, 0));
        assertNull(RecipeSteps.editingIndexAfterMoveUp(2, null));
    }

    @Test
    void editingIndexAfterMoveDownFollowsTheMovedStep() {
        assertEquals(2, RecipeSteps.editingIndexAfterMoveDown(1, 1));
        assertEquals(1, RecipeSteps.editingIndexAfterMoveDown(1, 2));
        assertEquals(0, RecipeSteps.editingIndexAfterMoveDown(1, 0));
        assertNull(RecipeSteps.editingIndexAfterMoveDown(1, null));
    }

    @Test
    void editingIndexAfterDeleteClearsWhenTheEditedStepIsDeleted() {
        assertNull(RecipeSteps.editingIndexAfterDelete(2, 2));
    }

    @Test
    void editingIndexAfterDeleteShiftsDownWhenAnEarlierStepIsDeleted() {
        assertEquals(1, RecipeSteps.editingIndexAfterDelete(0, 2));
    }

    @Test
    void editingIndexAfterDeleteUnaffectedWhenALaterStepIsDeleted() {
        assertEquals(0, RecipeSteps.editingIndexAfterDelete(2, 0));
    }

    @Test
    void editingIndexAfterDeleteWithNoActiveEditIsNull() {
        assertNull(RecipeSteps.editingIndexAfterDelete(0, null));
    }
}
