package com.foodbot.food;

import com.foodbot.lang.Lang;
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
        Food food = new Food("id-1", "Omelette", 10, "Breakfast", List.of("egg", "cheese"), 111L, 111L, null, Lang.EN);
        repo.add(food);

        Optional<Food> found = repo.findById("id-1");
        assertTrue(found.isPresent());
        assertEquals("Omelette", found.get().getName());
        assertTrue(repo.findVisibleTo(111L, Lang.EN).contains(food));
        assertTrue(repo.findOwnedBy(111L, Lang.EN).contains(food));
        assertFalse(repo.findGlobal(Lang.EN).contains(food));
    }

    @Test
    void globalFoodIsVisibleToEveryone(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food food = new Food("id-2", "Soup", 20, "MainCourse", List.of("water", "salt"), null, 111L, null, Lang.EN);
        repo.add(food);

        assertTrue(repo.findGlobal(Lang.EN).contains(food));
        assertTrue(repo.findVisibleTo(999L, Lang.EN).contains(food));
        assertFalse(repo.findOwnedBy(999L, Lang.EN).contains(food));
    }

    @Test
    void foodsAreScopedByLanguageRegardlessOfOwnership(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food enFood = new Food("id-en", "Soup", 20, "MainCourse", List.of("water"), null, 111L, null, Lang.EN);
        Food faFood = new Food("id-fa", "آش", 20, "MainCourse", List.of("آب"), null, 111L, null, Lang.FA);
        repo.add(enFood);
        repo.add(faFood);

        assertTrue(repo.findGlobal(Lang.EN).contains(enFood));
        assertFalse(repo.findGlobal(Lang.EN).contains(faFood));
        assertTrue(repo.findGlobal(Lang.FA).contains(faFood));
        assertFalse(repo.findGlobal(Lang.FA).contains(enFood));

        assertTrue(repo.findVisibleTo(111L, Lang.EN).contains(enFood));
        assertFalse(repo.findVisibleTo(111L, Lang.EN).contains(faFood));
    }

    @Test
    void updateReplacesFoodWithMatchingId(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        Food original = new Food("id-3", "Pasta", 15, "MainCourse", List.of("pasta"), null, 111L, null, Lang.EN);
        repo.add(original);

        Food updated = new Food("id-3", "Pasta Bolognese", 25, "MainCourse", List.of("pasta", "beef"), null, 111L,
                null, Lang.EN);
        repo.update(updated);

        Optional<Food> found = repo.findById("id-3");
        assertTrue(found.isPresent());
        assertEquals("Pasta Bolognese", found.get().getName());
        assertEquals(25, found.get().getPrepTimeMinutes());
        assertEquals(1, repo.findGlobal(Lang.EN).size());
    }

    @Test
    void deleteRemovesFoodById(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        repo.add(new Food("id-4", "Salad", 5, "Snack", List.of("lettuce"), null, 111L, null, Lang.EN));

        repo.delete("id-4");

        assertTrue(repo.findById("id-4").isEmpty());
        assertTrue(repo.findGlobal(Lang.EN).isEmpty());
    }

    @Test
    void findAllIngredientsAggregatesAcrossVisibleFoodsCaseInsensitively(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        repo.add(new Food("id-5", "Omelette", 10, "Breakfast", List.of("Egg", "cheese"), null, 111L, null, Lang.EN));
        repo.add(new Food("id-6", "Scramble", 8, "Breakfast", List.of("egg", "milk"), 222L, 222L, null, Lang.EN));

        List<String> visibleToOwner = repo.findAllIngredients(222L, Lang.EN);
        assertEquals(3, visibleToOwner.size());
        assertTrue(visibleToOwner.stream().anyMatch(i -> i.equalsIgnoreCase("egg")));

        List<String> visibleToStranger = repo.findAllIngredients(333L, Lang.EN);
        assertEquals(2, visibleToStranger.size());
    }

    @Test
    void findAllIngredientsExcludesOtherLanguageFoods(@TempDir Path tempDir) {
        FoodRepository repo = newRepo(tempDir);
        repo.add(new Food("id-en", "Omelette", 10, "Breakfast", List.of("egg"), null, 111L, null, Lang.EN));
        repo.add(new Food("id-fa", "املت", 10, "Breakfast", List.of("تخم مرغ"), null, 111L, null, Lang.FA));

        assertEquals(List.of("egg"), repo.findAllIngredients(111L, Lang.EN));
        assertEquals(List.of("تخم مرغ"), repo.findAllIngredients(111L, Lang.FA));
    }

    @Test
    void dataPersistsAcrossRepositoryInstances(@TempDir Path tempDir) {
        Path file = tempDir.resolve("foods.json");
        FoodRepository repo = new FoodRepository(file);
        repo.add(new Food("id-7", "Burger", 12, "MainCourse", List.of("beef", "bun"), null, 111L, null, Lang.EN));

        FoodRepository reloaded = new FoodRepository(file);
        assertEquals(1, reloaded.findGlobal(Lang.EN).size());
        assertEquals("Burger", reloaded.findGlobal(Lang.EN).get(0).getName());
    }

    @Test
    void recipeFieldPersistsAcrossRepositoryInstances(@TempDir Path tempDir) {
        Path file = tempDir.resolve("foods.json");
        FoodRepository repo = new FoodRepository(file);
        repo.add(new Food("id-8", "Soup", 20, "MainCourse", List.of("water"), null, 111L, "Boil water, add salt.",
                Lang.EN));

        FoodRepository reloaded = new FoodRepository(file);
        Optional<Food> found = reloaded.findById("id-8");
        assertTrue(found.isPresent());
        assertEquals("Boil water, add salt.", found.get().getRecipe());
    }

    @Test
    void legacyDataMissingIdGetsBackfilledAndPersisted(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("foods.json");
        Files.writeString(file, "[{\"name\":\"Legacy\",\"prepTimeMinutes\":10,\"category\":\"Other\",\"ingredients\":[\"rice\"]}]");

        FoodRepository repo = new FoodRepository(file);
        List<Food> all = repo.findGlobal(Lang.EN);
        assertEquals(1, all.size());
        assertTrue(all.get(0).getId() != null && !all.get(0).getId().isBlank());

        String savedJson = Files.readString(file);
        assertTrue(savedJson.contains("\"id\""));
        assertTrue(savedJson.contains("\"language\""));
    }

    @Test
    void legacyDataInfersLanguageFromPersianScriptInName(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("foods.json");
        Files.writeString(file,
                "[{\"id\":\"x1\",\"name\":\"قورمه سبزی\",\"prepTimeMinutes\":10,\"category\":\"Other\",\"ingredients\":[\"رشته\"]}]");

        FoodRepository repo = new FoodRepository(file);
        Optional<Food> found = repo.findById("x1");
        assertTrue(found.isPresent());
        assertEquals(Lang.FA, found.get().getLanguage());
    }

    @Test
    void legacyDataInfersEnglishLanguageWhenNameHasNoPersianScript(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("foods.json");
        Files.writeString(file,
                "[{\"id\":\"x2\",\"name\":\"Burger\",\"prepTimeMinutes\":10,\"category\":\"Other\",\"ingredients\":[\"beef\"]}]");

        FoodRepository repo = new FoodRepository(file);
        Optional<Food> found = repo.findById("x2");
        assertTrue(found.isPresent());
        assertEquals(Lang.EN, found.get().getLanguage());
    }
}
