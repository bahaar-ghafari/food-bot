package com.foodbot.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.foodbot.util.AtomicFiles;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Telegram @usernames of people who have used the bot. Telegram can't resolve an arbitrary @username
 * to a user id, so the bot remembers the ones it has seen and lets people follow by @username.
 */
public class UsernameRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("usernames.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<HashMap<String, Long>>() {}.getType();

    private static final Type NAMES_TYPE = new TypeToken<HashMap<Long, String>>() {}.getType();

    private final Path dataFile;
    /** lower-cased username (no @) -> chat id */
    private final Map<String, Long> byUsername;
    private final Path namesFile;
    /** chat id -> Telegram full name, so people can be shown by name when Telegram won't resolve them. */
    private final Map<Long, String> namesById;

    public UsernameRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public UsernameRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.byUsername = load();
        this.namesFile = dataFile.resolveSibling("usernames-names.json");
        this.namesById = loadNames();
    }

    /** Remembers the user's Telegram full name (ignored when blank). */
    public synchronized void recordName(long chatId, String fullName) {
        if (fullName == null || fullName.isBlank() || fullName.equals(namesById.get(chatId))) {
            return;
        }
        namesById.put(chatId, fullName);
        try {
            AtomicFiles.writeString(namesFile, GSON.toJson(namesById, NAMES_TYPE));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + namesFile, e);
        }
    }

    public synchronized String findName(long chatId) {
        return namesById.get(chatId);
    }

    /** The last @username (no @) recorded for {@code chatId}, or null. */
    public synchronized String findUsername(long chatId) {
        return byUsername.entrySet().stream().filter(e -> e.getValue() == chatId)
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private Map<Long, String> loadNames() {
        if (!Files.exists(namesFile)) {
            return new HashMap<>();
        }
        try {
            Map<Long, String> loaded = GSON.fromJson(Files.readString(namesFile), NAMES_TYPE);
            return loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + namesFile, e);
        }
    }

    /** Remembers {@code username} for {@code chatId}, dropping the user's previous name and any other holder of it. */
    public synchronized void record(long chatId, String username) {
        String key = normalize(username);
        if (key != null && Long.valueOf(chatId).equals(byUsername.get(key))) {
            return;
        }
        boolean changed = byUsername.values().removeIf(id -> id == chatId);
        if (key != null) {
            byUsername.put(key, chatId);
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    public synchronized Long findChatId(String username) {
        String key = normalize(username);
        return key == null ? null : byUsername.get(key);
    }

    /** Lower-cased name without a leading @ or t.me link, or null when empty. */
    public static String normalize(String username) {
        if (username == null) {
            return null;
        }
        String name = username.trim().replaceFirst("(?i)^(https?://)?(www\\.)?t\\.me/", "");
        if (name.startsWith("@")) {
            name = name.substring(1);
        }
        return name.isEmpty() ? null : name.toLowerCase(Locale.ROOT);
    }

    private Map<String, Long> load() {
        if (!Files.exists(dataFile)) {
            return new HashMap<>();
        }
        try {
            Map<String, Long> loaded = GSON.fromJson(Files.readString(dataFile), MAP_TYPE);
            return loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + dataFile, e);
        }
    }

    private void save() {
        try {
            AtomicFiles.writeString(dataFile, GSON.toJson(byUsername, MAP_TYPE));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }
}
