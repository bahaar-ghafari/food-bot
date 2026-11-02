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
addPair("باقالی پلو با مرغ", "Baghali Polo ba Morgh");
        addPair("سبزی پلو با ماهی", "Sabzi Polo ba Mahi");
        addPair("عدس پلو", "Adas Polo");
        addPair("لوبیا پلو", "Loobia Polo");
        addPair("شیرین پلو", "Shirin Polo");
        addPair("چلو کباب", "Chelo Kabab");
        addPair("کباب برگ", "Kabab Barg");
        addPair("خورشت کرفس", "Khoresht Karafs");
        addPair("خورشت بادمجان", "Khoresht Bademjan");
        addPair("دلمه برگ مو", "Dolmeh Barg-e Mo");
        addPair("دلمه فلفل", "Dolmeh Felfel");
        addPair("کوفته تبریزی", "Kofteh Tabrizi");
        addPair("کتلت", "Kotlet");
        addPair("شامی", "Shami");
        addPair("آبگوشت", "Abgoosht");
        addPair("کشک بادمجان", "Kashk Bademjan");
        addPair("ماست و خیار", "Mast-o-Khiar");
        addPair("سالاد شیرازی", "Salad Shirazi");
        addPair("سالاد الویه", "Salad Olivieh");
        addPair("کوکو سیب‌زمینی", "Kuku Sibzamini");
        addPair("بورانی اسفناج", "Borani Esfenaj");
        addPair("نرگسی اسفناج", "Nargesi Esfenaj");
        addPair("آش انار", "Ash Anar");
        addPair("سوپ جو", "Soup Jo");
        addPair("شله زرد", "Sholeh Zard");
        addPair("شیر برنج", "Shir Berenj");
        addPair("حلوا", "Halva");
        addPair("حلوا ارده", "Halva Ardeh");
        addPair("زولبیا بامیه", "Zoolbia Bamieh");
        addPair("شربت سکنجبین", "Sekanjabin");
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
