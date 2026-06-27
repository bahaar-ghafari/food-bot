package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IngredientTranslationsTest {

    @Test
    void translatesKnownEnglishIngredientToPersian() {
        assertEquals("تخم مرغ", IngredientTranslations.translate("egg", Lang.FA));
    }

    @Test
    void translatesKnownPersianIngredientToEnglish() {
        assertEquals("egg", IngredientTranslations.translate("تخم مرغ", Lang.EN));
    }

    @Test
    void unknownIngredientPassesThroughUnchanged() {
        assertEquals("dragonfruit", IngredientTranslations.translate("dragonfruit", Lang.FA));
    }
}
