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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Per-user bookmarked recipes, stored as food ids in the order they were starred. */
public class FavoriteRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("favorites.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STATE_TYPE = new TypeToken<TreeMap<Long, LinkedHashSet<String>>>() {
    }.getType();

    private final Path dataFile;
    private final Map<Long, Set<String>> favorites;

    public FavoriteRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public FavoriteRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.favorites = load();
    }

    public synchronized boolean isFavorite(long chatId, String foodId) {
        return favorites.getOrDefault(chatId, Set.of()).contains(foodId);
    }

    /** Stars the food if it wasn't a favorite, un-stars it otherwise. Returns the new state. */
    public synchronized boolean toggle(long chatId, String foodId) {
        Set<String> ids = favorites.computeIfAbsent(chatId, k -> new LinkedHashSet<>());
        boolean nowFavorite = ids.add(foodId);
        if (!nowFavorite) {
            ids.remove(foodId);
            if (ids.isEmpty()) {
                favorites.remove(chatId);
            }
        }
        save();
        return nowFavorite;
    }

    /** How many people have starred this food. */
    public synchronized int countFavorites(String foodId) {
        int count = 0;
        for (Set<String> ids : favorites.values()) {
            if (ids.contains(foodId)) {
                count++;
            }
        }
        return count;
    }

    /** Food ids in the order they were starred, oldest first. */
    public synchronized List<String> idsOf(long chatId) {
        return new ArrayList<>(favorites.getOrDefault(chatId, Set.of()));
    }

    private Map<Long, Set<String>> load() {
        if (!Files.exists(dataFile)) {
            return new TreeMap<>();
        }
        try {
            Map<Long, LinkedHashSet<String>> loaded = GSON.fromJson(Files.readString(dataFile), STATE_TYPE);
            Map<Long, Set<String>> result = new TreeMap<>();
            if (loaded != null) {
                loaded.forEach((chatId, ids) -> {
                    if (ids != null && !ids.isEmpty()) {
                        result.put(chatId, ids);
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
            AtomicFiles.writeString(dataFile, GSON.toJson(favorites));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }
}
