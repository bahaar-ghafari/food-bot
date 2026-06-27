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
        put("btn.what_can_cook", "🍳 What can I cook?", "🍳 چی می‌تونم بپزم؟");
        put("btn.change_lang", "🌐 Language", "🌐 زبان");
        put("btn.help", "❓ Help", "❓ راهنما");
        put("btn.settings", "⚙️ Settings", "⚙️ تنظیمات");
        put("btn.edit", "✏️ Edit", "✏️ ویرایش");
        put("btn.delete", "🗑️ Delete", "🗑️ حذف");
        put("btn.skip", "⏭ Skip", "⏭ رد کردن");
        put("btn.clear_recipe", "🗑️ Clear recipe", "🗑️ حذف دستور پخت");
        put("help.text",
                "🍽️ FoodBot helps you track recipes and decide what to cook.\n\n"
                        + "➕ Add food — add a recipe to your personal list or the shared global list: name, prep time, category, ingredients (tap existing ones, type a few letters to search a long list, or type a full new name to add it), then an optional recipe.\n"
                        + "📋 All foods — pick your list or the global list, then tap a food to see its category, prep time, ingredients, and recipe.\n"
                        + "⚙️ Settings — shown on a food's detail view if you added it or you're the admin; tap it for ✏️ Edit / 🗑️ Delete.\n"
                        + "🍳 What can I cook? — tell it your time, what you have, and whether you can shop, and it suggests recipes.\n"
                        + "🌐 Language — switch between English and Persian anytime.\n"
                        + "/whoami — shows your chat ID.\n"
                        + "/cancel — cancel whatever you're doing.\n"
                        + "/help — show this message.",
                "🍽️ فودبات به شما کمک می‌کند دستور پخت‌ها را پیگیری کنید و تصمیم بگیرید چه چیزی بپزید.\n\n"
                        + "➕ افزودن غذا — یک غذا به لیست شخصی یا لیست عمومی اضافه کنید: اسم، زمان آماده‌سازی، دسته‌بندی، مواد اولیه (روی موادِ موجود بزنید، برای جستجو در لیست بلند چند حرف تایپ کنید، یا اسم کامل یک ماده جدید را تایپ کنید تا اضافه شود)، و در آخر یک دستور پخت اختیاری.\n"
                        + "📋 همه غذاها — لیست خودتان یا لیست عمومی را انتخاب کنید، سپس روی یک غذا بزنید تا دسته‌بندی، زمان آماده‌سازی، مواد اولیه و دستور پختش را ببینید.\n"
                        + "⚙️ تنظیمات — در صفحه جزئیات غذایی که خودتان اضافه کرده‌اید یا برای ادمین نشان داده می‌شود؛ با زدنش گزینه‌های ✏️ ویرایش / 🗑️ حذف می‌آید.\n"
                        + "🍳 چی می‌تونم بپزم؟ — زمان، مواد اولیه‌ای که دارید و اینکه می‌توانید خرید کنید یا نه را بگویید تا پیشنهاد بگیرید.\n"
                        + "🌐 زبان — هر وقت خواستید بین انگلیسی و فارسی تغییر دهید.\n"
                        + "/whoami — شناسه چت شما را نشان می‌دهد.\n"
                        + "/cancel — هر کاری که در حال انجام است را لغو می‌کند.\n"
                        + "/help — نشان دادن همین پیام.");
        put("whoami.reply", "Your chat ID is: %s\nTelegram username: %s", "شناسه چت شما: %s\nنام کاربری تلگرام: %s");
        put("whoami.no_username", "(not set)", "(تنظیم نشده)");
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
                "Tap ingredients to add or remove them, or type to search the list or add a new one. Tap Done when you're finished.",
                "روی مواد اولیه بزنید تا اضافه یا حذف شوند، یا برای جستجو یا افزودن ماده جدید تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("addfood.select_at_least_one",
                "Select at least one ingredient first.",
                "اول حداقل یک ماده اولیه را انتخاب کنید.");
        put("addfood.ask_recipe",
                "What's the recipe (cooking steps)? Tap Skip to leave it blank.",
                "دستور پخت (مراحل پختن) چیست؟ برای خالی گذاشتن روی «رد کردن» بزنید.");
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
                "What ingredients do you have right now? Tap to select, or type to search the list or add a new one. Tap Done when you're finished.",
                "الان چه مواد اولیه‌ای دارید؟ برای انتخاب لمس کنید یا برای جستجو یا افزودن ماده جدید تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
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
        put("foods.header.mine", "👤 Your foods, tap one for details:", "👤 غذاهای شما، برای جزئیات روی یکی بزنید:");
        put("foods.header.global", "🌍 Global foods, tap one for details:", "🌍 غذاهای عمومی، برای جزئیات روی یکی بزنید:");
        put("settings.choose_action", "What do you want to do?", "چه کاری می‌خواهید انجام دهید؟");
        put("food.detail_category", "Category: %s", "دسته‌بندی: %s");
        put("food.detail_time", "Prep time: %s", "زمان آماده‌سازی: %s");
        put("food.detail_ingredients", "Ingredients: %s", "مواد اولیه: %s");
        put("food.detail_recipe", "Recipe: %s", "دستور پخت: %s");
        put("food.no_recipe", "(no recipe added yet)", "(هنوز دستور پختی اضافه نشده)");
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
        put("edit.field.recipe", "Recipe", "دستور پخت");
        put("edit.ask_name", "What's the new name?", "اسم جدید چیست؟");
        put("edit.ask_time", "What's the new prep time in minutes?", "زمان آماده‌سازی جدید چند دقیقه است؟");
        put("edit.ask_category", "Pick the new category.", "دسته‌بندی جدید را انتخاب کنید.");
        put("edit.ingredient_prompt",
                "Tap to add or remove ingredients, or type to search the list or add a new one. Tap Done when you're finished.",
                "برای اضافه یا حذف کردن مواد اولیه لمس کنید، یا برای جستجو یا افزودن ماده جدید تایپ کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("edit.ask_recipe",
                "What's the new recipe? Tap Clear to remove it instead.",
                "دستور پخت جدید چیست؟ برای حذف آن روی «حذف دستور پخت» بزنید.");
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
