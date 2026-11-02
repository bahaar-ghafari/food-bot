package com.foodbot.bot;

import com.foodbot.food.Food;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.FoodSearch;
import com.foodbot.lang.Lang;
import com.foodbot.lang.TimeFormat;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

/** Answers {@code @bot query} so a recipe can be searched and posted into any chat. */
class InlineQueryHandler extends FlowHandler {
    /** Telegram caps an inline answer at 50 results. */
    static final int MAX_RESULTS = 50;

    InlineQueryHandler(BotContext ctx) {
        super(ctx);
    }

    void handleInlineQuery(InlineQuery inlineQuery) {
        long userId = inlineQuery.getFrom().getId();
        AnswerInlineQuery answer = new AnswerInlineQuery();
        answer.setInlineQueryId(inlineQuery.getId());
        answer.setResults(buildResults(userId, inlineQuery.getQuery()));
        // results depend on who asks (their personal list), so they must not be shared between users
        answer.setIsPersonal(true);
        answer.setCacheTime(0);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Only foods the asker could already open in the bot (the global list and their own) are offered, in
     * their language, so inline mode never exposes another user's personal or followed-only recipes.
     */
    List<InlineQueryResult> buildResults(long userId, String query) {
        Lang lang = lang(userId);
        List<Food> visible = foodRepository.findVisibleTo(userId, lang);
        String trimmed = query == null ? "" : query.trim();
        List<Food> matches = publicLast(userId, trimmed.isEmpty() ? visible : FoodSearch.search(visible, trimmed));
        List<InlineQueryResult> results = new ArrayList<>();
        for (Food food : matches) {
            if (results.size() >= MAX_RESULTS) {
                break;
            }
            results.add(toResult(food, lang));
        }
        return results;
    }

    private InlineQueryResult toResult(Food food, Lang lang) {
        String title = FoodNameTranslations.translate(food.getName(), lang);
        InputTextMessageContent content = new InputTextMessageContent(ctx.browse.formatFoodDetail(food, lang));
        InlineQueryResultArticle article = new InlineQueryResultArticle();
        article.setId(food.getId());
        article.setTitle(title);
        article.setDescription(categoryLabel(food.getCategory(), lang) + " · "
                + TimeFormat.format(food.getPrepTimeMinutes(), lang));
        article.setInputMessageContent(content);
        return article;
    }
}
