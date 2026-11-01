package com.foodbot.bot;

import com.foodbot.food.CookRanking;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.Food;
import com.foodbot.food.IngredientTranslations;
import com.foodbot.food.ShoppingListRepository.Item;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.foodbot.bot.BotConstants.*;

class ShoppingHandler extends FlowHandler {
    ShoppingHandler(BotContext ctx) {
        super(ctx);
    }

    /** Adds the ingredients of a /cook result that the user said they don't have. */
    void handleAddMissingCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        Optional<Food> foodOpt = foodRepository.findById(data.substring(CB_SHOP_ADD.length()));
        CookResultContext cookCtx = lastCookResults.get(chatId);
        if (foodOpt.isEmpty() || cookCtx == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        int added = shoppingListRepository.addAll(chatId, missingIngredients(foodOpt.get(), cookCtx));
        answerCallback(callbackQuery.getId(),
                Messages.get(lang, added > 0 ? "shop.added" : "shop.nothing_new", String.valueOf(added)), false);
    }

    List<String> missingIngredients(Food food, CookResultContext cookCtx) {
        List<String> missing = new ArrayList<>();
        for (String ingredient : food.getIngredients()) {
            if (!CookRanking.effectivelyHas(cookCtx.getHaveIngredients(), ingredient)) {
                missing.add(ingredient);
            }
        }
        return missing;
    }

    void handleListCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        if (data.equals(CB_SHOP_OPEN)) {
            answerCallback(callbackQuery.getId(), null, false);
            sendShoppingList(chatId, lang, null);
            return;
        }
        if (data.equals(CB_SHOP_CLEAR)) {
            int removed = shoppingListRepository.clearChecked(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "shop.cleared", String.valueOf(removed)), false);
        } else if (data.startsWith(CB_SHOP_TOGGLE)) {
            shoppingListRepository.toggleChecked(chatId, Integer.parseInt(data.substring(CB_SHOP_TOGGLE.length())));
            answerCallback(callbackQuery.getId(), null, false);
        } else {
            answerCallback(callbackQuery.getId(), null, false);
            return;
        }
        sendShoppingList(chatId, lang, callbackQuery.getMessage().getMessageId());
    }

    /** Sends the list, or edits the message at {@code editMessageId} in place when given. */
    void sendShoppingList(long chatId, Lang lang, Integer editMessageId) {
        List<Item> items = shoppingListRepository.itemsOf(chatId);
        if (items.isEmpty()) {
            if (editMessageId != null) {
                editMessageTextAndMarkup(chatId, editMessageId, Messages.get(lang, "shop.empty"),
                        new InlineKeyboardMarkup(List.of()));
            } else {
                sendWithMainMenu(chatId, lang, Messages.get(lang, "shop.empty"));
            }
            return;
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buildListRows(items, lang));
        String header = Messages.get(lang, "shop.header");
        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, header, markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), header);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    List<List<InlineKeyboardButton>> buildListRows(List<Item> items, Lang lang) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        boolean anyChecked = false;
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            anyChecked |= item.checked();
            String label = (item.checked() ? "☑️ " : "⬜ ") + iconPrefix(item.ingredient())
                    + IngredientTranslations.translate(item.ingredient(), lang);
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(CB_SHOP_TOGGLE + i);
            rows.add(List.of(button));
        }
        if (anyChecked) {
            InlineKeyboardButton clear = new InlineKeyboardButton(Messages.get(lang, "shop.btn_clear"));
            clear.setCallbackData(CB_SHOP_CLEAR);
            rows.add(List.of(clear));
        }
        return rows;
    }
}
