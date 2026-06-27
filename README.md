# FoodBot

A minimal Telegram bot project.

## Setup

1. Add your Telegram bot token:
   - `src/main/resources/application.properties`
   - or set `TELEGRAM_BOT_TOKEN` in your environment

2. Build the project:
   ```bash
   mvn package
   ```

3. Run the bot:
   ```bash
   java -jar target/food-bot-0.0.1-SNAPSHOT.jar
   ```

## Configuration

- `telegram.bot.token` in `application.properties`
- `telegram.bot.username` in `application.properties`

If `telegram.bot.token` is empty, the code will use `TELEGRAM_BOT_TOKEN` from the environment.

## Bot commands

- `/addfood` - starts a guided flow to add a food (asks for name, prep time in minutes, then ingredients).
- `/menu` - lists all saved foods.
- `/cancel` - cancels an in-progress `/addfood` flow.

Saved foods are persisted to `foods.json` in the working directory.

## Notes

- Do not commit a real bot token.
- Keep secrets in environment variables or a secure secret manager.
