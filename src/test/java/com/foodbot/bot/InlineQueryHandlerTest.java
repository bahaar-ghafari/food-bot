package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.social.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineQueryHandlerTest {
    private final List<BotApiMethod<?>> sent = new ArrayList<>();
    private BotContext ctx;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        ApiExecutor recorder = new ApiExecutor() {
            @Override
            public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
                sent.add(method);
                return null;
            }
        };
        ctx = new BotContext(recorder, 1L, 99L,
                new FoodRepository(dir.resolve("foods.json")),
                new LanguageRepository(dir.resolve("languages.json")),
                new FollowRepository(dir.resolve("follows.json")),
                new com.foodbot.social.UsernameRepository(dir.resolve("usernames.json")),
                new FavoriteRepository(dir.resolve("favorites.json")),
                new ShoppingListRepository(dir.resolve("shopping.json")),
                new AiSuggestionService(null), new MealDbSuggestionService());
        ctx.languageRepository.set(5L, Lang.EN);
    }

    private void addFood(String id, String name, Long owner) {
        ctx.foodRepository.add(new Food(id, name, 20, "MainCourse", List.of("rice"), owner, owner, null, Lang.EN));
    }

    private static String titleOf(InlineQueryResult result) {
        return ((InlineQueryResultArticle) result).getTitle();
    }

    @Test
    void searchMatchesGlobalAndOwnFoodsButNotOtherUsersPersonalOnes() {
        addFood("g1", "Rice Pudding", null);
        addFood("m1", "Rice Salad", 5L);
        addFood("o1", "Rice Secret", 6L);

        List<String> titles = ctx.inline.buildResults(5L, "rice").stream()
                .map(InlineQueryHandlerTest::titleOf).toList();

        assertEquals(2, titles.size());
        assertTrue(titles.contains("Rice Pudding"));
        assertTrue(titles.contains("Rice Salad"));
    }

    @Test
    void emptyQueryListsVisibleFoodsCappedAtTelegramsLimit() {
        for (int i = 0; i < InlineQueryHandler.MAX_RESULTS + 5; i++) {
            addFood("f" + i, "Dish " + i, null);
        }

        assertEquals(InlineQueryHandler.MAX_RESULTS, ctx.inline.buildResults(5L, "  ").size());
    }

    @Test
    void resultCarriesTheFullRecipeAsTheMessageToPost() {
        addFood("g1", "Rice Pudding", null);

        InlineQueryResultArticle article = (InlineQueryResultArticle) ctx.inline.buildResults(5L, "pudding").get(0);

        assertEquals("g1", article.getId());
        String text = ((org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent
                .InputTextMessageContent) article.getInputMessageContent()).getMessageText();
        assertTrue(text.contains("Rice Pudding"));
        assertTrue(text.contains("rice"));
    }

    @Test
    void answerIsPersonalAndAddressedToTheQuery() {
        addFood("g1", "Rice Pudding", null);
        InlineQuery query = new InlineQuery();
        query.setId("q1");
        query.setQuery("rice");
        User from = new User();
        from.setId(5L);
        query.setFrom(from);

        ctx.inline.handleInlineQuery(query);

        AnswerInlineQuery answer = (AnswerInlineQuery) sent.get(0);
        assertEquals("q1", answer.getInlineQueryId());
        assertTrue(answer.getIsPersonal());
        assertEquals(1, answer.getResults().size());
    }
}
