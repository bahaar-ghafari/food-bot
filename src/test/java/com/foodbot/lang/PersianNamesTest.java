package com.foodbot.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersianNamesTest {
    @Test
    void knownNamesUseTheDictionaryRegardlessOfCase() {
        assertEquals("بهار", PersianNames.localize("Bahar"));
        assertEquals("بهار", PersianNames.localize("BAHAR"));
    }

    @Test
    void fullNamesAndUsernamesAreHandledWordByWord() {
        assertEquals("بهار (@bahar_g)", PersianNames.localize("Bahar (@bahar_g)"));
        assertEquals("علی سارا", PersianNames.localize("Ali Sara"));
    }

    @Test
    void persianAndDigitsAreLeftAlone() {
        assertEquals("بهار ۲", PersianNames.localize("بهار ۲"));
        assertEquals("user123", PersianNames.localize("user123"));
    }

    @Test
    void unknownNamesGetAPersianScriptApproximation() {
        String result = PersianNames.localize("Xerxes");
        assertTrue(result.chars().noneMatch(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')), result);
        assertTrue(PersianNames.localize("Khosro").startsWith("خ"));
    }

    @Test
    void onlyPersianReadersGetTheConversion() {
        assertEquals("Bahar", PersianNames.forLang(Lang.EN, "Bahar"));
        assertEquals("بهار", PersianNames.forLang(Lang.FA, "Bahar"));
        assertNull(PersianNames.forLang(Lang.FA, null));
    }
}
