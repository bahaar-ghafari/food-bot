package com.foodbot.importer;

import com.foodbot.lang.Lang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Reads the text in an image with the free, open-source Tesseract OCR engine, run as a local process (it's
 * installed in the Docker image, see the Dockerfile). Nothing leaves the server and nothing is billed.
 */
public class TesseractOcr {
    private static final long TIMEOUT_SECONDS = 60;
    /** Keeps a burst of screenshots from swamping the small VPS: OCR is CPU- and memory-heavy. */
    private final Semaphore slots = new Semaphore(2);
    private final String command;
    private volatile Set<String> installedLanguages;

    public TesseractOcr() {
        this("tesseract");
    }

    TesseractOcr(String command) {
        this.command = command;
    }

    /** Whether the tesseract binary is installed; checked once. */
    public boolean isAvailable() {
        return !languages().isEmpty();
    }

    private Set<String> languages() {
        Set<String> langs = installedLanguages;
        if (langs == null) {
            langs = Set.of();
            try {
                Process p = new ProcessBuilder(command, "--list-langs").redirectErrorStream(true).start();
                String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    langs = out.lines().skip(1).map(String::strip).filter(l -> !l.isEmpty())
                            .collect(Collectors.toUnmodifiableSet());
                }
            } catch (IOException e) {
                // not installed (e.g. running outside Docker)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            installedLanguages = langs;
        }
        return langs;
    }

    /** Persian users get Persian first (most of their screenshots), with English for mixed text. */
    static String languageArg(Lang lang, Set<String> installed) {
        List<String> wanted = lang == Lang.FA ? List.of("fas", "eng") : List.of("eng");
        String arg = wanted.stream().filter(installed::contains).collect(Collectors.joining("+"));
        return arg.isEmpty() ? null : arg;
    }

    public Optional<String> read(byte[] image, Lang lang) {
        String languageArg = languageArg(lang, languages());
        if (languageArg == null) {
            return Optional.empty();
        }
        Path file = null;
        boolean acquired = false;
        try {
            slots.acquire();
            acquired = true;
            file = Files.createTempFile("recipe-", ".img");
            Files.write(file, image);
            Process p = new ProcessBuilder(command, file.toString(), "stdout", "-l", languageArg, "--psm", "3")
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            byte[] out = p.getInputStream().readAllBytes();
            if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return Optional.empty();
            }
            String text = new String(out, StandardCharsets.UTF_8).strip();
            return p.exitValue() == 0 && !text.isEmpty() ? Optional.of(text) : Optional.empty();
        } catch (IOException e) {
            System.err.println("OCR failed: " + e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (acquired) {
                slots.release();
            }
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                    // temp dir is cleaned eventually
                }
            }
        }
    }
}
