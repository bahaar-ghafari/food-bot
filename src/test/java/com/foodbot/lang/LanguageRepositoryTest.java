package com.foodbot.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LanguageRepositoryTest {

    @Test
    void unknownChatHasNoLanguage(@TempDir Path tempDir) {
        LanguageRepository repo = new LanguageRepository(tempDir.resolve("languages.json"));
        assertNull(repo.get(123L));
    }

    @Test
    void setLanguagePersistsAcrossInstances(@TempDir Path tempDir) {
        Path file = tempDir.resolve("languages.json");
        LanguageRepository repo = new LanguageRepository(file);
        repo.set(123L, Lang.FA);

        LanguageRepository reloaded = new LanguageRepository(file);
        assertEquals(Lang.FA, reloaded.get(123L));
    }

    @Test
    void settingLanguageAgainOverwritesPreviousChoice(@TempDir Path tempDir) {
        LanguageRepository repo = new LanguageRepository(tempDir.resolve("languages.json"));
        repo.set(123L, Lang.EN);
        repo.set(123L, Lang.FA);
        assertEquals(Lang.FA, repo.get(123L));
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
    void concurrentSetsAreAllPersisted(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("languages.json");
        LanguageRepository repo = new LanguageRepository(file);
        runConcurrently(50, i -> repo.set(i, i % 2 == 0 ? Lang.EN : Lang.FA));

        LanguageRepository reloaded = new LanguageRepository(file);
        for (int i = 0; i < 50; i++) {
            assertEquals(i % 2 == 0 ? Lang.EN : Lang.FA, reloaded.get(i));
        }
    }
}
