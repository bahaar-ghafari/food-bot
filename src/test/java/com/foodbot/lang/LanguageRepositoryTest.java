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
}
