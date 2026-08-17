package com.foodbot.food;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ranks /cook matches so that dishes making the best use of what the user already has
 * come first — not just dishes that happen to need the fewest extra ingredients.
 *
 * Primary key: how many ingredients are still missing (fewer is better — that's what
 * actually drives whether/how much shopping is needed). Secondary key, used to break
 * ties: how many of the dish's ingredients the user already has (more is better), so a
 * dish that reuses several things the user has on hand (e.g. chicken, when they've told
 * the bot they have chicken) outranks an unrelated dish that merely happens to be missing
 * the same small number of ingredients.
 */
public final class CookRanking {

    public static void sortByBestMatch(List<Food> foods, Set<String> haveIngredients) {
        foods.sort(bestMatchComparator(haveIngredients));
    }

    public static Comparator<Food> bestMatchComparator(Set<String> haveIngredients) {
        return Comparator
                .<Food>comparingInt(food -> missingCount(food, haveIngredients))
                .thenComparing(Comparator.<Food>comparingInt(food -> matchedCount(food, haveIngredients)).reversed());
    }

    /**
     * Whether the dish shares at least one non-staple ingredient with what the user already
     * has. Used to keep /cook's "could cook if you shop" suggestions grounded in the user's
     * own kitchen: a dish that would need every non-staple ingredient bought from scratch
     * (e.g. a fish dish suggested to someone who only has chicken) isn't "cook what you have
     * and buy the shortfall" - it's just a different recipe, so it's filtered out rather than
     * ranked low.
     */
    public static boolean buildsOnWhatUserHas(Food food, Set<String> haveIngredients) {
        return matchedCount(food, haveIngredients) > 0;
    }

    public static int missingCount(Food food, Set<String> haveIngredients) {
        return (int) food.getIngredients().stream()
                .filter(ing -> !effectivelyHas(haveIngredients, ing))
                .count();
    }

    /**
     * How many of the dish's (non-staple) ingredients the user already told the bot they have.
     * Staples aren't counted — they're assumed available to everyone, so they shouldn't make a
     * dish look like a better use of the user's specific pantry than it is.
     */
    public static int matchedCount(Food food, Set<String> haveIngredients) {
        return (int) food.getIngredients().stream()
                .filter(ing -> !PantryStaples.isStaple(ing) && effectivelyHas(haveIngredients, ing))
                .count();
    }

    public static boolean effectivelyHas(Set<String> haveIngredients, String ingredient) {
        return PantryStaples.isStaple(ingredient)
                || haveIngredients.stream().anyMatch(h -> h.equalsIgnoreCase(ingredient));
    }

    private CookRanking() {
    }
}
