package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import com.foodbot.social.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareLinkTest {
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
        ctx.botUsername = "foodbot";
        ctx.languageRepository.set(5L, Lang.EN);
    }

    private Food addFood(String id, Long owner) {
        Food food = new Food(id, "Soup", 20, "MainCourse", List.of("rice"), owner, owner, null, Lang.EN);
        ctx.foodRepository.add(food);
        return food;
    }

    private String lastText() {
        return ((SendMessage) sent.get(sent.size() - 1)).getText();
    }

    private List<InlineKeyboardButton> firstRow() {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) ((SendMessage) sent.get(sent.size() - 1)).getReplyMarkup();
        return markup.getKeyboard().get(0);
    }

    @Test
    void parsesRecipeIdFromStartPayload() {
        assertEquals("abc-123", FoodBot.sharedRecipeId("/start recipe_abc-123"));
        assertEquals("abc", FoodBot.sharedRecipeId("/start@foodbot recipe_abc"));
        assertNull(FoodBot.sharedRecipeId("/start"));
        assertNull(FoodBot.sharedRecipeId("/start recipe_"));
        assertNull(FoodBot.sharedRecipeId("/start other_abc"));
        assertNull(FoodBot.sharedRecipeId("/search recipe_abc"));
    }

    @Test
    void globalRecipeOpensForAnyone() {
        addFood("g1", null);
        ctx.browse.openSharedRecipe(5L, Lang.EN, "g1");
        assertTrue(lastText().contains("Soup"));
    }

    @Test
    void personalRecipeOpensForOwnerAndFollowersOnly() {
        addFood("p1", 7L);
        ctx.followRepository.follow(5L, 7L);
        ctx.browse.openSharedRecipe(5L, Lang.EN, "p1");
        assertTrue(lastText().contains("Soup"));

        sent.clear();
        ctx.browse.openSharedRecipe(8L, Lang.EN, "p1");
        assertFalse(lastText().contains("Soup"));

        sent.clear();
        ctx.browse.openSharedRecipe(7L, Lang.EN, "p1");
        assertTrue(lastText().contains("Soup"));
    }

    @Test
    void missingRecipeGetsSameReplyAsHiddenOne() {
        addFood("p1", 7L);
        ctx.browse.openSharedRecipe(8L, Lang.EN, "p1");
        String hidden = lastText();
        ctx.browse.openSharedRecipe(8L, Lang.EN, "nope");
        assertEquals(hidden, lastText());
    }

    @Test
    void detailRowHasShareUrlButtonWithDeepLink() {
        Food food = addFood("g1", null);
        ctx.browse.sendFoodDetailMessage(5L, Lang.EN, SCOPE_ALL, 0, food);
        String url = firstRow().stream().map(InlineKeyboardButton::getUrl)
                .filter(u -> u != null).findFirst().orElseThrow();
        assertTrue(url.startsWith("https://t.me/share/url?url="));
        assertTrue(url.contains("foodbot%3Fstart%3Drecipe_g1"));
    }

    @Test
    void noShareButtonWithoutBotUsername() {
        ctx.botUsername = null;
        Food food = addFood("g1", null);
        ctx.browse.sendFoodDetailMessage(5L, Lang.EN, SCOPE_ALL, 0, food);
        assertTrue(firstRow().stream().allMatch(b -> b.getUrl() == null));
    }

    private List<String> callbacks() {
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) ((SendMessage) sent.get(sent.size() - 1)).getReplyMarkup();
        List<String> result = new ArrayList<>();
        markup.getKeyboard().forEach(row -> row.forEach(b -> result.add(b.getCallbackData())));
        return result;
    }

    @Test
    void parsesProfileIdFromStartPayload() {
        assertEquals(42L, FoodBot.sharedProfileId("/start user_42"));
        assertEquals(42L, FoodBot.sharedProfileId("/start@foodbot user_42"));
        assertNull(FoodBot.sharedProfileId("/start user_"));
        assertNull(FoodBot.sharedProfileId("/start user_abc"));
        assertNull(FoodBot.sharedProfileId("/start recipe_42"));
        assertNull(FoodBot.sharedProfileId("/start"));
    }

    @Test
    void profileCardOffersFollowAndShareToStrangers() {
        ctx.languageRepository.set(7L, Lang.EN);
        addFood("p1", 7L);
        ctx.follow.sendProfile(5L, Lang.EN, 7L, null);
        assertTrue(lastText().contains("Recipes: 1"));
        assertFalse(lastText().contains("Soup"), "recipe names are only for followers");
        assertTrue(lastText().contains(Messages.get(Lang.EN, "profile.features")));
        assertTrue(lastText().contains("shopping list"), "card briefly explains the features");
        assertTrue(callbacks().contains(BotConstants.CB_PROFILE_FOLLOW + 7L));
        InlineKeyboardMarkup markup = (InlineKeyboardMarkup) ((SendMessage) sent.get(sent.size() - 1)).getReplyMarkup();
        String url = markup.getKeyboard().stream().flatMap(List::stream).map(InlineKeyboardButton::getUrl)
                .filter(u -> u != null).findFirst().orElseThrow();
        assertTrue(url.contains("foodbot%3Fstart%3Duser_7"));
    }

    @Test
    void profileCardOfFollowedUserShowsRecipesAndUnfollow() {
        ctx.languageRepository.set(7L, Lang.EN);
        addFood("p1", 7L);
        ctx.followRepository.follow(5L, 7L);
        ctx.follow.sendProfile(5L, Lang.EN, 7L, null);
        assertTrue(lastText().contains("Soup"));
        assertTrue(lastText().contains("⏱"), "recipe previews show prep time");
        assertTrue(callbacks().contains(BotConstants.CB_PROFILE_UNFOLLOW + 7L));
        assertFalse(callbacks().contains(BotConstants.CB_PROFILE_FOLLOW + 7L));
    }

    @Test
    void profileOfUnknownUserIsUnavailable() {
        ctx.follow.sendProfile(5L, Lang.EN, 12345L, null);
        assertEquals(Messages.get(Lang.EN, "profile.unavailable"), lastText());
    }

    @Test
    void ownProfileHasNoFollowButton() {
        ctx.follow.sendProfile(5L, Lang.EN, 5L, null);
        assertTrue(callbacks().stream().noneMatch(c -> c != null && c.startsWith("frp")));
    }

    private static final String SCOPE_ALL = BotConstants.SCOPE_ALL;
}
