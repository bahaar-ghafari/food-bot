package com.foodbot.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void everyKeyHasNonBlankTextForBothLanguages() {
        for (String key : Messages.keys()) {
            String en = Messages.get(Lang.EN, key);
            String fa = Messages.get(Lang.FA, key);
            assertNotNull(en, "Missing EN text for " + key);
            assertNotNull(fa, "Missing FA text for " + key);
            assertFalse(en.isBlank(), "Blank EN text for " + key);
            assertFalse(fa.isBlank(), "Blank FA text for " + key);
        }
    }

    @Test
    void unknownKeyFallsBackToTheKeyItself() {
        assertTrue(Messages.get(Lang.EN, "no.such.key").equals("no.such.key"));
    }

    @Test
    void formatPlaceholdersAreSubstituted() {
        String result = Messages.get(Lang.EN, "addfood.saved_message", "Omelette");
        assertTrue(result.contains("Omelette"));
    }
}
