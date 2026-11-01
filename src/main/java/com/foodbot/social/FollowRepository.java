package com.foodbot.social;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.foodbot.util.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Who follows whom. Every user is visible by default - anyone can follow a visible user
 * straight away. Following a hidden user only creates a pending request the hidden user
 * has to approve.
 */
public class FollowRepository {
    public enum FollowResult { FOLLOWING, REQUESTED, ALREADY_FOLLOWING, ALREADY_REQUESTED, SELF }

    private static final Path DEFAULT_DATA_FILE = Path.of("follows.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Persisted shape; maps are keyed by the followee's chat id. */
    private static class State {
        Set<Long> hidden = new HashSet<>();
        Map<Long, Set<Long>> followers = new HashMap<>();
        Map<Long, Set<Long>> pending = new HashMap<>();
        /** followee -> followers who muted them (still following, but no notifications). */
        Map<Long, Set<Long>> muted = new HashMap<>();
        /** Followers who want one daily digest instead of a message per recipe. */
        Set<Long> digest = new HashSet<>();
        /** follower -> ids of recipes waiting for their next digest. */
        Map<Long, List<String>> queued = new HashMap<>();
    }

    private final Path dataFile;
    private final State state;

    public FollowRepository() {
        this(DEFAULT_DATA_FILE);
    }

    public FollowRepository(Path dataFile) {
        this.dataFile = dataFile;
        this.state = load();
    }

    public synchronized boolean isVisible(long chatId) {
        return !state.hidden.contains(chatId);
    }

    public synchronized void setVisible(long chatId, boolean visible) {
        boolean changed = visible ? state.hidden.remove(chatId) : state.hidden.add(chatId);
        if (changed) {
            save();
        }
    }

    public synchronized FollowResult follow(long follower, long followee) {
        if (follower == followee) {
            return FollowResult.SELF;
        }
        if (followersOf(state.followers, followee).contains(follower)) {
            return FollowResult.ALREADY_FOLLOWING;
        }
        if (isVisible(followee)) {
            state.followers.computeIfAbsent(followee, k -> new HashSet<>()).add(follower);
            save();
            return FollowResult.FOLLOWING;
        }
        if (!state.pending.computeIfAbsent(followee, k -> new HashSet<>()).add(follower)) {
            return FollowResult.ALREADY_REQUESTED;
        }
        save();
        return FollowResult.REQUESTED;
    }

    /** Turns a pending request into a follow. Returns false when there was no such request. */
    public synchronized boolean approve(long followee, long follower) {
        if (!removePending(followee, follower)) {
            return false;
        }
        state.followers.computeIfAbsent(followee, k -> new HashSet<>()).add(follower);
        save();
        return true;
    }

    /** Drops a pending request. Returns false when there was no such request. */
    public synchronized boolean decline(long followee, long follower) {
        if (!removePending(followee, follower)) {
            return false;
        }
        save();
        return true;
    }

    public synchronized boolean unfollow(long follower, long followee) {
        Set<Long> set = state.followers.get(followee);
        if (set == null || !set.remove(follower)) {
            return false;
        }
        if (set.isEmpty()) {
            state.followers.remove(followee);
        }
        Set<Long> mutedSet = state.muted.get(followee);
        if (mutedSet != null && mutedSet.remove(follower) && mutedSet.isEmpty()) {
            state.muted.remove(followee);
        }
        save();
        return true;
    }

    public synchronized boolean isMuted(long follower, long followee) {
        return followersOf(state.muted, followee).contains(follower);
    }

    /** Mutes or unmutes a followee. Returns false when the follower doesn't follow them. */
    public synchronized boolean setMuted(long follower, long followee, boolean muted) {
        if (!isFollowing(follower, followee)) {
            return false;
        }
        if (muted) {
            if (state.muted.computeIfAbsent(followee, k -> new HashSet<>()).add(follower)) {
                save();
            }
        } else {
            Set<Long> set = state.muted.get(followee);
            if (set != null && set.remove(follower)) {
                if (set.isEmpty()) {
                    state.muted.remove(followee);
                }
                save();
            }
        }
        return true;
    }

    /** Followers of the chat who should be told about a new recipe (everyone but the muted ones). */
    public synchronized List<Long> notifiableFollowersOf(long chatId) {
        Set<Long> mutedSet = followersOf(state.muted, chatId);
        List<Long> result = new ArrayList<>();
        for (long follower : followersOf(state.followers, chatId)) {
            if (!mutedSet.contains(follower)) {
                result.add(follower);
            }
        }
        return result;
    }

    public synchronized boolean isDigest(long follower) {
        return state.digest.contains(follower);
    }

    public synchronized void setDigest(long follower, boolean digest) {
        boolean changed = digest ? state.digest.add(follower) : state.digest.remove(follower);
        if (changed) {
            save();
        }
    }

    public synchronized void queueForDigest(long follower, String foodId) {
        state.queued.computeIfAbsent(follower, k -> new ArrayList<>()).add(foodId);
        save();
    }

    /** Removes and returns everything queued for every follower, keyed by follower. */
    public synchronized Map<Long, List<String>> drainDigests() {
        if (state.queued.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> drained = new HashMap<>(state.queued);
        state.queued.clear();
        save();
        return drained;
    }

    public synchronized boolean isFollowing(long follower, long followee) {
        return followersOf(state.followers, followee).contains(follower);
    }

    public synchronized List<Long> followersOf(long chatId) {
        return new ArrayList<>(followersOf(state.followers, chatId));
    }

    public synchronized List<Long> followingOf(long chatId) {
        List<Long> result = new ArrayList<>();
        state.followers.forEach((followee, set) -> {
            if (set.contains(chatId)) {
                result.add(followee);
            }
        });
        return result;
    }

    private boolean removePending(long followee, long follower) {
        Set<Long> set = state.pending.get(followee);
        if (set == null || !set.remove(follower)) {
            return false;
        }
        if (set.isEmpty()) {
            state.pending.remove(followee);
        }
        return true;
    }

    private static Set<Long> followersOf(Map<Long, Set<Long>> map, long followee) {
        return map.getOrDefault(followee, Set.of());
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
            if (loaded.hidden == null) loaded.hidden = new HashSet<>();
            if (loaded.followers == null) loaded.followers = new HashMap<>();
            if (loaded.pending == null) loaded.pending = new HashMap<>();
            if (loaded.muted == null) loaded.muted = new HashMap<>();
            if (loaded.digest == null) loaded.digest = new HashSet<>();
            if (loaded.queued == null) loaded.queued = new HashMap<>();
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
