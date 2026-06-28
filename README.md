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

- **`bot`** — built from the repo's multi-stage `Dockerfile` (Maven build → `eclipse-temurin:17-jre-alpine` runtime). Reads secrets from a sibling `.env` (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, `SUPERADMIN_CHAT_ID` — never committed). `foods.json`/`languages.json` persist under a bind-mounted `./data` directory, so they can be seeded or backed up directly from the host.
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
- `superadmin.chat.id` / `SUPERADMIN_CHAT_ID` — the only chat ID that can browse `/menu` or edit/delete any food. Find your numeric Telegram chat ID via a service like `@userinfobot`.

If `telegram.bot.token` is empty, the code falls back to `TELEGRAM_BOT_TOKEN` from the environment.

## Bot commands

- First message — asks you to pick a language (English / فارسی, shown with a 🦁☀️ symbol — Unicode has no dedicated Iranian flag emoji for this, so a lion-and-sun pair stands in for it) before anything else. Once chosen, the bot only accepts typed names/ingredients in that script.
- `/lang` (or the "🌐 Language" button) — change the language at any time.
- `/help` (or the "❓ Help" button) — explains everything the bot can do.
- `/start` — shows the main menu buttons.
- `/addfood` (or the "➕ Add food" button) — guided flow: pick your personal list or the global list → name → prep time in minutes → category (tap one) → ingredients → an optional recipe, entered one step at a time (each step up to 150 characters; tap "✅ Done" once you've entered every step, or tap Done immediately to skip the recipe). Tap "✅ Done" after ingredients to move on to the recipe step. The whole flow is a single message that morphs as you go — each question replaces the previous one instead of piling up new messages — and every step has a "🔙 Back" button to revisit the previous question (category, ingredient, and recipe-step choices are kept when you go back and then forward again; a typed name or prep time just gets asked again). Back on the very first question cancels the flow.
- `/menu` (or the "📋 All foods" button) — **superadmin only.** Asks which list to view (yours or global), then shows food names as tappable buttons, 10 per page with "◀️"/"▶️" navigation if there are more. Tap a food to see "🍽️" its name, "🏷️" category, "⏱️" prep time, "🧾" ingredients, and "📝" recipe, plus a "⚙️ Settings" button (→ "✏️ Edit" / "🗑️ Delete") — shown only to the superadmin, regardless of who added the food. A "🔙 Back" button is always available to retrace your steps (list → detail → settings → edit-field choice) by editing the same message in place, rather than piling up new ones. Non-admin users don't see the Settings button and `/menu` does nothing for them — they can still `/addfood` and `/cook`, just not browse/edit/delete the saved lists.
- `/cook` (or the "🍳 What can I cook?" button) — asks how much time you have (tap "⚡ Fast (~30m)", "🕐 ~2 hours", "🕔 ~5 hours", "📅 ~1 day", "♾️ Any", or type an exact number of minutes), then which ingredients you currently have (water and salt aren't asked about — everyone's assumed to have those already), whether you can go shopping for anything missing, and a category filter (or Any). Replies with two tappable, paginated lists: "✅ ready now" and "🍽️ could cook if you shop". Tap any food to see a clean breakdown — what you already have, what you need to buy, and its recipe, with a "🔙 Back" button to return to that same result list. Considers both your list and the global list. If literally nothing fits and shopping isn't an option, the bot will (gently) roast you for it.
- `/feedback` (or the "💬 Feedback" button) — asks you to type a message, then forwards it straight to the superadmin's chat (with your chat ID/username attached) and confirms it was sent. The superadmin doesn't get a self-notification if they send feedback themselves.
- `/cancel` — cancels an in-progress `/addfood`, `/cook`, edit, or feedback flow.

### Ingredient picker

Whenever you're asked to pick ingredients (adding/editing a food, or telling `/cook` what you have), tap an existing one to toggle it, or type:
- A few letters — filters the buttons down to matches (shown with a "🔍" search chip and a "❌" to clear it). Selected ingredients always stay visible even while filtered.
- An exact existing name — toggles that ingredient directly.
- A name that matches nothing — adds it as a brand-new ingredient and selects it.

The button list is paginated at 12 per page with "◀️"/"▶️" navigation, so it stays usable even with 100+ ingredients in the system; typing narrows the search across the full list, and selected ingredients always stay visible on the first page.

### Editing

From a food's detail view → "⚙️ Settings" → "✏️ Edit", you (the superadmin) can change name, prep time, category, ingredients, or recipe independently. Editing the recipe restarts the step-by-step entry from scratch; tap "🗑️ Clear recipe" at any step to remove it entirely instead.

Saved foods (name, category, prep time, ingredients, and an optional recipe) are persisted to `foods.json`, and each chat's language choice to `languages.json`, both in the working directory. The ingredient buttons shown are built from ingredients already used in foods visible to you, so the list grows as you add more.

### Language separation

Every food is tagged with the language it was created in (inferred from whether its name was typed in Persian script). All lists, ingredient pools, and `/cook` matches are filtered by your *current* language: in English mode you never see Persian-named foods or Persian ingredient words, and vice versa in Persian mode — these are effectively two separate menus, not one menu translated on the fly. Editing/deleting a food you already have open works regardless of language (it's permission-based, not menu-browsing), and preserves the food's original language tag.

## Tests

Unit tests cover `FoodRepository` (CRUD, ownership scoping, persistence, legacy-data ID backfill), `FoodCategories`, `IngredientIcons`, `IngredientTranslations`, `FoodNameTranslations`, `IngredientSearch` (the filter/cap logic behind the ingredient picker), `Paginator`, `PantryStaples`, `Messages` (catalog completeness), and `LanguageRepository`. The Telegram-facing `FoodBot` class itself isn't unit-tested — it's tightly coupled to the Telegram API's `execute()` calls — so changes there should still be manually verified by running the bot.

## Notes

- Do not commit a real bot token.
- Keep secrets in environment variables or a secure secret manager.
