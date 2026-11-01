package com.foodbot.food;

import com.foodbot.util.AtomicFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Per-user shopping list: ingredient keys in the order they were added, each checked off or not. */
public class ShoppingListRepository {
    public record Item(String ingredient, boolean checked) {
    }

    private static final Path DEFAULT_DATA_FILE = Path.of("shopping.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STATE_TYPE = new TypeToken<TreeMap<Long, LinkedHashMap<String, Boolean>>>() {
    }.getType();

    private final Path dataFile;
    private final Map<Long, Map<String, Boolean>> lists;

    public ShoppingListRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public ShoppingListRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.lists = load();
    }

    /** Adds the ingredients that aren't already listed. Returns how many were new. */
    public synchronized int addAll(long chatId, Collection<String> ingredients) {
        Map<String, Boolean> list = lists.computeIfAbsent(chatId, k -> new LinkedHashMap<>());
        int added = 0;
        for (String ingredient : ingredients) {
            if (list.putIfAbsent(ingredient, false) == null) {
                added++;
            }
        }
        if (list.isEmpty()) {
            lists.remove(chatId);
        }
        if (added > 0) {
            save();
        }
        return added;
    }

    /** Flips the checked state of the item at {@code index}. Returns false if there is no such item. */
    public synchronized boolean toggleChecked(long chatId, int index) {
        Map<String, Boolean> list = lists.get(chatId);
        if (list == null || index < 0 || index >= list.size()) {
            return false;
        }
        String ingredient = new ArrayList<>(list.keySet()).get(index);
        list.put(ingredient, !list.get(ingredient));
        save();
        return true;
    }

    /** Drops every checked-off item. Returns how many were removed. */
    public synchronized int clearChecked(long chatId) {
        Map<String, Boolean> list = lists.get(chatId);
        if (list == null) {
            return 0;
        }
        int before = list.size();
        list.values().removeIf(checked -> checked);
        if (list.isEmpty()) {
            lists.remove(chatId);
        }
        int removed = before - list.size();
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public synchronized List<Item> itemsOf(long chatId) {
        List<Item> items = new ArrayList<>();
        lists.getOrDefault(chatId, Map.of()).forEach((ingredient, checked) -> items.add(new Item(ingredient, checked)));
        return items;
    }

    private Map<Long, Map<String, Boolean>> load() {
        if (!Files.exists(dataFile)) {
            return new TreeMap<>();
        }
        try {
            Map<Long, LinkedHashMap<String, Boolean>> loaded = GSON.fromJson(Files.readString(dataFile), STATE_TYPE);
            Map<Long, Map<String, Boolean>> result = new TreeMap<>();
            if (loaded != null) {
                loaded.forEach((chatId, items) -> {
                    if (items != null && !items.isEmpty()) {
                        result.put(chatId, items);
                    }
                });
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + dataFile, e);
        }
    }

    private void save() {
        try {
            AtomicFiles.writeString(dataFile, GSON.toJson(lists));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }
}
