package com.foodbot.social;

import com.foodbot.util.AtomicFiles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Shopping-list partners: two users linked 1:1 who share one list. Each side can also give the other a
 * private nickname (what "I" call "my partner").
 */
public class PartnerRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("partners.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Serialized form of the repository. */
    private static class State {
        /** chat id -> partner's chat id (both directions are stored) */
        Map<Long, Long> partners = new HashMap<>();
        /** chat id -> the name that user gave their partner */
        Map<Long, String> nicknames = new HashMap<>();
    }

    private final Path dataFile;
    private final State state;

    public PartnerRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public PartnerRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.state = load();
    }

    /**
     * Links the two users as partners, ending any partnership either already had. Returns false when
     * they are the same user.
     */
    public synchronized boolean link(long a, long b) {
        if (a == b) {
            return false;
        }
        unlinkInternal(a);
        unlinkInternal(b);
        state.partners.put(a, b);
        state.partners.put(b, a);
        save();
        return true;
    }

    /** Ends the user's partnership (both sides, nicknames included). Returns the former partner, or null. */
    public synchronized Long unlink(long chatId) {
        Long former = unlinkInternal(chatId);
        if (former != null) {
            save();
        }
        return former;
    }

    public synchronized Long partnerOf(long chatId) {
        return state.partners.get(chatId);
    }

    /** The chat id the user's shopping list is stored under: shared by both partners, so the lower id. */
    public synchronized long listOwner(long chatId) {
        Long partner = state.partners.get(chatId);
        return partner == null ? chatId : Math.min(chatId, partner);
    }

    /** Sets what {@code chatId} calls their partner; a blank name clears it. */
    public synchronized void setNickname(long chatId, String name) {
        if (!state.partners.containsKey(chatId)) {
            return;
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            state.nicknames.remove(chatId);
        } else {
            state.nicknames.put(chatId, trimmed);
        }
        save();
    }

    /** What {@code chatId} calls their partner, or null when no name was given. */
    public synchronized String nicknameOf(long chatId) {
        return state.nicknames.get(chatId);
    }

    private Long unlinkInternal(long chatId) {
        Long former = state.partners.remove(chatId);
        state.nicknames.remove(chatId);
        if (former != null) {
            state.partners.remove(former);
            state.nicknames.remove(former);
        }
        return former;
    }

    private State load() {
        if (!Files.exists(dataFile)) {
            return new State();
        }
        try {
            State loaded = GSON.fromJson(Files.readString(dataFile), State.class);
            if (loaded == null) {
                return new State();
            }
            if (loaded.partners == null) {
                loaded.partners = new HashMap<>();
            }
            if (loaded.nicknames == null) {
                loaded.nicknames = new HashMap<>();
            }
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + dataFile, e);
        }
    }

    private void save() {
        try {
            AtomicFiles.writeString(dataFile, GSON.toJson(state));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save " + dataFile, e);
        }
    }
}
