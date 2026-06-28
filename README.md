# FoodBot

A minimal Telegram bot project.

## Setup

1. Add your Telegram bot token and (optionally) your superadmin chat ID:
   - `.env` (see `.env.example`) — recommended
   - or `src/main/resources/application.properties`
   - or `TELEGRAM_BOT_TOKEN` / `SUPERADMIN_CHAT_ID` environment variables

2. Build the project:
   ```bash
   mvn package
   ```

3. Run the tests:
   ```bash
   mvn test
   ```

4. Run the bot:
   ```bash
   java -jar target/food-bot-0.0.1-SNAPSHOT.jar
   ```

## Configuration

- `telegram.bot.token` / `TELEGRAM_BOT_TOKEN` — the bot token.
- `telegram.bot.username` / `TELEGRAM_BOT_USERNAME` — the bot's Telegram username.
- `superadmin.chat.id` / `SUPERADMIN_CHAT_ID` — chat ID that can edit/delete any food, regardless of who added it. Use `/whoami` to find your own chat ID.

If `telegram.bot.token` is empty, the code falls back to `TELEGRAM_BOT_TOKEN` from the environment.

## Bot commands

- First message — asks you to pick a language (English / فارسی) before anything else. Once chosen, the bot only accepts typed names/ingredients in that script.
- `/lang` (or the "🌐 Language" button) — change the language at any time.
- `/help` (or the "❓ Help" button) — explains everything the bot can do.
- `/whoami` — replies with your chat ID (useful for configuring `SUPERADMIN_CHAT_ID`).
- `/start` — shows the main menu buttons.
- `/addfood` (or the "➕ Add food" button) — guided flow: pick your personal list or the global list → name → prep time in minutes → category (tap one) → ingredients → an optional recipe (tap "⏭ Skip" to leave it blank). Tap "✅ Done" after ingredients to move on.
- `/menu` (or the "📋 All foods" button) — **superadmin only.** Asks which list to view (yours or global), then shows food names as tappable buttons, 10 per page with "◀️"/"▶️" navigation if there are more. Tap a food to see its category, prep time, ingredients, and recipe, plus a "⚙️ Settings" button (→ "✏️ Edit" / "🗑️ Delete"). Non-admin users don't see this button and `/menu` does nothing for them — they can still `/addfood` and `/cook`, just not browse/edit/delete the saved lists.
- `/cook` (or the "🍳 What can I cook?" button) — asks how much time you have (tap "⚡ Fast (~30m)", "🕐 ~2 hours", "🕔 ~5 hours", "📅 ~1 day", "♾️ Any", or type an exact number of minutes), then which ingredients you currently have, whether you can go shopping for anything missing, and a category filter (or Any). Replies with two tappable, paginated lists: "✅ ready now" and "🛒 could cook if you shop". Tap any food to see a clean breakdown — what you already have, what you need to buy, and its recipe. Considers both your list and the global list.
- `/cancel` — cancels an in-progress `/addfood`, `/cook`, or edit flow.

### Ingredient picker

Whenever you're asked to pick ingredients (adding/editing a food, or telling `/cook` what you have), tap an existing one to toggle it, or type:
- A few letters — filters the buttons down to matches (shown with a "🔍" search chip and a "❌" to clear it). Selected ingredients always stay visible even while filtered.
- An exact existing name — toggles that ingredient directly.
- A name that matches nothing — adds it as a brand-new ingredient and selects it.

The button list is paginated at 12 per page with "◀️"/"▶️" navigation, so it stays usable even with 100+ ingredients in the system; typing narrows the search across the full list, and selected ingredients always stay visible on the first page.

### Editing

From a food's detail view → "⚙️ Settings" → "✏️ Edit", you can change name, prep time, category, ingredients, or recipe independently. Clearing a recipe is done from the recipe-edit prompt's "🗑️ Clear recipe" button rather than typing.

Saved foods (name, category, prep time, ingredients, and an optional recipe) are persisted to `foods.json`, and each chat's language choice to `languages.json`, both in the working directory. The ingredient buttons shown are built from ingredients already used in foods visible to you, so the list grows as you add more.

### Language separation

Every food is tagged with the language it was created in (inferred from whether its name was typed in Persian script). All lists, ingredient pools, and `/cook` matches are filtered by your *current* language: in English mode you never see Persian-named foods or Persian ingredient words, and vice versa in Persian mode — these are effectively two separate menus, not one menu translated on the fly. Editing/deleting a food you already have open works regardless of language (it's permission-based, not menu-browsing), and preserves the food's original language tag.

## Tests

Unit tests cover `FoodRepository` (CRUD, ownership scoping, persistence, legacy-data ID backfill), `FoodCategories`, `IngredientIcons`, `IngredientTranslations`, `FoodNameTranslations`, `IngredientSearch` (the filter/cap logic behind the ingredient picker), `Messages` (catalog completeness), and `LanguageRepository`. The Telegram-facing `FoodBot` class itself isn't unit-tested — it's tightly coupled to the Telegram API's `execute()` calls — so changes there should still be manually verified by running the bot.

## Notes

- Do not commit a real bot token.
- Keep secrets in environment variables or a secure secret manager.
