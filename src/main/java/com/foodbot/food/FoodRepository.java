package com.foodbot.food;

import com.foodbot.lang.Lang;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

public class FoodRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("foods.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FOOD_LIST_TYPE = new TypeToken<ArrayList<Food>>() {}.getType();

    private final Path dataFile;
    private final List<Food> foods;

    public FoodRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public FoodRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.foods = load();
    }

    public synchronized void add(Food food) {
        foods.add(food);
        save();
    }

    public synchronized Optional<Food> findById(String id) {
        return foods.stream().filter(food -> food.getId().equals(id)).findFirst();
    }

    public synchronized void update(Food updated) {
        for (int i = 0; i < foods.size(); i++) {
            if (foods.get(i).getId().equals(updated.getId())) {
                foods.set(i, updated);
                save();
                return;
            }
        }
    }

    public synchronized void delete(String id) {
        if (foods.removeIf(food -> food.getId().equals(id))) {
            save();
        }
    }

    /**
     * Re-reads {@code dataFile} from disk, replacing the in-memory list - picks up changes
     * made outside the running bot (e.g. a manually edited or restored foods.json).
     */
    public synchronized void reload() {
        foods.clear();
        foods.addAll(load());
    }

    public synchronized List<Food> findVisibleTo(long chatId, Lang lang) {
        List<Food> result = new ArrayList<>();
        for (Food food : foods) {
            if (food.getLanguage() == lang && (food.getOwnerChatId() == null || food.getOwnerChatId() == chatId)) {
                result.add(food);
            }
        }
        return result;
    }

    public synchronized List<Food> findGlobal(Lang lang) {
        List<Food> result = new ArrayList<>();
        for (Food food : foods) {
            if (food.getLanguage() == lang && food.getOwnerChatId() == null) {
                result.add(food);
            }
        }
        return result;
    }

    public synchronized List<Food> findOwnedBy(long chatId, Lang lang) {
        List<Food> result = new ArrayList<>();
        for (Food food : foods) {
            if (food.getLanguage() == lang && food.getOwnerChatId() != null && food.getOwnerChatId() == chatId) {
                result.add(food);
            }
        }
        return result;
    }

    public synchronized List<String> findAllIngredients(long chatId, Lang lang) {
        TreeSet<String> ingredients = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Food food : findVisibleTo(chatId, lang)) {
            ingredients.addAll(food.getIngredients());
        }
        return new ArrayList<>(ingredients);
    }

    private List<Food> load() {
        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(dataFile);
            List<Food> loaded = GSON.fromJson(json, FOOD_LIST_TYPE);
            if (loaded == null) {
                return new ArrayList<>();
            }
            return backfillMissingFields(loaded);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + dataFile, e);
        }
    }

    private List<Food> backfillMissingFields(List<Food> loaded) {
        List<Food> result = new ArrayList<>();
        boolean changed = false;
        for (Food food : loaded) {
            String id = food.getId();
            Lang language = food.getLanguage();
            if (id == null || language == null) {
                id = (id == null) ? UUID.randomUUID().toString() : id;
                language = (language == null) ? inferLanguage(food.getName()) : language;
                result.add(new Food(id, food.getName(), food.getPrepTimeMinutes(), food.getCategory(),
                        food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe(),
                        language));
                changed = true;
            } else {
                result.add(food);
            }
        }
        if (changed) {
            saveSnapshot(result);
        }
        return result;
    }

    private Lang inferLanguage(String name) {
        boolean hasPersian = name != null && name.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        return hasPersian ? Lang.FA : Lang.EN;
    }

    private void saveSnapshot(List<Food> snapshot) {
        try {
            Files.writeString(dataFile, GSON.toJson(snapshot));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }

    private void save() {
        saveSnapshot(foods);
    }
}
