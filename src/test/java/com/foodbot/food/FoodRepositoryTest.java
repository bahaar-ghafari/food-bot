package com.foodbot.food;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodRepositoryTest {

    private FoodRepository newRepo(Path tempDir) {
        return new FoodRepository(tempDir.resolve("foods.json"));
    }

    @Test
    void addedFoodIsFoundByIdAndVisibleToCreator(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food food = new Food("id-1", "Omelette", 10, "Breakfast", List.of("egg", "cheese"), 111L, 111L);
        repo.add(food);

        Optional<Food> found = repo.findById("id-1");
        assertTrue(found.isPresent());
        assertEquals("Omelette", found.get().getName());
        assertTrue(repo.findVisibleTo(111L).contains(food));
        assertTrue(repo.findOwnedBy(111L).contains(food));
        assertFalse(repo.findGlobal().contains(food));
    }

    @Test
    void globalFoodIsVisibleToEveryone(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food food = new Food("id-2", "Soup", 20, "MainCourse", List.of("water", "salt"), null, 111L);
        repo.add(food);

        assertTrue(repo.findGlobal().contains(food));
        assertTrue(repo.findVisibleTo(999L).contains(food));
        assertFalse(repo.findOwnedBy(999L).contains(food));
    }

    @Test
    void updateReplacesFoodWithMatchingId(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food original = new Food("id-3", "Pasta", 15, "MainCourse", List.of("pasta"), null, 111L);
        repo.add(original);

        Food updated = new Food("id-3", "Pasta Bolognese", 25, "MainCourse", List.of("pasta", "beef"), null, 111L);
        repo.update(updated);

        Optional<Food> found = repo.findById("id-3");
        assertTrue(found.isPresent());
        assertEquals("Pasta Bolognese", found.get().getName());
        assertEquals(25, found.get().getPrepTimeMinutes());
        assertEquals(1, repo.findGlobal().size());
    }

    @Test
    void deleteRemovesFoodById(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        repo.add(new Food("id-4", "Salad", 5, "Snack", List.of("lettuce"), null, 111L));

        repo.delete("id-4");

        assertTrue(repo.findById("id-4").isEmpty());
        assertTrue(repo.findGlobal().isEmpty());
    }

    @Test
    void findAllIngredientsAggregatesAcrossVisibleFoodsCaseInsensitively(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        repo.add(new Food("id-5", "Omelette", 10, "Breakfast", List.of("Egg", "cheese"), null, 111L));
        repo.add(new Food("id-6", "Scramble", 8, "Breakfast", List.of("egg", "milk"), 222L, 222L));

        List<String> visibleToOwner = repo.findAllIngredients(222L);
        assertEquals(3, visibleToOwner.size());
        assertTrue(visibleToOwner.stream().anyMatch(i -> i.equalsIgnoreCase("egg")));

        List<String> visibleToStranger = repo.findAllIngredients(333L);
        assertEquals(2, visibleToStranger.size());
    }

    @Test
    void dataPersistsAcrossRepositoryInstances(@TempDir Path tempDir) {
        Path file = tempDir.resolve("foods.json");
        FoodRepository repo = new FoodRepository(file);
        repo.add(new Food("id-7", "Burger", 12, "MainCourse", List.of("beef", "bun"), null, 111L));

        FoodRepository reloaded = new FoodRepository(file);
        assertEquals(1, reloaded.findGlobal().size());
        assertEquals("Burger", reloaded.findGlobal().get(0).getName());
    }

    @Test
    void legacyDataMissingIdGetsBackfilledAndPersisted(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("foods.json");
        Files.writeString(file, "[{\"name\":\"Legacy\",\"prepTimeMinutes\":10,\"category\":\"Other\",\"ingredients\":[\"rice\"]}]");

        FoodRepository repo = new FoodRepository(file);
        List<Food> all = repo.findGlobal();
        assertEquals(1, all.size());
        assertTrue(all.get(0).getId() != null && !all.get(0).getId().isBlank());

        String savedJson = Files.readString(file);
        assertTrue(savedJson.contains("\"id\""));
    }
}
