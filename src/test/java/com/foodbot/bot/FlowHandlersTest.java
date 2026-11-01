package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.social.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowHandlersTest {
    private static final long ADMIN = 1L;
    private static final long FEEDBACK_CHAT = 99L;

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
        ctx = new BotContext(recorder, ADMIN, FEEDBACK_CHAT,
                new FoodRepository(dir.resolve("foods.json")),
                new LanguageRepository(dir.resolve("languages.json")),
                new FollowRepository(dir.resolve("follows.json")),
                new FavoriteRepository(dir.resolve("favorites.json")),
                new AiSuggestionService(null), new MealDbSuggestionService());
    }

    private List<SendMessage> sentMessages() {
        return sent.stream().filter(SendMessage.class::isInstance).map(SendMessage.class::cast).toList();
    }

    @Test
    void feedbackIsForwardedToTheFeedbackChatAndAcknowledged() {
        ctx.awaitingFeedback.add(5L);

        ctx.feedback.handleFeedbackText(5L, "great bot", Lang.EN, null);

        assertFalse(ctx.awaitingFeedback.contains(5L));
        List<SendMessage> messages = sentMessages();
        assertEquals(2, messages.size());
        assertEquals(String.valueOf(FEEDBACK_CHAT), messages.get(0).getChatId());
        assertTrue(messages.get(0).getText().contains("great bot"));
        assertEquals("5", messages.get(1).getChatId());
    }

    @Test
    void searchRejectsQueryInTheWrongScriptAndKeepsWaiting() {
        ctx.awaitingSearch.add(5L);

        ctx.search.handleSearchQuery(5L, "قورمه", Lang.EN);

        assertTrue(ctx.awaitingSearch.contains(5L));
        assertEquals(1, sentMessages().size());
    }

    @Test
    void searchWithNoMatchesKeepsAwaitingAndSaysSo() {
        ctx.search.handleSearchQuery(5L, "nothingmatchesthis", Lang.EN);

        assertTrue(ctx.awaitingSearch.contains(5L));
        assertNull(ctx.lastSearchResults.get(5L));
        assertEquals(1, sentMessages().size());
    }

    @Test
    void superAdminAndOwnerCanModifyButOthersCannot() {
        assertTrue(ctx.ui.isSuperAdmin(ADMIN));
        assertFalse(ctx.ui.isSuperAdmin(2L));
    }

    @Test
    void languageDefaultsToPersianUntilChosen() {
        assertEquals(Lang.FA, ctx.ui.lang(7L));
        ctx.languageRepository.set(7L, Lang.EN);
        assertEquals(Lang.EN, ctx.ui.lang(7L));
    }

    @Test
    void matchesLanguageChecksScript() {
        assertTrue(ctx.ui.matchesLanguage("soup", Lang.EN));
        assertFalse(ctx.ui.matchesLanguage("سوپ", Lang.EN));
        assertTrue(ctx.ui.matchesLanguage("سوپ", Lang.FA));
        assertFalse(ctx.ui.matchesLanguage("soup", Lang.FA));
    }

    @Test
    void parseMinutesReturnsNullForNonNumbers() {
        assertEquals(45, ctx.ui.parseMinutes(" 45 "));
        assertNull(ctx.ui.parseMinutes("soon"));
    }

    @Test
    void parsePagedRefSplitsKeyPageAndId() {
        FlowHandler.PagedRef ref = ctx.ui.parsePagedRef("mine:3:abc-123");
        assertEquals("mine", ref.key());
        assertEquals(3, ref.page());
        assertEquals("abc-123", ref.id());
    }

    private Food addFood(String id, Long owner) {
        Food food = new Food(id, "Soup " + id, 20, "MainCourse", List.of("water"), owner, owner, null, Lang.EN);
        ctx.foodRepository.add(food);
        return food;
    }

    @Test
    void detailRowShowsAnEmptyStarUntilFavorited() {
        Food food = addFood("f1", null);

        assertEquals("☆", ctx.browse.buildDetailRow(5L, Lang.EN, "global", 0, food).get(1).getText());
        ctx.favoriteRepository.toggle(5L, "f1");
        assertEquals("⭐", ctx.browse.buildDetailRow(5L, Lang.EN, "global", 0, food).get(1).getText());
        assertEquals("ffav:global:0:f1", ctx.browse.buildDetailRow(5L, Lang.EN, "global", 0, food).get(1).getCallbackData());
    }

    @Test
    void favoriteFoodsKeepsOnlyWhatTheUserCanStillSee() {
        addFood("global", null);
        addFood("mine", 5L);
        addFood("theirs", 6L);
        addFood("followed", 7L);
        ctx.followRepository.follow(5L, 7L);
        for (String id : List.of("global", "mine", "theirs", "followed", "deleted")) {
            ctx.favoriteRepository.toggle(5L, id);
        }

        List<String> ids = ctx.browse.favoriteFoods(5L, Lang.EN).stream().map(Food::getId).toList();

        assertEquals(List.of("global", "mine", "followed"), ids);
        assertTrue(ctx.browse.favoriteFoods(5L, Lang.FA).isEmpty());
    }

    @Test
    void emptyFavoritesListSaysSoInsteadOfGenericNothingHere() {
        ctx.browse.sendFoodListPage(5L, Lang.EN, "fav", 0, null);

        assertEquals(1, sentMessages().size());
        assertTrue(sentMessages().get(0).getText().contains("No favorites yet"));
    }

    @Test
    void favoritesListShowsStarredFoods() {
        addFood("f1", null);
        ctx.favoriteRepository.toggle(5L, "f1");

        ctx.browse.sendFoodListPage(5L, Lang.EN, "fav", 0, null);

        SendMessage message = sentMessages().get(0);
        assertTrue(message.getText().startsWith("⭐"));
        assertTrue(message.getReplyMarkup().toString().contains("Soup f1"));
    }

    private CallbackQuery callback() {
        Message message = new Message();
        message.setMessageId(42);
        CallbackQuery query = new CallbackQuery();
        query.setId("cb1");
        query.setMessage(message);
        return query;
    }

    @Test
    void deleteStartAsksForConfirmationAndKeepsTheFood() {
        addFood("f1", 5L);
        ctx.languageRepository.set(5L, Lang.EN);

        ctx.editFood.handleDeleteStartCallback(callback(), 5L, "fd:mine:0:f1");

        assertTrue(ctx.foodRepository.findById("f1").isPresent());
        EditMessageText edit = sent.stream().filter(EditMessageText.class::isInstance)
                .map(EditMessageText.class::cast).findFirst().orElseThrow();
        assertTrue(edit.getText().contains("permanent"));
        String buttons = ((InlineKeyboardMarkup) edit.getReplyMarkup()).getKeyboard().toString();
        assertTrue(buttons.contains("fdy:f1"));
        assertTrue(buttons.contains("fdn:mine:0:f1"));
    }

    @Test
    void deleteConfirmYesRemovesTheFood() {
        addFood("f1", 5L);

        ctx.editFood.handleDeleteConfirmYesCallback(callback(), 5L, "fdy:f1");

        assertTrue(ctx.foodRepository.findById("f1").isEmpty());
    }

    @Test
    void deleteConfirmNoKeepsTheFood() {
        addFood("f1", 5L);

        ctx.editFood.handleDeleteConfirmNoCallback(callback(), 5L, "fdn:mine:0:f1");

        assertTrue(ctx.foodRepository.findById("f1").isPresent());
    }

    @Test
    void nonOwnerCannotStartOrConfirmDelete() {
        addFood("f1", 5L);

        ctx.editFood.handleDeleteStartCallback(callback(), 6L, "fd:mine:0:f1");
        ctx.editFood.handleDeleteConfirmYesCallback(callback(), 6L, "fdy:f1");

        assertTrue(ctx.foodRepository.findById("f1").isPresent());
        assertTrue(sent.stream().noneMatch(EditMessageText.class::isInstance));
    }
}
