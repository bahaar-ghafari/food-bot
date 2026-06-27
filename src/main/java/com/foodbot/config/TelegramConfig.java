package com.foodbot.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TelegramConfig {
    private static final String PROPERTY_FILE = "application.properties";
    private final Properties properties = new Properties();
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public TelegramConfig() {
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(PROPERTY_FILE)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + PROPERTY_FILE, e);
        }
    }

    public String getBotToken() {
        String token = dotenv.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            token = properties.getProperty("telegram.bot.token");
        }
        if (token == null || token.isBlank()) {
            token = System.getenv("TELEGRAM_BOT_TOKEN");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Telegram token is missing. Set TELEGRAM_BOT_TOKEN in .env or telegram.bot.token in application.properties.");
        }
        return token;
    }

    public String getBotUsername() {
        String username = dotenv.get("TELEGRAM_BOT_USERNAME");
        if (username != null && !username.isBlank()) {
            return username;
        }
        username = properties.getProperty("telegram.bot.username");
        if (username == null || username.isBlank()) {
            return "FoodBot";
        }
        return username;
    }
}
