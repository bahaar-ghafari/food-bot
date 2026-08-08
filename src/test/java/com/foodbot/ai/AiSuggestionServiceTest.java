package com.foodbot.ai;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSuggestionServiceTest {

    @Test
    void disabledWithoutApiKey() {
        AiSuggestionService service = new AiSuggestionService(null);
        assertFalse(service.isEnabled());
        assertEquals(Optional.empty(), service.suggest(List.of("rice"), 30, true, Lang.EN));
    }

    @Test
    void disabledWithBlankApiKey() {
        assertFalse(new AiSuggestionService("  ").isEnabled());
    }

    @Test
    void enabledWithApiKey() {
        assertTrue(new AiSuggestionService("sk-test").isEnabled());
    }

    @Test
    void promptMentionsIngredientsTimeAndShoppingAbility() {
        String prompt = AiSuggestionService.buildPrompt(List.of("rice", "onion"), 20, false, Lang.EN);
        assertTrue(prompt.contains("rice, onion"));
        assertTrue(prompt.contains("20"));
        assertTrue(prompt.contains("cannot go shopping"));
    }

    @Test
    void promptReflectsShoppingAllowed() {
        String prompt = AiSuggestionService.buildPrompt(List.of("rice"), 45, true, Lang.EN);
        assertTrue(prompt.contains("can go buy"));
    }

    @Test
    void promptHandlesEmptyIngredientsPerLanguage() {
        assertTrue(AiSuggestionService.buildPrompt(List.of(), 30, true, Lang.EN).contains("nothing in particular"));
        assertTrue(AiSuggestionService.buildPrompt(List.of(), 30, true, Lang.FA).contains("هیچ‌چیز خاصی"));
    }

    @Test
    void systemPromptLocksResponseLanguage() {
        assertTrue(AiSuggestionService.systemPrompt(Lang.FA).contains("Persian"));
        assertTrue(AiSuggestionService.systemPrompt(Lang.EN).contains("English"));
    }
}
