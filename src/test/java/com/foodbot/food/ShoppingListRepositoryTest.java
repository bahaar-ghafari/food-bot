package com.foodbot.food;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShoppingListRepositoryTest {

    @Test
    void listIsEmptyByDefault(@TempDir Path dir) {
        assertTrue(new ShoppingListRepository(dir.resolve("s.json")).itemsOf(1L).isEmpty());
    }

    @Test
    void addAllSkipsDuplicatesAndReportsHowManyWereNew(@TempDir Path dir) {
        ShoppingListRepository repo = new ShoppingListRepository(dir.resolve("s.json"));
        assertEquals(2, repo.addAll(1L, List.of("egg", "milk")));
        assertEquals(1, repo.addAll(1L, List.of("milk", "flour")));
        assertEquals(List.of("egg", "milk", "flour"),
                repo.itemsOf(1L).stream().map(ShoppingListRepository.Item::ingredient).toList());
    }

    @Test
    void readdingAnItemKeepsItsCheckedState(@TempDir Path dir) {
        ShoppingListRepository repo = new ShoppingListRepository(dir.resolve("s.json"));
        repo.addAll(1L, List.of("egg"));
        repo.toggleChecked(1L, 0);
        assertEquals(0, repo.addAll(1L, List.of("egg")));
        assertTrue(repo.itemsOf(1L).get(0).checked());
    }

    @Test
    void toggleCheckedFlipsAndRejectsBadIndexes(@TempDir Path dir) {
        ShoppingListRepository repo = new ShoppingListRepository(dir.resolve("s.json"));
        repo.addAll(1L, List.of("egg", "milk"));
        assertTrue(repo.toggleChecked(1L, 1));
        assertFalse(repo.itemsOf(1L).get(0).checked());
        assertTrue(repo.itemsOf(1L).get(1).checked());
        assertTrue(repo.toggleChecked(1L, 1));
        assertFalse(repo.itemsOf(1L).get(1).checked());
        assertFalse(repo.toggleChecked(1L, 2));
        assertFalse(repo.toggleChecked(1L, -1));
        assertFalse(repo.toggleChecked(2L, 0));
    }

    @Test
    void clearCheckedRemovesOnlyCheckedItems(@TempDir Path dir) {
        ShoppingListRepository repo = new ShoppingListRepository(dir.resolve("s.json"));
        repo.addAll(1L, List.of("egg", "milk", "flour"));
        repo.toggleChecked(1L, 0);
        repo.toggleChecked(1L, 2);
        assertEquals(2, repo.clearChecked(1L));
        assertEquals(List.of("milk"), repo.itemsOf(1L).stream().map(ShoppingListRepository.Item::ingredient).toList());
        assertEquals(0, repo.clearChecked(1L));
        assertEquals(0, repo.clearChecked(2L));
    }

    @Test
    void listsAreScopedPerUser(@TempDir Path dir) {
        ShoppingListRepository repo = new ShoppingListRepository(dir.resolve("s.json"));
        repo.addAll(1L, List.of("egg"));
        repo.addAll(2L, List.of("milk"));
        assertEquals("egg", repo.itemsOf(1L).get(0).ingredient());
        assertEquals("milk", repo.itemsOf(2L).get(0).ingredient());
    }

    @Test
    void listSurvivesARestartIncludingCheckedState(@TempDir Path dir) {
        Path file = dir.resolve("s.json");
        ShoppingListRepository first = new ShoppingListRepository(file);
        first.addAll(1L, List.of("egg", "milk"));
        first.toggleChecked(1L, 0);
        List<ShoppingListRepository.Item> reloaded = new ShoppingListRepository(file).itemsOf(1L);
        assertEquals(List.of(new ShoppingListRepository.Item("egg", true), new ShoppingListRepository.Item("milk", false)),
                reloaded);
    }

    @Test
    void emptyFileLoadsAsNoLists(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("s.json");
        Files.writeString(file, "");
        assertTrue(new ShoppingListRepository(file).itemsOf(1L).isEmpty());
    }
}
