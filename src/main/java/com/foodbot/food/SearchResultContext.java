package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;

public class SearchResultContext {
    private final List<Food> foods;
    private final String query;

    public SearchResultContext(List<Food> foods, String query) {
        this.foods = new ArrayList<>(foods);
        this.query = query;
    }

    public List<Food> getFoods() {
        return foods;
    }

    public String getQuery() {
        return query;
    }
}
