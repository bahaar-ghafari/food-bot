package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;

public final class RecipeSteps {

    public static List<String> parse(String recipeText) {
        List<String> steps = new ArrayList<>();
        if (recipeText == null || recipeText.isBlank()) {
            return steps;
        }
        for (String line : recipeText.split("\n")) {
            steps.add(line.replaceFirst("^\\d+\\.\\s*", ""));
        }
        return steps;
    }

    public static void moveUp(List<String> steps, int index) {
        if (index <= 0 || index >= steps.size()) {
            return;
        }
        swap(steps, index, index - 1);
    }

    public static void moveDown(List<String> steps, int index) {
        if (index < 0 || index >= steps.size() - 1) {
            return;
        }
        swap(steps, index, index + 1);
    }

    public static Integer editingIndexAfterMoveUp(int target, Integer editing) {
        if (editing == null) {
            return null;
        }
        if (editing == target) {
            return target - 1;
        }
        if (editing == target - 1) {
            return target;
        }
        return editing;
    }

    public static Integer editingIndexAfterMoveDown(int target, Integer editing) {
        if (editing == null) {
            return null;
        }
        if (editing == target) {
            return target + 1;
        }
        if (editing == target + 1) {
            return target;
        }
        return editing;
    }

    public static Integer editingIndexAfterDelete(int target, Integer editing) {
        if (editing == null) {
            return null;
        }
        if (editing == target) {
            return null;
        }
        if (editing > target) {
            return editing - 1;
        }
        return editing;
    }

    public static String join(List<String> steps) {
        if (steps.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(i + 1).append(". ").append(steps.get(i));
        }
        return builder.toString();
    }

    private static void swap(List<String> steps, int i, int j) {
        String temp = steps.get(i);
        steps.set(i, steps.get(j));
        steps.set(j, temp);
    }

    private RecipeSteps() {
    }
}
