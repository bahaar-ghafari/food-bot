package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class IngredientSearch {

    /**
     * Full ordered candidate list for the picker: selected ingredients first (so they can always be
     * toggled off), then non-selected ingredients matching the filter. No size limit - callers paginate.
     */
    public static List<String> orderedCandidates(List<String> candidates, Set<String> selected, String filter) {
        List<String> ordered = new ArrayList<>();
        for (String name : candidates) {
            if (selected.contains(name)) {
                ordered.add(name);
            }
        }
        String needle = (filter == null) ? "" : filter.toLowerCase();
        for (String name : candidates) {
            if (selected.contains(name)) {
                continue;
            }
            if (needle.isEmpty() || name.toLowerCase().contains(needle)) {
                ordered.add(name);
            }
        }
        return ordered;
    }

    public static List<String> visibleCandidates(List<String> candidates, Set<String> selected, String filter, int max) {
        List<String> ordered = orderedCandidates(candidates, selected, filter);
        return ordered.size() <= max ? ordered : new ArrayList<>(ordered.subList(0, max));
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
