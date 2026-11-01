package com.foodbot.social;

import com.foodbot.social.FollowRepository.FollowResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowRepositoryTest {

    @Test
    void everyoneIsVisibleByDefault(@TempDir Path dir) {
        assertTrue(new FollowRepository(dir.resolve("f.json")).isVisible(1L));
    }

    @Test
    void followingAVisibleUserTakesEffectImmediately(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        assertEquals(FollowResult.FOLLOWING, repo.follow(1L, 2L));
        assertEquals(List.of(1L), repo.followersOf(2L));
        assertEquals(List.of(2L), repo.followingOf(1L));
        assertEquals(FollowResult.ALREADY_FOLLOWING, repo.follow(1L, 2L));
    }

    @Test
    void cannotFollowYourself(@TempDir Path dir) {
        assertEquals(FollowResult.SELF, new FollowRepository(dir.resolve("f.json")).follow(1L, 1L));
    }

    @Test
    void followingAHiddenUserOnlyCreatesARequest(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        repo.setVisible(2L, false);
        assertEquals(FollowResult.REQUESTED, repo.follow(1L, 2L));
        assertEquals(FollowResult.ALREADY_REQUESTED, repo.follow(1L, 2L));
        assertTrue(repo.followersOf(2L).isEmpty());
        assertFalse(repo.isFollowing(1L, 2L));
    }

    @Test
    void approvingARequestMakesTheFollowerActive(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        repo.setVisible(2L, false);
        repo.follow(1L, 2L);
        assertTrue(repo.approve(2L, 1L));
        assertTrue(repo.isFollowing(1L, 2L));
        assertFalse(repo.approve(2L, 1L));
    }

    @Test
    void decliningARequestLeavesNoFollower(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        repo.setVisible(2L, false);
        repo.follow(1L, 2L);
        assertTrue(repo.decline(2L, 1L));
        assertFalse(repo.isFollowing(1L, 2L));
        assertEquals(FollowResult.REQUESTED, repo.follow(1L, 2L));
    }

    @Test
    void existingFollowersSurviveTheFolloweeGoingHidden(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        repo.follow(1L, 2L);
        repo.setVisible(2L, false);
        assertTrue(repo.isFollowing(1L, 2L));
    }

    @Test
    void unfollowRemovesTheFollower(@TempDir Path dir) {
        FollowRepository repo = new FollowRepository(dir.resolve("f.json"));
        repo.follow(1L, 2L);
        assertTrue(repo.unfollow(1L, 2L));
        assertFalse(repo.unfollow(1L, 2L));
        assertTrue(repo.followingOf(1L).isEmpty());
    }

    @Test
    void stateSurvivesReload(@TempDir Path dir) {
        Path file = dir.resolve("f.json");
        FollowRepository repo = new FollowRepository(file);
        repo.follow(1L, 2L);
        repo.setVisible(3L, false);
        repo.follow(1L, 3L);

        FollowRepository reloaded = new FollowRepository(file);
        assertTrue(reloaded.isFollowing(1L, 2L));
        assertFalse(reloaded.isVisible(3L));
        assertTrue(reloaded.approve(3L, 1L));
    }

    private static void runConcurrently(int n, java.util.function.IntConsumer task) throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(n);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                task.accept(idx);
                return null;
            }));
        }
        start.countDown();
        for (java.util.concurrent.Future<?> f : futures) {
            f.get(30, java.util.concurrent.TimeUnit.SECONDS);
        }
        pool.shutdown();
    }

    @Test
    void concurrentFollowsAreAllPersisted(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("f.json");
        FollowRepository repo = new FollowRepository(file);
        runConcurrently(50, i -> repo.follow(i + 100L, 1L));

        assertEquals(50, repo.followersOf(1L).size());
        assertEquals(50, new FollowRepository(file).followersOf(1L).size());
    }
}
