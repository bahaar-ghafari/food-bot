package com.foodbot.bot;

import com.foodbot.food.Food;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.FoodSearch;
import com.foodbot.food.IngredientTranslations;
import com.foodbot.food.SearchResultContext;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

import static com.foodbot.bot.BotConstants.*;

class SearchHandler extends FlowHandler {
    SearchHandler(BotContext ctx) {
        super(ctx);
    }

    void handleSearchQuery(long chatId, String text, Lang lang) {
        if (!matchesLanguage(text, lang)) {
            send(chatId, Messages.get(lang, "lang.wrong_script"));
            return;
        }
        awaitingSearch.remove(chatId);
        List<Food> visible = foodRepository.findVisibleTo(chatId, lang);
        List<Food> matches = publicLast(chatId, FoodSearch.search(visible, text));
        if (!matches.isEmpty()) {
            presentSearchResults(chatId, lang, matches, text);
            return;
        }

        Optional<Food> fuzzy = FoodSearch.fuzzyNameMatch(visible, text);
        if (fuzzy.isPresent()) {
            pendingSearchSuggestion.put(chatId, fuzzy.get());
            pendingSearchQuery.put(chatId, text);
            InlineKeyboardButton yes = new InlineKeyboardButton(Messages.get(lang, "search.fuzzy_yes"));
            yes.setCallbackData(CB_SEARCH_FUZZY_YES);
            InlineKeyboardButton no = new InlineKeyboardButton(Messages.get(lang, "search.fuzzy_no"));
            no.setCallbackData(CB_SEARCH_FUZZY_NO);
            String suggestedName = FoodNameTranslations.translate(fuzzy.get().getName(), lang);
            SendMessage message = new SendMessage(String.valueOf(chatId),
                    Messages.get(lang, "search.fuzzy_suggestion", suggestedName));
            message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(yes, no))));
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        tryOtherLanguageSuggestion(chatId, lang, text);
    }

    void presentSearchResults(long chatId, Lang lang, List<Food> matches, String query) {
        lastSearchResults.put(chatId, new SearchResultContext(matches, query));
        if (matches.size() == 1) {
            ctx.browse.sendFoodDetailMessage(chatId, lang, SCOPE_SEARCH, 0, matches.get(0));
        } else {
            ctx.browse.sendFoodListPage(chatId, lang, SCOPE_SEARCH, 0, null);
        }
    }

    void tryOtherLanguageSuggestion(long chatId, Lang lang, String text) {
        Lang other = lang == Lang.EN ? Lang.FA : Lang.EN;
        String translated = translateQueryToOtherLanguage(text, other);
        List<Food> otherVisible = foodRepository.findVisibleTo(chatId, other);
        List<Food> otherMatches = publicLast(chatId, FoodSearch.search(otherVisible, translated));
        if (otherMatches.isEmpty()) {
            awaitingSearch.add(chatId);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "search.no_results", text));
            return;
        }
        Food suggestion = otherMatches.get(0);
        pendingSearchSuggestion.put(chatId, suggestion);
        pendingSearchQuery.put(chatId, text);
        InlineKeyboardButton yes = new InlineKeyboardButton(Messages.get(lang, "search.otherlang_yes"));
        yes.setCallbackData(CB_SEARCH_OTHERLANG_YES);
        InlineKeyboardButton no = new InlineKeyboardButton(Messages.get(lang, "search.otherlang_no"));
        no.setCallbackData(CB_SEARCH_OTHERLANG_NO);
        String otherLangName = Messages.get(lang, other == Lang.FA ? "lang.name.fa" : "lang.name.en");
        SendMessage message = new SendMessage(String.valueOf(chatId),
                Messages.get(lang, "search.otherlang_suggestion", otherLangName));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(yes, no))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    String translateQueryToOtherLanguage(String query, Lang otherLang) {
        String nameTranslated = FoodNameTranslations.translate(query, otherLang);
        if (!nameTranslated.equalsIgnoreCase(query)) {
            return nameTranslated;
        }
        return IngredientTranslations.translate(query, otherLang);
    }

    void handleSearchFuzzyYesCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        Food food = pendingSearchSuggestion.remove(chatId);
        String query = pendingSearchQuery.remove(chatId);
        if (food == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        lastSearchResults.put(chatId, new SearchResultContext(List.of(food), query == null ? "" : query));
        sendFoodPhoto(chatId, food);
        String text = ctx.browse.formatFoodDetail(food, lang);
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(), text,
                ctx.browse.buildDetailMarkup(chatId, lang, SCOPE_SEARCH, 0, food));
    }

    void handleSearchFuzzyNoCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        pendingSearchSuggestion.remove(chatId);
        String query = pendingSearchQuery.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        clearInlineKeyboard(chatId, callbackQuery.getMessage().getMessageId());
        tryOtherLanguageSuggestion(chatId, lang, query == null ? "" : query);
    }

    void handleSearchOtherLangYesCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        Food food = pendingSearchSuggestion.remove(chatId);
        String query = pendingSearchQuery.remove(chatId);
        if (food == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        lastSearchResults.put(chatId, new SearchResultContext(List.of(food), query == null ? "" : query));
        sendFoodPhoto(chatId, food);
        String text = ctx.browse.formatFoodDetail(food, food.getLanguage());
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(), text,
                ctx.browse.buildDetailMarkup(chatId, lang, SCOPE_SEARCH, 0, food));
    }

    void handleSearchOtherLangNoCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        pendingSearchSuggestion.remove(chatId);
        String query = pendingSearchQuery.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        clearInlineKeyboard(chatId, callbackQuery.getMessage().getMessageId());
        awaitingSearch.add(chatId);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "search.no_results", query == null ? "" : query));
    }

    void sendSearchPrompt(long chatId, Lang lang) {
        sendWithHomeButton(chatId, lang, Messages.get(lang, "search.ask"));
    }
}
