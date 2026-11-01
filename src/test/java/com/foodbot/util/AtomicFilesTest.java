package com.foodbot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicFilesTest {

    @TempDir
    Path dir;

    @Test
    void createsNewFile() throws IOException {
        Path file = dir.resolve("data.json");
        AtomicFiles.writeString(file, "one");
        assertEquals("one", Files.readString(file));
    }

    @Test
    void replacesExistingFileAndLeavesNoTempFiles() throws IOException {
        Path file = dir.resolve("data.json");
        AtomicFiles.writeString(file, "one");
        AtomicFiles.writeString(file, "two");
        assertEquals("two", Files.readString(file));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }
}
