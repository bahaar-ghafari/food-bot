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

## Production deployment (Docker)

The bot runs in production as a Docker Compose stack (`docker-compose.yml` + `Dockerfile`), currently deployed at `/opt/foodbot` on the VPS:

- **`bot`** — built from the repo's multi-stage `Dockerfile` (Maven build → `eclipse-temurin:17-jre-alpine` runtime). Reads secrets from a sibling `.env` (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `SUPERADMIN_CHAT_ID`, `FEEDBACK_CHAT_ID` — never committed). `foods.json`/`languages.json` persist under a bind-mounted `./data` directory, so they can be seeded or backed up directly from the host.
- **`autoheal`** — restarts the `bot` container if it ever reports unhealthy, covering a hung-but-running process that the plain restart policy wouldn't catch on its own.
- The container exposes an opt-in health endpoint (`HealthCheckServer`, only started when `HEALTH_ADDR` is set) at `/healthz`, which the Compose healthcheck polls every 30s.

To deploy an update: sync the repo to the server (the deploy used `rsync`, since the GitHub repo is private and the server has no stored credentials for it — `git pull` would need a deploy key or PAT set up first), then from `/opt/foodbot`:
```bash
docker compose up -d --build
```

**Note:** since the bot uses long-polling, only one instance can be running against the Telegram token at a time — stop any local/dev instance before (re)starting the production one, or Telegram will return 409 conflicts.

## Configuration

- `telegram.bot.token` / `TELEGRAM_BOT_TOKEN` — the bot token.
- `telegram.bot.username` / `TELEGRAM_BOT_USERNAME` — the bot's Telegram username.
- `superadmin.chat.id` / `SUPERADMIN_CHAT_ID` — the chat ID that can browse `/menu` and edit/delete *any* food (personal or global). Regular users can still edit/delete the foods on their own personal list, reached via `/search`. Find your numeric Telegram chat ID via a service like `@userinfobot`.
- `feedback.chat.id` / `FEEDBACK_CHAT_ID` — where `/feedback` submissions are sent. Defaults to `SUPERADMIN_CHAT_ID` if unset. Can be a group's chat ID (negative number) if the bot has been added to a group.

If `telegram.bot.token` is empty, the code falls back to `TELEGRAM_BOT_TOKEN` from the environment.

## Bot commands

- First message — asks you to pick a language (فارسی shown first, with a 🦁☀️ symbol since Unicode has no dedicated Iranian flag emoji / English) before anything else. Once chosen, the bot only accepts typed names/ingredients in that script. Persian is the default wherever a language can't be determined (e.g. a feedback group that's never run `/lang`).
- `/lang` (or the "🌐 Language" button) — change the language at any time.
- `/help` (or the "❓ Help" button) — explains everything the bot can do.
- `/start` — shows the main menu buttons.
- `/addfood` (or the "➕ Add food" button) — guided flow: pick your personal list or the global list → name → prep time in minutes → category (tap one) → ingredients → an optional recipe, managed with the recipe-step manager described below. Once you tap "✅ Done" on the recipe, you land on a **review screen** showing everything you entered, with a button per field ("✏️ Name", "✏️ Time", "✏️ Category", "✏️ Ingredients", "✏️ Recipe") to jump back and fix anything before it's saved — tapping one revisits that step and returns you straight to the review when you're done, rather than replaying the whole wizard. Tap "✅ Confirm & Save" once it looks right. The whole flow (including the review screen) is a single message that morphs as you go, and every step has a "🔙 Back" button (Back on the very first question cancels the flow).
- `/menu` (or the "📋 All foods" button) — **superadmin only.** Pick your list, the global list, or "🌐 All" (both combined), then shows food names as tappable buttons, 10 per page with "◀️"/"▶️" navigation if there are more. Tap a food to see "🍽️" its name, "🏷️" category, "⏱️" prep time, "🧾" ingredients, and "📝" recipe, plus a "⚙️ Settings" button (→ "✏️ Edit" / "🗑️ Delete") on every food, personal or global. A "🔙 Back" button is always available to retrace your steps (list → detail → settings → edit-field choice) by editing the same message in place, rather than piling up new ones. Non-admins don't see this button — they manage their own personal-list foods via `/search` instead (see below).
- `/cook` (or the "🍳 What can I cook?" button) — asks how much time you have (tap "⚡ Fast (~30m)", "🕐 ~2 hours", "🕔 ~5 hours", "📅 ~1 day", "♾️ Any", or type an exact number of minutes), then which ingredients you currently have (water and salt aren't asked about — everyone's assumed to have those already), whether you can go shopping for anything missing, and a category filter (or Any). Replies with two tappable, paginated lists: "✅ ready now" and "🍽️ could cook if you shop". Tap any food to see a clean breakdown — what you already have, what you need to buy, and its recipe, with a "🔙 Back" button to return to that same result list. Considers both your list and the global list. If literally nothing fits and shopping isn't an option, the bot will (gently) roast you for it.
- `/search <text>` (or the "🔍 Search recipe" button, which prompts for the text instead) — searches your own list and the global list, in your current language, in order:
  1. **By name** — partial, case-insensitive (e.g. "ghorme" matches "Ghormeh Sabzi").
  2. **By ingredient** — only if nothing matched by name.
  3. **Fuzzy name match** — if still nothing, and a food name is a close typo away from your query (e.g. "sibtokm" → "sibtokhm"), it asks "Did you mean ...?" before showing it.
  4. **Other language** — if still nothing, it translates the query (via the same dictionaries used for ingredient/dish-name display) and checks the *other* language's foods; if it finds one, it asks "I found a Persian/English recipe for that — want to see it?" before showing it (in its original language, since recipes aren't machine-translated).

  A single match shows the recipe directly; multiple matches show tappable buttons (10 per page) that lead to the same detail view as `/menu`, with the same "🔙 Back"/"⚙️ Settings" behavior (so you can edit/delete your own personal-list foods straight from a search result). No match at any stage gets a clear "nothing found" reply.
