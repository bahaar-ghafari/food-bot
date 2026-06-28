package com.foodbot.food;

import java.util.Map;
import java.util.TreeMap;

public final class IngredientIcons {
    private static final String DEFAULT_ICON = "🧂";
    private static final Map<String, String> ICONS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        ICONS.put("egg", "🥚");
        ICONS.put("eggs", "🥚");
        ICONS.put("تخم مرغ", "🥚");
        ICONS.put("potato", "🥔");
        ICONS.put("potatoes", "🥔");
        ICONS.put("سیب زمینی", "🥔");
        ICONS.put("rice", "🍚");
        ICONS.put("برنج", "🍚");
        ICONS.put("tuna", "🐟");
        ICONS.put("fish", "🐟");
        ICONS.put("ماهی تن", "🐟");
        ICONS.put("تن ماهی", "🐟");
        ICONS.put("ماهی", "🐟");
        ICONS.put("beef", "🥩");
        ICONS.put("meat", "🥩");
        ICONS.put("گوشت", "🥩");
        ICONS.put("chicken", "🍗");
        ICONS.put("مرغ", "🍗");
        ICONS.put("onion", "🧅");
        ICONS.put("پیاز", "🧅");
        ICONS.put("garlic", "🧄");
        ICONS.put("سیر", "🧄");
        ICONS.put("tomato", "🍅");
        ICONS.put("tomatoes", "🍅");
        ICONS.put("گوجه فرنگی", "🍅");
        ICONS.put("گوجه", "🍅");
        ICONS.put("cheese", "🧀");
        ICONS.put("پنیر", "🧀");
        ICONS.put("milk", "🥛");
        ICONS.put("شیر", "🥛");
        ICONS.put("bread", "🍞");
        ICONS.put("نان", "🍞");
        ICONS.put("shrimp", "🦐");
        ICONS.put("pepper", "🌶️");
        ICONS.put("salt", "🧂");
        ICONS.put("sugar", "🍬");
        ICONS.put("شکر", "🍬");
        ICONS.put("butter", "🧈");
        ICONS.put("oil", "🫒");
        ICONS.put("lemon", "🍋");
        ICONS.put("لیمو", "🍋");
        ICONS.put("lettuce", "🥬");
        ICONS.put("cucumber", "🥒");
        ICONS.put("خیار", "🥒");
        ICONS.put("carrot", "🥕");
        ICONS.put("هویج", "🥕");
        ICONS.put("mushroom", "🍄");
        ICONS.put("pasta", "🍝");
        ICONS.put("noodle", "🍜");
        ICONS.put("noodles", "🍜");
        ICONS.put("beans", "🫘");
        ICONS.put("lentil", "🫘");
        ICONS.put("lentils", "🫘");
        ICONS.put("عدس", "🫘");
        ICONS.put("spinach", "🥬");
        ICONS.put("اسفناج", "🥬");
        ICONS.put("apple", "🍎");
        ICONS.put("سیب", "🍎");
        ICONS.put("banana", "🍌");
        ICONS.put("موز", "🍌");
        ICONS.put("corn", "🌽");
        ICONS.put("avocado", "🥑");
        ICONS.put("bacon", "🥓");
        ICONS.put("nuts", "🥜");
        ICONS.put("peanut", "🥜");
        ICONS.put("honey", "🍯");
        ICONS.put("عسل", "🍯");
        ICONS.put("yogurt", "🥣");
        ICONS.put("ماست", "🥣");
        ICONS.put("flour", "🌾");
        ICONS.put("water", "💧");
        ICONS.put("آب", "💧");
        ICONS.put("parsley", "🌿");
        ICONS.put("جعفری", "🌿");
        ICONS.put("leek", "🌿");
        ICONS.put("تره", "🌿");
        ICONS.put("fenugreek", "🌿");
        ICONS.put("شنبلیله", "🌿");
        ICONS.put("mint", "🌿");
        ICONS.put("نعنا", "🌿");
        ICONS.put("walnut", "🌰");
        ICONS.put("گردو", "🌰");
        ICONS.put("pomegranate", "🔴");
        ICONS.put("انار", "🔴");
        ICONS.put("barberries", "🔴");
        ICONS.put("زرشک", "🔴");
        ICONS.put("kidney beans", "🫘");
        ICONS.put("لوبیا قرمز", "🫘");
        ICONS.put("رشته", "🍜");
        ICONS.put("آرد", "🌾");
        ICONS.put("کره", "🧈");
        ICONS.put("نمک", "🧂");
        ICONS.put("dill", "🌿");
        ICONS.put("شیوید", "🌿");
        ICONS.put("basil", "🌿");
        ICONS.put("ریحان", "🌿");
    }

    public static String iconFor(String ingredient) {
        return ICONS.getOrDefault(ingredient.trim(), DEFAULT_ICON);
    }

    private IngredientIcons() {
    }
}
