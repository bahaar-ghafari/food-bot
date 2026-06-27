package com.foodbot.food;

import com.foodbot.lang.Lang;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class FoodNameTranslations {
    private static final Map<String, String> FA_TO_EN = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Map<String, String> EN_TO_FA = new HashMap<>();

    static {
        addPair("قورمه سبزی", "Ghormeh Sabzi");
        addPair("فسنجان", "Fesenjan");
        addPair("تهچین", "Tahchin");
        addPair("جوجه کباب", "Joojeh Kabab");
        addPair("آش رشته", "Ash Reshteh");
        addPair("کوکو سبزی", "Kuku Sabzi");
        addPair("زرشک پلو با مرغ", "Zereshk Polo ba Morgh");
        addPair("دوغ", "Doogh");
        addPair("فالوده", "Faloodeh");
    }

    private static void addPair(String fa, String en) {
        FA_TO_EN.put(fa, en);
        EN_TO_FA.put(en.toLowerCase(), fa);
    }

    public static String translate(String name, Lang targetLang) {
        String trimmed = name.trim();
        if (targetLang == Lang.EN) {
            return FA_TO_EN.getOrDefault(trimmed, name);
        }
        return EN_TO_FA.getOrDefault(trimmed.toLowerCase(), name);
    }

    private FoodNameTranslations() {
    }
}
