package com.foodbot.lang;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LanguageRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("languages.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<HashMap<Long, Lang>>() {}.getType();

    private final Path dataFile;
    private final Map<Long, Lang> languages;

    public LanguageRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public LanguageRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.languages = load();
    }

    public synchronized Lang get(long chatId) {
        return languages.get(chatId);
    }

    public synchronized void set(long chatId, Lang lang) {
        languages.put(chatId, lang);
        save();
    }

    private Map<Long, Lang> load() {
        if (!Files.exists(dataFile)) {
            return new HashMap<>();
        }
        try {
            String json = Files.readString(dataFile);
            Map<Long, Lang> loaded = GSON.fromJson(json, MAP_TYPE);
            return loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + dataFile, e);
        }
    }

    private void save() {
        try {
            Files.writeString(dataFile, GSON.toJson(languages, MAP_TYPE));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }
}
