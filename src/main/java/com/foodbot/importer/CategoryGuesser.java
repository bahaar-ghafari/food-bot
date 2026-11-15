package com.foodbot.importer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Guesses a food category from keywords in the dish name (then the start of the text); "Other" if unsure. */
final class CategoryGuesser {
    // Checked in this order, so "chocolate pancakes" is Breakfast and "chicken soup" is MainCourse.
    private static final Map<String, List<String>> KEYWORDS = new LinkedHashMap<>();

    static {
        KEYWORDS.put("Drink", List.of("smoothie", "juice", "lemonade", "latte", "coffee", "tea", "milkshake",
                "shake", "cocktail", "mocktail", "drink", "cocoa", "chai", "شربت", "چای", "قهوه", "آبمیوه", "دوغ",
                "اسموتی", "نوشیدنی", "شیک", "لیموناد", "موهیتو", "کاپوچینو", "لاته"));
        KEYWORDS.put("Breakfast", List.of("pancake", "pancakes", "omelette", "omelet", "waffle", "waffles",
                "granola", "oatmeal", "porridge", "french toast", "breakfast", "frittata", "shakshuka", "muesli",
                "scrambled", "صبحانه", "املت", "نیمرو", "کاچی", "عدسی", "حلیم", "پنکیک", "شکشوکا", "فرنی", "کره بادام زمینی"));
        KEYWORDS.put("Dessert", List.of("cake", "cupcake", "cookie", "cookies", "brownie", "brownies", "pie", "tart",
                "pudding", "ice cream", "dessert", "cheesecake", "mousse", "tiramisu", "muffin", "muffins",
                "doughnut", "donut", "fudge", "custard", "crumble", "biscuit", "candy", "کیک", "شیرینی", "دسر",
                "حلوا", "شله زرد", "بستنی", "کلوچه", "کوکی", "پودینگ", "ژله", "باقلوا", "زولبیا", "بامیه", "رنگینک",
                "مافین", "تارت", "شیر برنج", "سوهان"));
        KEYWORDS.put("MainCourse", List.of("stew", "curry", "pasta", "spaghetti", "lasagna", "lasagne", "risotto",
                "chicken", "beef", "steak", "pork", "lamb", "salmon", "fish", "shrimp", "kebab", "kabob", "burger",
                "pizza", "soup", "chili", "casserole", "roast", "biryani", "pilaf", "noodles", "tacos", "burrito",
                "meatballs", "خورش", "خورشت", "پلو", "کباب", "ماکارونی", "قورمه", "قیمه", "فسنجان", "کوفته", "دلمه",
                "آبگوشت", "چلو", "کوکو", "کتلت", "سوپ", "آش", "استانبولی", "زرشک پلو", "باقالی پلو", "جوجه", "میرزا قاسمی",
                "کشک بادمجان", "ته چین", "تهچین", "لازانیا", "پیتزا", "همبرگر", "ماهی", "مرغ", "گوشت", "عدس پلو"));
        KEYWORDS.put("Snack", List.of("snack", "dip", "hummus", "chips", "popcorn", "bites", "fries", "nachos",
                "salad", "sandwich", "wrap", "toast", "bruschetta", "اسنک", "میان وعده", "پفک", "ساندویچ", "سالاد",
                "الویه", "اولویه", "فلافل", "سمبوسه", "بورانی", "ماست و خیار", "پیراشکی", "رول"));
    }

    private CategoryGuesser() {
    }

    static String guess(String name, String text) {
        String fromName = match(name);
        if (fromName != null) {
            return fromName;
        }
        String start = text.length() > 200 ? text.substring(0, 200) : text;
        String fromText = match(start);
        return fromText != null ? fromText : "Other";
    }

    private static String match(String text) {
        String t = " " + text.toLowerCase(Locale.ROOT).replace('‌', ' ').replaceAll("[^\\p{L}\\s]", " ")
                .replaceAll("\\s+", " ") + " ";
        for (Map.Entry<String, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (Pattern.compile("(?U)(?<![\\p{L}])" + Pattern.quote(keyword) + "(?:s|es)?(?![\\p{L}])")
                        .matcher(t).find()) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