- `/feedback` (or the "💬 Feedback" button) — asks you to type a message, then forwards it straight to the feedback chat (`FEEDBACK_CHAT_ID`, a group or the superadmin's DM — see Configuration) with the sender's username (or first name) attached, and confirms it was sent. This includes the superadmin's own test submissions, so the round-trip is always visible.
- `/cancel` — cancels an in-progress `/addfood`, `/cook`, edit, feedback, or search flow.

### Ingredient picker

Whenever you're asked to pick ingredients (adding/editing a food, or telling `/cook` what you have), tap an existing one to toggle it, or type:
- A few letters — filters the buttons down to matches (shown with a "🔍" search chip and a "❌" to clear it). Selected ingredients always stay visible even while filtered.
- An exact existing name — toggles that ingredient directly.
- A name that matches nothing — adds it as a brand-new ingredient and selects it.

The button list is paginated at 12 per page with "◀️"/"▶️" navigation, so it stays usable even with 100+ ingredients in the system; typing narrows the search across the full list, and selected ingredients always stay visible on the first page.

### Editing

From a food's detail view → "⚙️ Settings" → "✏️ Edit", you (the food's owner, or the superadmin for any food) can change name, prep time, category, ingredients, or recipe independently.

### Recipe-step manager

Whenever you're building or editing a recipe (in `/addfood` or via "✏️ Edit" → recipe), each step is shown numbered with its own row of controls: "✏️" to retype that step's text, "⬆️"/"⬇️" to move it earlier/later (omitted at the first/last position), and "🗑️" to delete it. Type anything to add it as a new step (max 150 characters). Tap "✅ Done" when finished, or "🗑️ Clear recipe" (edit flow only) to remove the recipe entirely. Editing an existing food's recipe starts from its current steps, not from scratch.

Saved foods (name, category, prep time, ingredients, and an optional recipe) are persisted to `foods.json`, and each chat's language choice to `languages.json`, both in the working directory. The ingredient buttons shown are built from ingredients already used in foods visible to you, so the list grows as you add more.

### Language separation

Every food is tagged with the language it was created in (inferred from whether its name was typed in Persian script). All lists, ingredient pools, and `/cook` matches are filtered by your *current* language: in English mode you never see Persian-named foods or Persian ingredient words, and vice versa in Persian mode — these are effectively two separate menus, not one menu translated on the fly. Editing/deleting a food you already have open works regardless of language (it's permission-based, not menu-browsing), and preserves the food's original language tag.

### UI text catalog

All bot-facing text lives outside the code, in `src/main/resources/messages_en.json` and `messages_fa.json` (flat `"key": "text"` maps, loaded once at startup). `Messages.get(lang, key, args...)` looks a key up in the matching file and applies `String.format` if args are given; an unknown key just falls back to the key itself. To change wording or add a new string, edit the JSON files directly — `Messages.java` itself has no text in it, just the lookup/formatting logic.

## Tests

Unit tests cover `FoodRepository` (CRUD, ownership scoping, persistence, legacy-data ID backfill), `FoodCategories`, `IngredientIcons`, `IngredientTranslations`, `FoodNameTranslations`, `IngredientSearch` (the filter/cap logic behind the ingredient picker), `Paginator`, `PantryStaples`, `RecipeSteps`, `FoodSearch` (name/ingredient/fuzzy matching), `Levenshtein`, `TimeFormat`, `Messages` (catalog completeness), and `LanguageRepository`. The Telegram-facing `FoodBot` class itself isn't unit-tested — it's tightly coupled to the Telegram API's `execute()` calls — so changes there should still be manually verified by running the bot.

## Notes

- Do not commit a real bot token.
- Keep secrets in environment variables or a secure secret manager.
- `onUpdateReceived` wraps all update processing in a top-level try/catch: if anything throws unexpectedly, the user gets a bilingual "something went wrong, try again in a few minutes — we might be doing maintenance" reply instead of silence, and the exception is logged server-side. This only covers errors *while the bot is running* — if the process itself is down (e.g. mid-deploy), there's nothing the bot's own code can do about that, but Telegram's long-polling queues messages on its end, so nothing sent during a brief restart is lost; it's just processed a few seconds late once the bot reconnects.
