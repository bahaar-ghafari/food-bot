package com.foodbot.food;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoriteRepositoryTest {

    @Test
    void nothingIsFavoriteByDefault(@TempDir Path dir) {
        FavoriteRepository repo = new FavoriteRepository(dir.resolve("fav.json"));
        assertFalse(repo.isFavorite(1L, "a"));
        assertTrue(repo.idsOf(1L).isEmpty());
    }

    @Test
    void countFavoritesCountsDistinctPeopleWhoStarredTheFood(@TempDir Path dir) {
        FavoriteRepository repo = new FavoriteRepository(dir.resolve("fav.json"));
        repo.toggle(1L, "a");
        repo.toggle(2L, "a");
        repo.toggle(2L, "b");
        assertEquals(2, repo.countFavorites("a"));
        assertEquals(1, repo.countFavorites("b"));
        repo.toggle(1L, "a");
        assertEquals(1, repo.countFavorites("a"));
        assertEquals(0, repo.countFavorites("zzz"));
    }

    @Test
    void toggleStarsThenUnstars(@TempDir Path dir) {
        FavoriteRepository repo = new FavoriteRepository(dir.resolve("fav.json"));
        assertTrue(repo.toggle(1L, "a"));
        assertTrue(repo.isFavorite(1L, "a"));
        assertFalse(repo.toggle(1L, "a"));
        assertFalse(repo.isFavorite(1L, "a"));
        assertTrue(repo.idsOf(1L).isEmpty());
    }

    @Test
    void favoritesAreScopedPerUserAndKeepStarOrder(@TempDir Path dir) {
        FavoriteRepository repo = new FavoriteRepository(dir.resolve("fav.json"));
        repo.toggle(1L, "b");
        repo.toggle(1L, "a");
        repo.toggle(2L, "c");
        assertEquals(List.of("b", "a"), repo.idsOf(1L));
        assertEquals(List.of("c"), repo.idsOf(2L));
        assertFalse(repo.isFavorite(2L, "a"));
    }

    @Test
    void favoritesSurviveARestart(@TempDir Path dir) {
        Path file = dir.resolve("fav.json");
        FavoriteRepository first = new FavoriteRepository(file);
        first.toggle(1L, "b");
        first.toggle(1L, "a");
        assertEquals(List.of("b", "a"), new FavoriteRepository(file).idsOf(1L));
    }

    @Test
    void emptyFileLoadsAsNoFavorites(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("fav.json");
        java.nio.file.Files.writeString(file, "");
        assertTrue(new FavoriteRepository(file).idsOf(1L).isEmpty());
    }
}
