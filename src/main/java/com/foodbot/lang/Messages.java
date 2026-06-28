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
        put("btn.back", "🔙 Back", "🔙 بازگشت");
        put("btn.edit", "✏️ Edit", "✏️ ویرایش");
        put("btn.delete", "🗑️ Delete", "🗑️ حذف");
        put("btn.done_recipe", "✅ Done", "✅ تمام");
        put("btn.clear_recipe", "🗑️ Clear recipe", "🗑️ حذف دستور پخت");
        put("help.text",
                "🍽️ FoodBot helps you track recipes and decide what to cook.\n\n"
                        + "➕ Add food — add a recipe to your personal list or the shared global list: name, prep time, category, ingredients (tap existing ones, type a few letters to search a long list, or type a full new name to add it), then an optional recipe.\n\n"
                        + "🍳 What can I cook? — pick a time preset (or type one), say what you have and whether you can shop, then tap any suggested food to see what you have vs. need to buy, plus its recipe.\n\n"
                        + "🌐 Language — switch between English and Persian anytime.\n\n"
                        + "/cancel — cancel whatever you're doing.\n\n"
                        + "/help — show this message.",
                "🍽️ فودبات به شما کمک می‌کند دستور پخت‌ها را پیگیری کنید و تصمیم بگیرید چه چیزی بپزید.\n\n"
                        + "➕ افزودن غذا — یک غذا به لیست شخصی یا لیست عمومی اضافه کنید: اسم، زمان آماده‌سازی، دسته‌بندی، مواد اولیه (روی موادِ موجود بزنید، برای جستجو در لیست بلند چند حرف تایپ کنید، یا اسم کامل یک ماده جدید را تایپ کنید تا اضافه شود)، و در آخر یک دستور پخت اختیاری.\n\n"
                        + "🍳 چی می‌تونم بپزم؟ — یک زمان آماده انتخاب کنید (یا تایپ کنید)، بگویید چی دارید و می‌توانید خرید کنید یا نه، سپس روی هر غذای پیشنهادی بزنید تا ببینید چی دارید و چی باید بخرید، به همراه دستور پختش.\n\n"
                        + "🌐 زبان — هر وقت خواستید بین انگلیسی و فارسی تغییر دهید.\n\n"
                        + "/cancel — هر کاری که در حال انجام است را لغو می‌کند.\n\n"
                        + "/help — نشان دادن همین پیام.");
        put("help.text.admin",
                "📋 All foods — pick your list or the global list, then tap a food to see its category, prep time, ingredients, and recipe. (Admin only.)\n\n"
                        + "⚙️ Settings — shown only to you on any food's detail view; tap it for ✏️ Edit / 🗑️ Delete.",
                "📋 همه غذاها — لیست خودتان یا لیست عمومی را انتخاب کنید، سپس روی یک غذا بزنید تا دسته‌بندی، زمان آماده‌سازی، مواد اولیه و دستور پختش را ببینید. (فقط ادمین.)\n\n"
                        + "⚙️ تنظیمات — فقط برای شما، روی صفحه جزئیات هر غذایی نشان داده می‌شود؛ با زدنش گزینه‌های ✏️ ویرایش / 🗑️ حذف می‌آید.");
        put("permission.denied",
                "Only the admin can do that.",
                "فقط ادمین می‌تواند این کار را انجام دهد.");

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
        put("addfood.ask_recipe_step",
                "📝 Step %d — describe it in %d characters or less. Tap Done if there are no more steps.",
                "📝 مرحله %d — آن را در %d کاراکتر یا کمتر توضیح دهید. اگر مرحله دیگری نیست روی «تمام» بزنید.");
        put("recipe.step_too_long",
                "That's %d characters — please keep each step to %d characters or less.",
                "این %d کاراکتر است — لطفاً هر مرحله را در %d کاراکتر یا کمتر بنویسید.");
        put("addfood.saved_toast", "Saved!", "ذخیره شد!");
        put("addfood.saved_message", "🎉 Saved: %s", "🎉 ذخیره شد: %s");

        put("selection_expired", "This selection has expired.", "این انتخاب منقضی شده است.");
        put("tap_button_above", "Please tap one of the buttons above.", "لطفاً یکی از دکمه‌های بالا را بزنید.");

        put("cook.ask_time",
                "How much time do you have to cook? Tap an option below, or type an exact number of minutes.",
                "چقدر وقت دارید برای پختن؟ یکی از گزینه‌های زیر را بزنید یا تعداد دقیقه دقیق را تایپ کنید.");
        put("cook.time.fast", "⚡ Fast (~30m)", "⚡ سریع (~30 دقیقه)");
        put("cook.time.2h", "🕐 ~2 hours", "🕐 حدود 2 ساعت");
        put("cook.time.5h", "🕔 ~5 hours", "🕔 حدود 5 ساعت");
        put("cook.time.1day", "📅 ~1 day", "📅 حدود 1 روز");
        put("cook.time.any", "♾️ Any amount of time", "♾️ هر مقدار زمان");
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
                "Hmm, nothing on the menu fits that. 🤔 Try more time, different ingredients, or add more foods with /addfood.",
                "هوم، هیچی با این شرایط جور نشد. 🤔 زمان بیشتر، مواد اولیه دیگر، یا با /addfood غذای بیشتری اضافه کنید.");
        put("cook.nothing_matches_no_shop",
                "Nothing fits, and shopping's off the table... looks like a fresh-air dinner tonight! 🌬️🍽️ Try more time, different ingredients, or add a food with /addfood.",
                "هیچی جور نشد و رفتن خرید هم گزینه نیست... امشب باید با هوای تازه سیر شوید! 🌬️🍽️ زمان بیشتر، مواد اولیه دیگر، یا با /addfood یک غذا اضافه کنید.");
        put("cook.ready_header", "✅ You can cook right now — tap one for details:",
                "✅ همین الان می‌توانید این‌ها را بپزید — برای جزئیات روی یکی بزنید:");
        put("cook.shopping_header",
                "🛒 You could cook these if you grab a few things — tap one for details:",
                "🛒 اگر چند چیز بخرید می‌توانید این‌ها را هم بپزید — برای جزئیات روی یکی بزنید:");
        put("cook.detail_have", "✅ You already have:", "✅ این‌ها را از قبل دارید:");
        put("cook.detail_need", "🛒 You need to buy:", "🛒 این‌ها را باید بخرید:");
        put("cook.detail_have_everything", "🎉 You have everything for this!", "🎉 همه چیز برای این غذا را دارید!");

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
                "The cupboard's empty! 📭 Use /addfood to stock it up.",
                "قفسه خالیه! 📭 با /addfood یکی اضافه کنید.");
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
        put("edit.ask_recipe_step",
                "📝 New step %d — describe it in %d characters or less. Tap Done when finished, or Clear recipe to remove it entirely.",
                "📝 مرحله جدید %d — آن را در %d کاراکتر یا کمتر توضیح دهید. وقتی تمام شد روی «تمام» بزنید، یا برای حذف کامل دستور پخت روی «حذف دستور پخت» بزنید.");
        put("edit.saved", "Updated: %s", "به‌روزرسانی شد: %s");

        put("delete.confirm", "Delete %s? This can't be undone.", "%s حذف شود؟ این کار قابل بازگشت نیست.");
        put("delete.confirm_yes", "🗑️ Yes, delete", "🗑️ بله، حذف کن");
        put("delete.confirm_no", "🚫 Cancel", "🚫 لغو");
        put("delete.done", "🗑️ Poof! Deleted: %s", "🗑️ پاک شد! حذف شد: %s");

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
