package com.foodbot.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class FoodBot extends TelegramLongPollingBot {
    private final String token;
    private final String username;

    public FoodBot(String token, String username) {
        this.token = token;
        this.username = username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String incomingText = update.getMessage().getText();
            SendMessage reply = new SendMessage(chatId, "You said: " + incomingText);
            try {
                execute(reply);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
