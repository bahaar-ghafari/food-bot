package com.foodbot.bot;

import com.foodbot.config.TelegramConfig;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class FoodBotApplication {
    public static void main(String[] args) {
        TelegramConfig config = new TelegramConfig();

        String token = config.getBotToken();
        String username = config.getBotUsername();

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new FoodBot(token, username));
            System.out.println("FoodBot started as " + username);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
