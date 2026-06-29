package com.foodbot.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Messages {
    private static final Map<String, Map<Lang, String>> TABLE = new HashMap<>();

    static {
        put("welcome",
                "👋 Hey! Welcome to FoodBot — your personal kitchen sidekick. What are we doing today?",
                "👋 سلام! به فودبات خوش اومدی — دستیار آشپزخونه‌ات. امروز چیکار می‌کنیم؟");
        put("btn.add_food", "➕ Add food", "➕ افزودن غذا");
        put("btn.all_foods", "📋 All foods", "📋 همه غذاها");
        put("btn.what_can_cook", "🍳 What can I cook?", "🍳 چی می‌تونم بپزم؟");
        put("btn.change_lang", "🌐 Language", "🌐 زبان");
        put("btn.help", "❓ Help", "❓ راهنما");
        put("btn.feedback", "💬 Feedback", "💬 بازخورد");
        put("btn.search", "🔍 Search recipe", "🔍 جستجوی غذا");
        put("btn.settings", "⚙️ Settings", "⚙️ تنظیمات");
        put("btn.back", "🔙 Back", "🔙 بازگشت");
        put("btn.edit", "✏️ Edit", "✏️ ویرایش");
        put("btn.delete", "🗑️ Delete", "🗑️ حذف");
        put("btn.done_recipe", "✅ Done", "✅ تمام");
        put("btn.clear_recipe", "🗑️ Clear recipe", "🗑️ حذف دستور پخت");
        put("help.text",
                "🍽️ FoodBot helps you track recipes and decide what to cook.\n\n"
                        + "➕ Add food — add a recipe to your personal list or the shared global list: name, prep time, category, ingredients (tap existing ones, type a few letters to search a long list, or type a full new name to add it), then an optional recipe. You'll get a chance to review and fix anything before it's saved.\n\n"
                        + "🍳 What can I cook? — pick a time preset (or type one), say what you have and whether you can shop, then tap any suggested food to see what you have vs. need to buy, plus its recipe.\n\n"
                        + "🔍 Search recipe — type a food name (or part of it, like \"ghorme\" for Ghormeh Sabzi) and the closest matches show up; if nothing matches by name, it checks ingredients instead. One match shows the recipe right away, more than one shows tappable buttons to pick from. On a food from your own personal list, you'll also see a \"⚙️ Settings\" button to ✏️ Edit or 🗑️ Delete it. Foods on the global list can only be edited or deleted by the admin.\n\n"
                        + "🌐 Language — switch between English and Persian anytime.\n\n"
                        + "💬 Feedback — send a message straight to the admin.\n\n"
                        + "/cancel — cancel whatever you're doing.\n\n"
                        + "/help — show this message.",
                "🍽️ فودبات به شما کمک می‌کند دستور پخت‌ها را پیگیری کنید و تصمیم بگیرید چه چیزی بپزید.\n\n"
                        + "➕ افزودن غذا — یک غذا به لیست شخصی یا لیست عمومی اضافه کنید: اسم، زمان آماده‌سازی، دسته‌بندی، مواد اولیه (روی موادِ موجود بزنید، برای جستجو در لیست بلند چند حرف تایپ کنید، یا اسم کامل یک ماده جدید را تایپ کنید تا اضافه شود)، و در آخر یک دستور پخت اختیاری. قبل از ذخیره شدن، فرصت بازبینی و اصلاح آن را خواهید داشت.\n\n"
                        + "🍳 چی می‌تونم بپزم؟ — یک زمان آماده انتخاب کنید (یا تایپ کنید)، بگویید چی دارید و می‌توانید خرید کنید یا نه، سپس روی هر غذای پیشنهادی بزنید تا ببینید چی دارید و چی باید بخرید، به همراه دستور پختش.\n\n"
                        + "🔍 جستجوی غذا — اسم یک غذا (یا بخشی از آن) را تایپ کنید تا نزدیک‌ترین نتایج نشان داده شود؛ اگر چیزی با اسم پیدا نشد، مواد اولیه را هم بررسی می‌کند. اگر فقط یک نتیجه باشد، دستور پختش را مستقیم نشان می‌دهد؛ اگر بیشتر بود، دکمه‌هایی برای انتخاب نشان می‌دهد. روی غذایی از لیست شخصی خودتان، دکمه «⚙️ تنظیمات» را هم می‌بینید تا ✏️ ویرایش یا 🗑️ حذف کنید. غذاهای لیست عمومی فقط توسط ادمین قابل ویرایش یا حذف هستند.\n\n"
                        + "🌐 زبان — هر وقت خواستید بین انگلیسی و فارسی تغییر دهید.\n\n"
                        + "💬 بازخورد — پیامی مستقیم برای ادمین ارسال کنید.\n\n"
                        + "/cancel — هر کاری که در حال انجام است را لغو می‌کند.\n\n"
                        + "/help — نشان دادن همین پیام.");
        put("permission.denied",
                "Only the admin, or whoever added this to their personal list, can do that.",
                "فقط ادمین یا کسی که این را به لیست شخصی خودش اضافه کرده می‌تواند این کار را انجام دهد.");

        put("addfood.ask_scope",
                "Add this to your personal list or the global list? (personal = only you can see it, global = everyone can see it)",
                "این رو به لیست شخصی یا لیست عمومی اضافه کنم؟ (شخصی = فقط شما می‌بینید، عمومی = همه می‌بینند)");
        put("addfood.scope.mine", "👤 My list", "👤 لیست من");
        put("addfood.scope.global", "🌍 Global list", "🌍 لیست عمومی");
        put("scope.mine", "👤 My list", "👤 لیست من");
        put("scope.global", "🌍 Global list", "🌍 لیست عمومی");
        put("scope.all", "🌐 All", "🌐 همه");
        put("addfood.ask_name",
                "Ooh, a new recipe! 🍽️ What's it called?",
                "اوه، یه غذای جدید! 🍽️ اسمش چیه؟");
        put("addfood.ask_time",
                "Nice — %s sounds delicious! ⏱️ How long does it take to make? (in minutes)",
                "عالیه — %s خوشمزه به نظر میرسه! ⏱️ چند دقیقه طول می‌کشه؟");
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
        put("recipe.step_too_long",
                "That's %d characters — please keep each step to %d characters or less.",
                "این %d کاراکتر است — لطفاً هر مرحله را در %d کاراکتر یا کمتر بنویسید.");
        put("recipe.manager.header", "📝 Recipe steps:", "📝 مراحل دستور پخت:");
        put("recipe.manager.empty", "No steps yet.", "هنوز مرحله‌ای نیست.");
        put("recipe.manager.prompt",
                "Type a new step (max %d characters). Use the buttons below to edit, reorder, or delete existing steps. Tap Done when finished.",
                "یک مرحله جدید تایپ کنید (حداکثر %d کاراکتر). با دکمه‌های پایین می‌توانید مراحل را ویرایش، جابه‌جا یا حذف کنید. وقتی تمام شد روی «تمام» بزنید.");
        put("recipe.manager.editing_step",
                "✏️ Editing step %d — type its new text (max %d characters).",
                "✏️ در حال ویرایش مرحله %d — متن جدید آن را تایپ کنید (حداکثر %d کاراکتر).");
        put("addfood.saved_toast", "Saved!", "ذخیره شد!");
        put("addfood.saved_message",
                "🎉 Boom! %s is now in your list. Happy cooking!",
                "🎉 آفرین! %s به لیستت اضافه شد. نوش جان!");
        put("addfood.review_header",
                "📝 Review your food before saving. Tap a field to change it, or Confirm & Save if it looks good.",
                "📝 قبل از ذخیره، غذا را بررسی کنید. برای تغییر هر بخش روی آن بزنید، یا اگر خوب است روی «تأیید و ذخیره» بزنید.");
        put("addfood.confirm_save", "✅ Confirm & Save", "✅ تأیید و ذخیره");

        put("selection_expired",
                "⏰ Oops, that selection expired. Give it another go!",
                "⏰ اوپس، این انتخاب منقضی شده. دوباره امتحان کن!");
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
                "Nothing matched your vibe right now. 🤷 Try more time, swap some ingredients, or add new foods with /addfood.",
                "چیزی پیدا نشد که جور باشه. 🤷 زمان بیشتر، مواد دیگه، یا با /addfood غذا اضافه کن.");
        put("cook.nothing_matches_no_shop",
                "Nothing works and shopping's out... guess it's a cereal night! 🥣 Try different ingredients or add more foods with /addfood.",
                "هیچی جور نشد و خرید هم نه... امشب شام نداریم! 🥣 مواد دیگه امتحان کن یا با /addfood غذا اضافه کن.");
        put("cook.ready_header",
                "🔥 You're all set! Tap one to see the details:",
                "🔥 آماده‌ای! روی یکی بزن تا جزئیات ببینی:");
        put("cook.shopping_header",
                "🛒 Just grab a few things and you're golden — tap one to see what you need:",
                "🛒 فقط چند چیز کم داری — روی یکی بزن ببین چی باید بخری:");
        put("cook.detail_have", "✅ You already have:", "✅ این‌ها را از قبل دارید:");
        put("cook.detail_need", "🛒 You need to buy:", "🛒 این‌ها را باید بخرید:");
        put("cook.detail_have_everything",
                "🎉 You've got everything! Time to get cooking!",
                "🎉 همه چیز داری! وقت پختنه!");

        put("removed", "Removed %s", "%s حذف شد");
        put("added", "Added %s", "%s اضافه شد");
        put("cancelled", "Cancelled.", "لغو شد.");
        put("feedback.ask",
                "✍️ Type your feedback or suggestion below and I'll pass it along.",
                "✍️ بازخورد یا پیشنهاد خود را تایپ کنید تا برایش ارسال کنم.");
        put("feedback.sent",
                "🙏 Got it — thanks for the input! We'll take a look.",
                "🙏 دریافت شد — ممنون از بازخوردت! نگاهی می‌اندازیم.");
        put("feedback.notify_admin",
                "📬 New feedback from %s:\n\n%s",
                "📬 بازخورد جدید از %s:\n\n%s");
        put("fallback",
                "Hmm, didn't catch that. 🤔 Use the buttons below or try /addfood!",
                "نفهمیدم چی گفتی. 🤔 از دکمه‌های پایین یا /addfood استفاده کن!");

        put("foods.ask_scope",
                "Which list do you want to see?",
                "کدوم لیست رو می‌خواید ببینید؟");
        put("foods.none",
                "Your list is empty! 😅 Start adding foods with /addfood.",
                "لیستت خالیه! 😅 با /addfood شروع به اضافه کردن کن.");
        put("foods.header.mine", "👤 Your foods, tap one for details:", "👤 غذاهای شما، برای جزئیات روی یکی بزنید:");
        put("foods.header.global", "🌍 Global foods, tap one for details:", "🌍 غذاهای عمومی، برای جزئیات روی یکی بزنید:");
        put("foods.header.all", "🌐 All foods (yours + global), tap one for details:",
                "🌐 همه غذاها (شما + عمومی)، برای جزئیات روی یکی بزنید:");
        put("search.ask",
                "🔍 Type a food name (or part of it), or an ingredient, to search for:",
                "🔍 اسم غذا (یا بخشی از آن) یا یک ماده اولیه را برای جستجو تایپ کنید:");
        put("search.results_header", "🔍 Results for \"%s\", tap one for details:",
                "🔍 نتایج برای «%s»، برای جزئیات روی یکی بزنید:");
        put("search.no_results",
                "😕 Couldn't find anything for \"%s\". Maybe a typo? Try a different word!",
                "😕 چیزی برای «%s» پیدا نشد. شاید اشتباه تایپ شده؟ کلمه دیگه‌ای امتحان کن!");
        put("search.fuzzy_suggestion", "🤔 Did you mean \"%s\"?", "🤔 منظورتون «%s» بود؟");
        put("search.fuzzy_yes", "✅ Yes, that's it", "✅ بله، همینه");
        put("search.fuzzy_no", "❌ No", "❌ نه");
        put("search.otherlang_suggestion",
                "🌐 Nothing in this language, but I found a %s recipe for that — want to see it?",
                "🌐 چیزی به این زبان نبود، ولی یک دستور پخت %s برایش پیدا کردم — می‌خواهید ببینید؟");
        put("search.otherlang_yes", "✅ Yes, show it", "✅ بله، نشانش بده");
        put("search.otherlang_no", "❌ No, thanks", "❌ نه، ممنون");
        put("lang.name.fa", "Persian", "فارسی");
        put("lang.name.en", "English", "انگلیسی");
        put("settings.choose_action", "What do you want to do?", "چه کاری می‌خواهید انجام دهید؟");
        put("food.detail_category", "Category: %s", "دسته‌بندی: %s");
        put("food.detail_time", "Prep time: %s", "زمان آماده‌سازی: %s");
        put("food.detail_ingredients", "Ingredients: %s", "مواد اولیه: %s");
        put("food.detail_recipe", "Recipe:\n%s", "دستور پخت:\n%s");
        put("food.no_recipe", "(no recipe added yet)", "(هنوز دستور پختی اضافه نشده)");
        put("min_unit", "min", "دقیقه");
        put("time.hour", "hour", "ساعت");
        put("time.hours", "hours", "ساعت");
        put("time.day", "day", "روز");
        put("time.days", "days", "روز");
        put("done_button", "✅ Done (%d selected)", "✅ تمام (%d انتخاب شده)");

        put("lang.prompt",
                "Please choose your language / لطفاً زبان خود را انتخاب کنید:",
                "Please choose your language / لطفاً زبان خود را انتخاب کنید:");
        put("lang.confirmed", "Language set to English.", "زبان به فارسی تغییر یافت.");
        put("lang.wrong_script",
                "Please type in English, or change the language with /lang.",
                "لطفاً به فارسی تایپ کنید، یا با /lang زبان را تغییر دهید.");

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
        put("edit.saved", "Updated: %s", "به‌روزرسانی شد: %s");

        put("delete.confirm", "Delete %s? This can't be undone.", "%s حذف شود؟ این کار قابل بازگشت نیست.");
        put("delete.confirm_yes", "🗑️ Yes, delete", "🗑️ بله، حذف کن");
        put("delete.confirm_no", "🚫 Cancel", "🚫 لغو");
        put("delete.done",
                "💨 Gone! %s has been wiped from existence.",
                "💨 رفت! %s از لیست پاک شد.");

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
