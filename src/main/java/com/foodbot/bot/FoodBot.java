package com.foodbot.bot;

import com.foodbot.food.AddFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FoodBot extends TelegramLongPollingBot {
    private final String token;
    private final String username;
    private final FoodRepository foodRepository = new FoodRepository();
    private final Map<Long, AddFoodSession> activeSessions = new ConcurrentHashMap<>();

    public FoodBot(String token, String username) {
        this.token = token;
        this.username = username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        String reply;
        if (text.equalsIgnoreCase("/addfood")) {
            activeSessions.put(chatId, new AddFoodSession());
            reply = "Let's add a new food. What's its name?";
        } else if (text.equalsIgnoreCase("/cancel")) {
            activeSessions.remove(chatId);
            reply = "Cancelled.";
        } else if (text.equalsIgnoreCase("/menu")) {
            reply = renderMenu();
        } else if (activeSessions.containsKey(chatId)) {
            reply = handleSessionStep(chatId, text);
        } else {
            reply = "I didn't get that. Try /addfood to add a food, or /menu to see what's saved.";
        }

        send(chatId, reply);
    }

    private String handleSessionStep(long chatId, String text) {
        AddFoodSession session = activeSessions.get(chatId);
        switch (session.getStep()) {
            case AWAITING_NAME:
                session.setName(text);
                session.setStep(AddFoodSession.Step.AWAITING_PREP_TIME);
                return "Got it: " + text + ". How many minutes does it take to prepare?";

            case AWAITING_PREP_TIME:
                int minutes;
                try {
                    minutes = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    return "That doesn't look like a number. How many minutes does it take to prepare?";
                }
                session.setPrepTimeMinutes(minutes);
                session.setStep(AddFoodSession.Step.AWAITING_INGREDIENTS);
                return "What does it have? List ingredients separated by commas (e.g. egg, beef).";

            case AWAITING_INGREDIENTS:
                List<String> ingredients = new ArrayList<>();
                for (String part : text.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        ingredients.add(trimmed);
                    }
                }
                if (ingredients.isEmpty()) {
                    return "Please list at least one ingredient, separated by commas.";
                }
                Food food = new Food(session.getName(), session.getPrepTimeMinutes(), ingredients);
                foodRepository.add(food);
                activeSessions.remove(chatId);
                return "Saved: " + food;

            default:
                activeSessions.remove(chatId);
                return "Something went wrong, let's start over with /addfood.";
        }
    }

    private String renderMenu() {
        List<Food> foods = foodRepository.findAll();
        if (foods.isEmpty()) {
            return "No foods saved yet. Use /addfood to add one.";
        }
        StringBuilder builder = new StringBuilder("Menu:\n");
        for (Food food : foods) {
            builder.append("- ").append(food).append("\n");
        }
        return builder.toString().trim();
    }

    private void send(long chatId, String text) {
        SendMessage reply = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(reply);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
