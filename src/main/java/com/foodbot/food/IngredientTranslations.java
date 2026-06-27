package com.foodbot.food;

import com.foodbot.lang.Lang;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class IngredientTranslations {
    private static final Map<String, String> EN_TO_FA = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, String> FA_TO_EN = new HashMap<>();

    static {
        addPair("egg", "تخم مرغ");
        addPair("potato", "سیب زمینی");
        addPair("rice", "برنج");
        addPair("tuna", "ماهی تن");
        addPair("fish", "ماهی");
        addPair("beef", "گوشت");
        addPair("meat", "گوشت");
        addPair("chicken", "مرغ");
        addPair("onion", "پیاز");
        addPair("garlic", "سیر");
        addPair("tomato", "گوجه فرنگی");
        addPair("cheese", "پنیر");
        addPair("milk", "شیر");
        addPair("bread", "نان");
        addPair("sugar", "شکر");
        addPair("lemon", "لیمو");
        addPair("cucumber", "خیار");
        addPair("carrot", "هویج");
        addPair("lentil", "عدس");
        addPair("spinach", "اسفناج");
        addPair("apple", "سیب");
        addPair("banana", "موز");
        addPair("honey", "عسل");
        addPair("yogurt", "ماست");
        addPair("water", "آب");
        addPair("turmeric", "زردچوبه");
        addPair("saffron", "زعفران");
        addPair("parsley", "جعفری");
        addPair("leek", "تره");
        addPair("fenugreek", "شنبلیله");
        addPair("walnut", "گردو");
        addPair("pomegranate", "انار");
        addPair("mint", "نعنا");
        addPair("barberries", "زرشک");
        addPair("kidney beans", "لوبیا قرمز");
    }

    private static void addPair(String en, String fa) {
        EN_TO_FA.put(en, fa);
        FA_TO_EN.put(fa, en);
    }

    public static String translate(String ingredient, Lang targetLang) {
        String trimmed = ingredient.trim();
        if (targetLang == Lang.FA) {
            return EN_TO_FA.getOrDefault(trimmed, ingredient);
        }
        return FA_TO_EN.getOrDefault(trimmed, ingredient);
    }

    private IngredientTranslations() {
    }
}
