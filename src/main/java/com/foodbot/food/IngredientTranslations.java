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
        addPair("noodle", "رشته");
        addPair("flour", "آرد");
        addPair("butter", "کره");
        addPair("salt", "نمک");
        addPair("basil", "ریحان");
        addPair("dill", "شیوید");
        addPair("rum", "رام");
        addPair("tequila", "تکیلا");
        addPair("pineapple", "آناناس");
        addPair("coconut milk", "شیر نارگیل");
        addPair("eggplant", "بادمجان");
        addPair("cabbage", "کلم");
        addPair("cinnamon", "دارچین");
        addPair("chickpeas", "نخود");
        addPair("tahini", "ارده");
        addPair("tamarind", "تمبر هندی");
        addPair("dried lime", "لیمو عمانی");
        addPair("celery", "کرفس");
        addPair("raisins", "کشمش");
        addPair("rosewater", "گلاب");
        addPair("vanilla", "وانیل");
        addPair("chocolate", "شکلات");
        addPair("cocoa powder", "پودر کاکائو");
        addPair("salmon", "ماهی سالمون");
        addPair("dried plums", "آلو");
        addPair("dried apricot", "قیسی");
        addPair("split peas", "لپه");
        addPair("grape leaves", "برگ مو");
        addPair("oats", "جو");
        addPair("tortilla", "تورتیلا");
        addPair("coffee", "قهوه");
        addPair("oil", "روغن");
        addPair("pepper", "فلفل");
        addPair("sunflower seeds", "تخمه");
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
