package com.foodbot.food;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FoodRepository {
    private static final Path DATA_FILE = Path.of("foods.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type FOOD_LIST_TYPE = new TypeToken<ArrayList<Food>>() {}.getType();

    private final List<Food> foods;

    public FoodRepository() {
        this.foods = load();
    }

    public synchronized void add(Food food) {
        foods.add(food);
        save();
    }

    public synchronized List<Food> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(foods));
    }

    private List<Food> load() {
        if (!Files.exists(DATA_FILE)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(DATA_FILE);
            List<Food> loaded = GSON.fromJson(json, FOOD_LIST_TYPE);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + DATA_FILE, e);
        }
    }

    private void save() {
        try {
            Files.writeString(DATA_FILE, GSON.toJson(foods));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + DATA_FILE, e);
        }
    }
}
