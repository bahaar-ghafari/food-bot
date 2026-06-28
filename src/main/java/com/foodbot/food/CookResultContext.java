package com.foodbot.food;

import java.util.LinkedHashSet;
import java.util.Set;

public class CookResultContext {
    private final int timeMinutes;
    private final Set<String> haveIngredients;
    private final boolean canShop;
    private final String category;

    public CookResultContext(int timeMinutes, Set<String> haveIngredients, boolean canShop, String category) {
        this.timeMinutes = timeMinutes;
        this.haveIngredients = new LinkedHashSet<>(haveIngredients);
        this.canShop = canShop;
        this.category = category;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public Set<String> getHaveIngredients() {
        return haveIngredients;
    }

    public boolean isCanShop() {
        return canShop;
    }

    public String getCategory() {
        return category;
    }
}
