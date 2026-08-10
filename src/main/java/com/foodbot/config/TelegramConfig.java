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
        String token = firstNonBlank(dotenv.get("TELEGRAM_BOT_TOKEN"), properties.getProperty("telegram.bot.token"),
                System.getenv("TELEGRAM_BOT_TOKEN"));
        if (token == null) {
            throw new IllegalStateException("Telegram token is missing. Set TELEGRAM_BOT_TOKEN in .env or telegram.bot.token in application.properties.");
        }
        return token;
    }

    public String getBotUsername() {
        return firstNonBlank(dotenv.get("TELEGRAM_BOT_USERNAME"), properties.getProperty("telegram.bot.username"),
                "FoodBot");
    }

    public Long getSuperAdminChatId() {
        String value = firstNonBlank(dotenv.get("SUPERADMIN_CHAT_ID"), properties.getProperty("superadmin.chat.id"),
                System.getenv("SUPERADMIN_CHAT_ID"));
        return value == null ? null : Long.parseLong(value.trim());
    }

    public String getAnthropicApiKey() {
        String value = firstNonBlank(dotenv.get("ANTHROPIC_API_KEY"), properties.getProperty("anthropic.api.key"),
                System.getenv("ANTHROPIC_API_KEY"));
        return value == null ? null : value.trim();
    }

    public Long getFeedbackChatId() {
        String value = firstNonBlank(dotenv.get("FEEDBACK_CHAT_ID"), properties.getProperty("feedback.chat.id"),
                System.getenv("FEEDBACK_CHAT_ID"));
        return value == null ? getSuperAdminChatId() : Long.parseLong(value.trim());
    }

    /** Returns the first non-null, non-blank value, preserving the given precedence order. */
    static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
