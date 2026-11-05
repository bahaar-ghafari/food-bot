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
        List<String> added = shoppingListRepository.addNew(listKey(chatId), missingIngredients(foodOpt.get(), cookCtx));
        answerCallback(callbackQuery.getId(),
                Messages.get(lang, !added.isEmpty() ? "shop.added" : "shop.nothing_new", String.valueOf(added.size())), false);
        ctx.partner.notifyAdded(chatId, added);
    }

    /** The key the user's list is stored under: their own chat id, or the shared one when they have a partner. */
    long listKey(long chatId) {
        return partnerRepository.listOwner(chatId);
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
        if (data.equals(CB_SHOP_OPEN) || data.equals(CB_SHOP_DONE)) {
            awaitingShoppingItem.remove(chatId);
            answerCallback(callbackQuery.getId(), null, false);
            if (data.equals(CB_SHOP_DONE)) {
                // finished adding: tell the partner everything new in one message
                List<String> pending = ctx.pendingPartnerNotice.remove(chatId);
                if (pending != null) {
                    ctx.partner.notifyAdded(chatId, pending);
                }
            }
            sendShoppingList(chatId, lang, null);
            return;
        }
        if (data.equals(CB_SHOP_ADD_ITEM)) {
            answerCallback(callbackQuery.getId(), null, false);
            addFoodSessions.remove(chatId);
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            awaitingFeedback.remove(chatId);
            awaitingSearch.remove(chatId);
            awaitingFollowId.remove(chatId);
            awaitingShoppingItem.add(chatId);
            sendWithHomeButton(chatId, lang, Messages.get(lang, "shop.add_prompt"));
            return;
        }
        if (data.equals(CB_SHOP_CLEAR)) {
            int removed = shoppingListRepository.clearChecked(listKey(chatId));
            answerCallback(callbackQuery.getId(), Messages.get(lang, "shop.cleared", String.valueOf(removed)), false);
        } else if (data.startsWith(CB_SHOP_TOGGLE)) {
            shoppingListRepository.toggleChecked(listKey(chatId), Integer.parseInt(data.substring(CB_SHOP_TOGGLE.length())));
            answerCallback(callbackQuery.getId(), null, false);
        } else {
            answerCallback(callbackQuery.getId(), null, false);
            return;
        }
        sendShoppingList(chatId, lang, callbackQuery.getMessage().getMessageId());
    }

    /**
     * Adds typed items (one per line or comma-separated). The user stays in add mode, so they can keep
     * sending messages, until they tap Done (or leave via another command).
     */
    void handleItemText(long chatId, String text, Lang lang) {
        List<String> items = new ArrayList<>();
        for (String part : text.split("[,\\n،]")) {
            String item = IngredientTranslations.translate(part.trim(), Lang.EN).toLowerCase();
            if (!item.isEmpty() && !items.contains(item)) {
                items.add(item);
            }
        }
        List<String> newItems = items.isEmpty() ? List.of() : shoppingListRepository.addNew(listKey(chatId), items);
        int added = newItems.size();
        if (added > 0 && partnerRepository.partnerOf(chatId) != null) {
            ctx.pendingPartnerNotice.computeIfAbsent(chatId, k -> new ArrayList<>()).addAll(newItems);
        }
        InlineKeyboardButton done = new InlineKeyboardButton(Messages.get(lang, "shop.btn_done"));
        done.setCallbackData(CB_SHOP_DONE);
        SendMessage message = new SendMessage(String.valueOf(chatId),
                Messages.get(lang, added > 0 ? "shop.added_more" : "shop.nothing_new_more", String.valueOf(added)));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(done))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** Sends the list, or edits the message at {@code editMessageId} in place when given. */
    void sendShoppingList(long chatId, Lang lang, Integer editMessageId) {
        sendShoppingList(chatId, lang, editMessageId, null);
    }

    /** As above, with an optional one-line {@code note} (e.g. what was just added) above the list header. */
    void sendShoppingList(long chatId, Lang lang, Integer editMessageId, String note) {
        List<Item> items = shoppingListRepository.itemsOf(listKey(chatId));
        if (items.isEmpty()) {
            InlineKeyboardMarkup addOnly = new InlineKeyboardMarkup(buildListRows(items, lang));
            String empty = Messages.get(lang, "shop.empty");
            if (editMessageId != null) {
                editMessageTextAndMarkup(chatId, editMessageId, empty, addOnly);
                return;
            }
            SendMessage message = new SendMessage(String.valueOf(chatId), empty);
            message.setReplyMarkup(addOnly);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(buildListRows(items, lang));
        String header = (note == null ? "" : note + "\n\n") + (partnerRepository.partnerOf(chatId) == null
                ? Messages.get(lang, "shop.header")
                : Messages.get(lang, "shop.header_shared", ctx.partner.partnerName(chatId)));
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
        InlineKeyboardButton add = new InlineKeyboardButton(Messages.get(lang, "shop.btn_add_item"));
        add.setCallbackData(CB_SHOP_ADD_ITEM);
        InlineKeyboardButton partner = new InlineKeyboardButton(Messages.get(lang, "shop.btn_partner"));
        partner.setCallbackData(CB_PARTNER_MENU);
        rows.add(List.of(add, partner));
        rows.add(List.of(homeButton(lang)));
        return rows;
    }
}
