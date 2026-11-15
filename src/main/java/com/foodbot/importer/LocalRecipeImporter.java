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
        Optional<ImportedRecipe> result = RecipeTextParser.parse(text, null, knownIngredients);
        log("text", text, result);
        return result;
    }

    @Override
    public Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                              List<String> knownIngredients) {
        Optional<String> text = ocr.read(image, lang);
        if (text.isEmpty()) {
            System.out.println("Recipe import (image, " + image.length + " bytes): OCR found no text"
                    + (ocr.isAvailable() ? "" : " - tesseract is not installed"));
            return Optional.empty();
        }
        Optional<ImportedRecipe> result = RecipeTextParser.parse(text.get(), caption, knownIngredients);
        log("image", text.get(), result);
        return result;
    }

    /** Sizes and counts only, never the recipe text itself. */
    private static void log(String source, String text, Optional<ImportedRecipe> result) {
        String outcome = result.map(r -> "ok: " + r.ingredients().size() + " ingredients, " + r.steps().size()
                + " steps, " + (r.name().isBlank() ? "no name" : "named")).orElse("no recipe found");
        System.out.println("Recipe import (" + source + ", " + text.length() + " chars, " + text.lines().count()
                + " lines): " + outcome);
    }
}
