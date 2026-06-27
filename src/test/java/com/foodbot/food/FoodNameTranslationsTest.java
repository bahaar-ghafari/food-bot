package com.foodbot.food;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoodNameTranslationsTest {

    @Test
    void translatesKnownPersianDishToEnglish() {
        assertEquals("Ghormeh Sabzi", FoodNameTranslations.translate("قورمه سبزی", Lang.EN));
    }

    @Test
    void translatesKnownEnglishDishToPersian() {
        assertEquals("قورمه سبزی", FoodNameTranslations.translate("Ghormeh Sabzi", Lang.FA));
    }

    @Test
    void unknownNamePassesThroughUnchanged() {
        assertEquals("tommegg", FoodNameTranslations.translate("tommegg", Lang.FA));
    }
}
