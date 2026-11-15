package com.foodbot.importer;

import com.foodbot.lang.Lang;

import java.util.List;
import java.util.Optional;

/** Free recipe import: Tesseract OCR for screenshots, then {@link RecipeTextParser} for the text. */
public class LocalRecipeImporter implements RecipeImporter {
    private final TesseractOcr ocr;

    public LocalRecipeImporter(TesseractOcr ocr) {
        this.ocr = ocr;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<ImportedRecipe> fromText(String text, Lang lang, List<String> knownIngredients) {
        return RecipeTextParser.parse(text, null, knownIngredients);
    }

    @Override
    public Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                              List<String> knownIngredients) {
        return ocr.read(image, lang).flatMap(text -> RecipeTextParser.parse(text, caption, knownIngredients));
    }
}
