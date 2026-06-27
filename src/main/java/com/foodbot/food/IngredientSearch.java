package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class IngredientSearch {

    public static List<String> visibleCandidates(List<String> candidates, Set<String> selected, String filter, int max) {
        List<String> visible = new ArrayList<>();
        for (String name : candidates) {
            if (selected.contains(name)) {
                visible.add(name);
            }
        }
        String needle = (filter == null) ? "" : filter.toLowerCase();
        for (String name : candidates) {
            if (visible.size() >= max) {
                break;
            }
            if (selected.contains(name)) {
                continue;
            }
            if (needle.isEmpty() || name.toLowerCase().contains(needle)) {
                visible.add(name);
            }
        }
        return visible;
    }

    public static boolean hasPartialMatch(List<String> candidates, String text) {
        String needle = text.toLowerCase();
        return candidates.stream().anyMatch(i -> i.toLowerCase().contains(needle));
    }

    public static String findExactMatch(List<String> candidates, String text) {
        return candidates.stream().filter(i -> i.equalsIgnoreCase(text)).findFirst().orElse(null);
    }

    private IngredientSearch() {
    }
}
