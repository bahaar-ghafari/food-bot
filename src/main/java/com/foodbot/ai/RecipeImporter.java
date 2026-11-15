package com.foodbot.ai;

import com.foodbot.lang.Lang;

import java.util.List;
import java.util.Optional;

/** Turns a pasted recipe or a screenshot of one into a structured draft the user only has to confirm. */
public interface RecipeImporter {
    boolean isEnabled();

    /** Empty when the text isn't a recipe, or the import failed for any reason. */
    Optional<ImportedRecipe> fromText(String text, Lang lang, List<String> knownIngredients);

    /** {@code caption} may be null. Empty when the image isn't a recipe, or the import failed for any reason. */
    Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                       List<String> knownIngredients);

    RecipeImporter DISABLED = new RecipeImporter() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public Optional<ImportedRecipe> fromText(String text, Lang lang, List<String> knownIngredients) {
            return Optional.empty();
        }

        @Override
        public Optional<ImportedRecipe> fromImage(byte[] image, String mediaType, String caption, Lang lang,
                                                  List<String> knownIngredients) {
            return Optional.empty();
        }
    };
}
