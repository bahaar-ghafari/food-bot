package com.foodbot.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Messages {
    private static final Map<String, Map<Lang, String>> TABLE = new HashMap<>();

    static {
        put("welcome",
                "Welcome to FoodBot! Use the buttons below.",
                "به فودبات خوش آمدید! از دکمه‌های زیر استفاده کنید.");
        put("btn.add_food", "➕ Add food", "➕ افزودن غذا");
        put("btn.all_foods", "📋 All foods", "📋 همه غذاها");
        put("btn.all_ingredients", "🥗 All ingredients", "🥗 همه مواد اولیه");
        put("btn.what_can_cook", "🍳 What can I cook?", "🍳 چی می‌تونم بپزم؟");
        put("btn.change_lang", "🌐 Language", "🌐 زبان");
        put("btn.help", "❓ Help", "❓ راهنما");
        put("help.text",
                "🍽️ FoodBot helps you track recipes and decide what to cook.\n\n"
                        + "➕ Add food — add a recipe to your personal list or the shared global list: name, prep time, category, then ingredients (tap existing ones or type new ones).\n"
                        + "📋 All foods — view your list or the global list.\n"
                        + "🥗 All ingredients — every ingredient used so far.\n"
                        + "🍳 What can I cook? — tell it your time, what you have, and whether you can shop, and it suggests recipes.\n"
                        + "✏️ Edit / 🗑️ Delete — shown next to foods you added, or to the admin, on any food.\n"
                        + "🌐 Language — switch between English and Persian anytime.\n"
                        + "/whoami — shows your chat ID.\n"
                        + "/cancel — cancel whatever you're doing.\n"
                        + "/help — show this message.",
                "🍽️ فودبات به شما کمک می‌کند دستور پخت‌ها را پیگیری کنید و تصمیم بگیرید چه چیزی بپزید.\n\n"
                        + "➕ افزودن غذا — یک غذا به لیست شخصی یا لیست عمومی اضافه کنید: اسم، زمان آماده‌سازی، دسته‌بندی و سپس مواد اولیه (روی موادِ موجود بزنید یا مورد جدید تایپ کنید).\n"
                        + "📋 همه غذاها — لیست خودتان یا لیست عمومی را ببینید.\n"
                        + "🥗 همه مواد اولیه — هر ماده اولیه‌ای که تا الان استفاده شده.\n"
                        + "🍳 چی می‌تونم بپزم؟ — زمان، مواد اولیه‌ای که دارید و اینکه می‌توانید خرید کنید یا نه را بگویید تا پیشنهاد بگیرید.\n"
                        + "✏️ ویرایش / 🗑️ حذف — کنار غذاهایی که خودتان اضافه کرده‌اید یا برای ادمین، روی هر غذا نشان داده می‌شود.\n"
                        + "🌐 زبان — هر وقت خواستید بین انگلیسی و فارسی تغییر دهید.\n"
                        + "/whoami — شناسه چت شما را نشان می‌دهد.\n"
                        + "/cancel — هر کاری که در حال انجام است را لغو می‌کند.\n"
                        + "/help — نشان دادن همین پیام.");
        put("whoami.reply", "Your chat ID is: %s", "شناسه چت شما: %s");
        put("permission.denied",
                "Only the person who added this, or the admin, can do that.",
                "فقط کسی که این را اضافه کرده یا ادمین می‌تواند این کار را انجام دهد.");

        put("addfood.ask_scope",
                "Add this to your personal list or the global list?",
                "این رو به لیست شخصی یا لیست عمومی اضافه کنم؟");
        put("scope.mine", "👤 My list", "👤 لیست من");
        put("scope.global", "🌍 Global list", "🌍 لیست عمومی");
        put("addfood.ask_name",
                "Let's add a new food. What's its name?",
                "بیایید یک غذای جدید اضافه کنیم. اسمش چیه؟");
        put("addfood.ask_time",
                "Got it: %s. How many minutes does it take to prepare?",
                "باشه: %s. چند دقیقه طول می‌کشه تا آماده شه؟");
        put("addfood.invalid_time",
                "That doesn't look like a number. How many minutes does it take to prepare?",
                "این عدد نیست. چند دقیقه طول می‌کشه تا آماده شه؟");
        put("addfood.ask_category",
                "What category is this food?",
                "این غذا چه دسته‌بندی‌ای داره؟");
        put("addfood.tap_category",
                "Please tap one of the category buttons above.",
                "لطفاً یکی از دکمه‌های دسته‌بندی بالا را بزنید.");
        put("addfood.ingredient_prompt",
                "Tap ingredients to add or remove them, or type a new ingredient name. Tap Done when you're finished.",
                "روی مواد اولیه بزنید تا اضافه یا حذف شوند، یا اسم یک ماده جدید را تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("addfood.select_at_least_one",
                "Select at least one ingredient first.",
                "اول حداقل یک ماده اولیه را انتخاب کنید.");
        put("addfood.saved_toast", "Saved!", "ذخیره شد!");
        put("addfood.saved_message", "Saved: %s", "ذخیره شد: %s");

        put("selection_expired", "This selection has expired.", "این انتخاب منقضی شده است.");
        put("tap_button_above", "Please tap one of the buttons above.", "لطفاً یکی از دکمه‌های بالا را بزنید.");

        put("cook.ask_time",
                "How many minutes do you have to cook?",
                "چند دقیقه وقت دارید برای پختن؟");
        put("cook.invalid_time",
                "That doesn't look like a number. How many minutes do you have?",
                "این عدد نیست. چند دقیقه وقت دارید؟");
        put("cook.ingredient_prompt",
                "What ingredients do you have right now? Tap to select, or type a new one. Tap Done when you're finished.",
                "الان چه مواد اولیه‌ای دارید؟ برای انتخاب لمس کنید یا یک ماده جدید تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("cook.ask_shopping",
                "Can you go shopping for missing ingredients?",
                "می‌توانید برای مواد کم‌شده برید خرید؟");
        put("yes", "✅ Yes", "✅ بله");
        put("no", "🚫 No", "🚫 نه");
        put("cook.ask_category_filter",
                "Filter by category, or pick Any.",
                "بر اساس دسته‌بندی فیلتر کنید، یا «همه» را انتخاب کنید.");
        put("category.any", "Any", "همه");
        put("cook.nothing_matches",
                "Nothing matches right now. Try more time, different ingredients, or add more foods with /addfood.",
                "الان چیزی پیدا نشد. زمان بیشتر، مواد اولیه دیگر، یا با /addfood غذای بیشتری اضافه کنید.");
        put("cook.ready_header", "✅ You can cook right now:", "✅ همین الان می‌توانید این‌ها را بپزید:");
        put("cook.shopping_header",
                "🛒 You could cook these if you grab a few things:",
                "🛒 اگر چند چیز بخرید می‌توانید این‌ها را هم بپزید:");
        put("cook.missing_label", "missing", "کم دارید");

        put("removed", "Removed %s", "%s حذف شد");
        put("added", "Added %s", "%s اضافه شد");
        put("cancelled", "Cancelled.", "لغو شد.");
        put("fallback",
                "I didn't get that. Use the buttons below or /addfood.",
                "متوجه نشدم. از دکمه‌های پایین یا /addfood استفاده کنید.");

        put("foods.ask_scope",
                "Which list do you want to see?",
                "کدوم لیست رو می‌خواید ببینید؟");
        put("foods.none",
                "No foods saved yet. Use /addfood to add one.",
                "هنوز غذایی ذخیره نشده. با /addfood یکی اضافه کنید.");
        put("foods.header.mine", "👤 Your foods:", "👤 غذاهای شما:");
        put("foods.header.global", "🌍 Global foods:", "🌍 غذاهای عمومی:");
        put("ingredients.none", "No ingredients saved yet.", "هنوز مواد اولیه‌ای ذخیره نشده.");
        put("ingredients.header", "Ingredients:", "مواد اولیه:");
        put("min_unit", "min", "دقیقه");
        put("done_button", "✅ Done (%d selected)", "✅ تمام (%d انتخاب شده)");

        put("lang.prompt",
                "Please choose your language / لطفاً زبان خود را انتخاب کنید:",
                "Please choose your language / لطفاً زبان خود را انتخاب کنید:");
        put("lang.confirmed", "Language set to English.", "زبان به فارسی تغییر یافت.");
        put("lang.wrong_script", "Please type in English.", "لطفاً به فارسی تایپ کنید.");

        put("edit.choose_field", "What do you want to edit?", "چی را می‌خواهید ویرایش کنید؟");
        put("edit.field.name", "Name", "اسم");
        put("edit.field.time", "Prep time", "زمان آماده‌سازی");
        put("edit.field.category", "Category", "دسته‌بندی");
        put("edit.field.ingredients", "Ingredients", "مواد اولیه");
        put("edit.ask_name", "What's the new name?", "اسم جدید چیست؟");
        put("edit.ask_time", "What's the new prep time in minutes?", "زمان آماده‌سازی جدید چند دقیقه است؟");
        put("edit.ask_category", "Pick the new category.", "دسته‌بندی جدید را انتخاب کنید.");
        put("edit.ingredient_prompt",
                "Tap to add or remove ingredients, or type a new one. Tap Done when you're finished.",
                "برای اضافه یا حذف کردن مواد اولیه لمس کنید، یا یک ماده جدید تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("edit.saved", "Updated: %s", "به‌روزرسانی شد: %s");

        put("delete.confirm", "Delete %s? This can't be undone.", "%s حذف شود؟ این کار قابل بازگشت نیست.");
        put("delete.confirm_yes", "🗑️ Yes, delete", "🗑️ بله، حذف کن");
        put("delete.confirm_no", "🚫 Cancel", "🚫 لغو");
        put("delete.done", "Deleted: %s", "حذف شد: %s");
        put("delete.cancelled", "Delete cancelled.", "حذف لغو شد.");

        put("category.Breakfast", "Breakfast", "صبحانه");
        put("category.MainCourse", "Main Course", "غذای اصلی");
        put("category.Snack", "Snack", "میان‌وعده");
        put("category.Dessert", "Dessert", "دسر");
        put("category.Drink", "Drink", "نوشیدنی");
        put("category.Other", "Other", "غیره");
    }

    private static void put(String key, String en, String fa) {
        Map<Lang, String> map = new HashMap<>();
        map.put(Lang.EN, en);
        map.put(Lang.FA, fa);
        TABLE.put(key, map);
    }

    public static String get(Lang lang, String key, Object... args) {
        Map<Lang, String> entry = TABLE.get(key);
        String template = entry != null ? entry.get(lang) : null;
        if (template == null) {
            template = key;
        }
        return args.length == 0 ? template : String.format(template, args);
    }

    public static Set<String> keys() {
        return TABLE.keySet();
    }

    private Messages() {
    }
}
