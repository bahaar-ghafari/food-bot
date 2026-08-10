package com.foodbot.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TelegramConfig's public getters read from a real .env file (untracked, machine-specific)
 * and process environment variables, so asserting on them directly would be flaky between a
 * developer's machine and CI. These tests instead exercise firstNonBlank(), the pure
 * precedence rule every getter is built on: .env value wins, then application.properties,
 * then the process environment (or a literal default), skipping any that are null or blank.
 */
class TelegramConfigTest {

    @Test
    void firstArgumentWinsWhenPresent() {
        assertEquals("dotenv-value", TelegramConfig.firstNonBlank("dotenv-value", "properties-value", "env-value"));
    }

    @Test
    void fallsThroughBlankAndNullValuesInOrder() {
        assertEquals("properties-value", TelegramConfig.firstNonBlank(null, "properties-value", "env-value"));
        assertEquals("properties-value", TelegramConfig.firstNonBlank("", "properties-value", "env-value"));
        assertEquals("properties-value", TelegramConfig.firstNonBlank("   ", "properties-value", "env-value"));
    }

    @Test
    void fallsThroughToLastArgumentWhenOthersAreMissing() {
        assertEquals("env-value", TelegramConfig.firstNonBlank(null, "", "env-value"));
    }

    @Test
    void returnsNullWhenEverythingIsMissingOrBlank() {
        assertNull(TelegramConfig.firstNonBlank(null, "", "   "));
    }

    @Test
    void handlesNoArgumentsAtAll() {
        assertNull(TelegramConfig.firstNonBlank());
    }
}
