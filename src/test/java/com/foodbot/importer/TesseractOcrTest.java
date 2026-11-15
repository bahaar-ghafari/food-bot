package com.foodbot.importer;

import com.foodbot.lang.Lang;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesseractOcrTest {
    @Test
    void persianUsersReadPersianThenEnglish() {
        assertEquals("fas+eng", TesseractOcr.languageArg(Lang.FA, Set.of("eng", "fas", "osd")));
        assertEquals("eng", TesseractOcr.languageArg(Lang.EN, Set.of("eng", "fas")));
    }

    @Test
    void missingLanguageModelsAreLeftOut() {
        assertEquals("eng", TesseractOcr.languageArg(Lang.FA, Set.of("eng")));
        assertNull(TesseractOcr.languageArg(Lang.EN, Set.of()));
    }

    @Test
    void missingBinaryMeansNoOcrInsteadOfACrash() {
        TesseractOcr ocr = new TesseractOcr("definitely-not-a-real-tesseract-binary");
        assertFalse(ocr.isAvailable());
        assertTrue(ocr.read(new byte[] {1, 2, 3}, Lang.EN).isEmpty());
    }

    @Test
    void textImportWorksWithoutOcr() {
        LocalRecipeImporter importer = new LocalRecipeImporter(new TesseractOcr("definitely-not-a-real-tesseract"));
        assertTrue(importer.isEnabled());
        assertTrue(importer.fromText("Soup\nIngredients\n2 carrots\n1 onion", Lang.EN, java.util.List.of())
                .isPresent());
        assertTrue(importer.fromImage(new byte[] {1}, "image/png", null, Lang.EN, java.util.List.of()).isEmpty());
    }
}
