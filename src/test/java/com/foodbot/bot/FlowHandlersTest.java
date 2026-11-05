package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.EditFoodSession;
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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.foodbot.bot.BotConstants.SCOPE_ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowHandlersTest {
    private static final long ADMIN = 1L;
    private static final long FEEDBACK_CHAT = 99L;

    private final List<BotApiMethod<?>> sent = new ArrayList<>();
    private final List<SendPhoto> photos = new ArrayList<>();
    private BotContext ctx;

    @BeforeEach
    void setUp(@TempDir Path dir) {
        ApiExecutor recorder = new ApiExecutor() {
            @Override
            public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
                sent.add(method);
                return null;
            }

            @Override
            public Message sendPhoto(SendPhoto photo) {
                photos.add(photo);
                return null;
            }
        };
        ctx = new BotContext(recorder, ADMIN, FEEDBACK_CHAT,
                new FoodRepository(dir.resolve("foods.json")),
                new LanguageRepository(dir.resolve("languages.json")),
                new FollowRepository(dir.resolve("follows.json")),
                new com.foodbot.social.UsernameRepository(dir.resolve("usernames.json")),
                new FavoriteRepository(dir.resolve("favorites.json")),
                new ShoppingListRepository(dir.resolve("shopping.json")),
                new com.foodbot.social.PartnerRepository(dir.resolve("partners.json")),
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
    void foodDetailScalesNumericAmountsOnly() {
        Food food = new Food("f9", "Omelette", 10, "Breakfast", List.of("egg", "salt"), null, null, null, Lang.EN,
                Map.of("egg", "2", "salt", "a pinch"));

        String scaled = ctx.browse.formatFoodDetail(food, Lang.EN, 3);

        assertTrue(scaled.contains("(6)"));
        assertTrue(scaled.contains("(a pinch)"));
        assertTrue(ctx.browse.formatFoodDetail(food, Lang.EN).contains("(2)"));
    }

    @Test
    void scaleRowMarksTheActiveFactorAndEncodesTheRecipe() {
        Food food = addFood("f1", null);

        var row = ctx.browse.buildScaleRow("global", 2, food, 2.0);

        assertEquals(List.of("½×", "1×", "• 2×", "3×"), row.stream().map(b -> b.getText()).toList());
        assertEquals("fsc:0.5:global:2:f1", row.get(0).getCallbackData());
        assertEquals("fsc:3:global:2:f1", row.get(3).getCallbackData());
    }

    @Test
    void scaleRowIsHiddenWhenNoAmountIsScalable() {
        Food plain = new Food("f7", "Salad", 5, "Lunch", List.of("salt"), null, null, null, Lang.EN,
                Map.of("salt", "a pinch"));
        Food none = addFood("f8", null);
        Food scalable = new Food("f9", "Omelette", 10, "Breakfast", List.of("egg"), null, null, null, Lang.EN,
                Map.of("egg", "2"));

        assertEquals(1, ctx.browse.buildDetailMarkup(5L, Lang.EN, "global", 0, plain).getKeyboard().size());
        assertEquals(1, ctx.browse.buildDetailMarkup(5L, Lang.EN, "global", 0, none).getKeyboard().size());
        assertEquals(2, ctx.browse.buildDetailMarkup(5L, Lang.EN, "global", 0, scalable).getKeyboard().size());
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

    private Food addFoodWith(String id, List<String> ingredients) {
        Food food = new Food(id, "Omelette " + id, 10, "Breakfast", ingredients, null, null, null, Lang.EN);
        ctx.foodRepository.add(food);
        return food;
    }

    @Test
    void addMissingPutsOnlyIngredientsTheUserLacksOnTheList() {
        addFoodWith("f1", List.of("egg", "milk", "flour"));
        ctx.lastCookResults.put(5L, new CookResultContext(60, java.util.Set.of("egg"), true, null));

        ctx.shopping.handleAddMissingCallback(callback(), 5L, "shadd:f1");

        assertEquals(List.of("milk", "flour"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());
    }

    @Test
    void typedShoppingItemsKeepAddModeOpenUntilDone() {
        ctx.awaitingShoppingItem.add(5L);

        ctx.shopping.handleItemText(5L, "milk, eggs", Lang.EN);
        ctx.shopping.handleItemText(5L, "bread", Lang.EN);

        assertTrue(ctx.awaitingShoppingItem.contains(5L));
        assertEquals(List.of("milk", "eggs", "bread"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());

        ctx.shopping.handleListCallback(callback(), 5L, "shl:done");

        assertTrue(!ctx.awaitingShoppingItem.contains(5L));
    }

    @Test
    void addMissingWithoutACookContextChangesNothing() {
        addFoodWith("f1", List.of("egg"));

        ctx.shopping.handleAddMissingCallback(callback(), 5L, "shadd:f1");

        assertTrue(ctx.shoppingListRepository.itemsOf(5L).isEmpty());
    }

    @Test
    void cookDetailOffersShoppingButtonsOnlyWhenSomethingIsMissing() {
        Food food = addFoodWith("f1", List.of("egg", "milk"));
        InlineKeyboardButton back = new InlineKeyboardButton("back");

        var missing = ctx.cook.buildCookDetailRows(food,
                new CookResultContext(60, java.util.Set.of("egg"), true, null), Lang.EN, back);
        var complete = ctx.cook.buildCookDetailRows(food,
                new CookResultContext(60, java.util.Set.of("egg", "milk"), true, null), Lang.EN, back);

        assertEquals("shadd:f1", missing.get(0).get(0).getCallbackData());
        assertEquals("shl:open", missing.get(1).get(0).getCallbackData());
        assertEquals(3, missing.size());
        assertEquals(1, complete.size());
    }

    @Test
    void togglingAndClearingUpdateTheListAndOfferClearOnlyWhenSomethingIsChecked() {
        ctx.languageRepository.set(5L, Lang.EN);
        ctx.shoppingListRepository.addAll(5L, List.of("egg", "milk"));
        assertEquals(4, ctx.shopping.buildListRows(ctx.shoppingListRepository.itemsOf(5L), Lang.EN).size());

        ctx.shopping.handleListCallback(callback(), 5L, "shl:t:0");
        var rows = ctx.shopping.buildListRows(ctx.shoppingListRepository.itemsOf(5L), Lang.EN);
        assertTrue(rows.get(0).get(0).getText().startsWith("☑️"));
        assertEquals("shl:clear", rows.get(2).get(0).getCallbackData());
        assertEquals("shl:add", rows.get(3).get(0).getCallbackData());
        assertEquals("home", rows.get(4).get(0).getCallbackData());

        ctx.shopping.handleListCallback(callback(), 5L, "shl:clear");
        assertEquals(List.of("milk"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());
    }

    @Test
    void typedItemsAreAddedToTheShoppingList() {
        ctx.awaitingShoppingItem.add(5L);

        ctx.shopping.handleItemText(5L, "Milk, eggs\nmilk", Lang.EN);

        assertTrue(ctx.awaitingShoppingItem.contains(5L));
        assertEquals(List.of("milk", "eggs"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());
    }

    @Test
    void typedItemsReplyWithASingleMessage() {
        ctx.shopping.handleItemText(5L, "milk", Lang.EN);

        assertEquals(1, sentMessages().size());
    }

    @Test
    void pickingAScopeRedrawsTheTappedMessageInsteadOfSendingANewOne() {
        ctx.languageRepository.set(5L, Lang.EN);
        addFood("g1", null);

        ctx.browse.handleViewFoodsCallback(callback(), 5L, BotConstants.CB_VIEW_FOODS_GLOBAL);

        assertTrue(sentMessages().isEmpty());
        assertEquals(42, sent.stream().filter(EditMessageText.class::isInstance)
                .map(EditMessageText.class::cast).findFirst().orElseThrow().getMessageId());
    }

    @Test
    void emptyScopeKeepsTheMenuMessageAndOffersBack() {
        ctx.languageRepository.set(5L, Lang.EN);

        ctx.browse.handleViewFoodsCallback(callback(), 5L, BotConstants.CB_VIEW_FOODS_FAVORITES);

        assertTrue(sentMessages().isEmpty());
        EditMessageText edit = sent.stream().filter(EditMessageText.class::isInstance)
                .map(EditMessageText.class::cast).findFirst().orElseThrow();
        assertTrue(edit.getText().contains("No favorites yet"));
        assertTrue(edit.getReplyMarkup().toString().contains(BotConstants.CB_VIEW_FOODS_MENU));
    }

    @Test
    void cookTimePresetMovesOnByEditingTheTappedMessage() {
        ctx.languageRepository.set(5L, Lang.EN);
        com.foodbot.food.CookSession session = new com.foodbot.food.CookSession();
        ctx.cookSessions.put(5L, session);

        ctx.cook.handleCookTimePresetCallback(callback(), 5L, BotConstants.CB_COOK_TIME_PRESET + "30");

        assertTrue(sentMessages().isEmpty());
        assertEquals(42, session.getKeyboardMessageId());
    }

    @Test
    void addItemButtonStartsAwaitingAnItem() {
        ctx.shopping.handleListCallback(callback(), 5L, "shl:add");

        assertTrue(ctx.awaitingShoppingItem.contains(5L));
    }

    @Test
    void emptyShoppingListSaysSo() {
        ctx.shopping.sendShoppingList(5L, Lang.EN, null);

        assertTrue(sentMessages().get(0).getText().contains("empty"));
    }

    @Test
    void photoIsSentBeforeTheDetailOnlyWhenTheFoodHasOne() {
        Food plain = addFood("p1", null);
        Food pictured = addFood("p2", null).withPhoto("FILE123");

        ctx.browse.sendFoodDetailMessage(5L, Lang.EN, "all", 0, plain);
        assertTrue(photos.isEmpty());

        ctx.browse.sendFoodDetailMessage(5L, Lang.EN, "all", 0, pictured);
        assertEquals(1, photos.size());
        assertEquals("5", photos.get(0).getChatId());
        assertEquals("FILE123", photos.get(0).getPhoto().getAttachName().replace("attach://", ""));
    }

    @Test
    void addFoodPhotoStepStoresTheFileIdAndMovesToReview() {
        AddFoodSession session = new AddFoodSession();
        session.setName("Soup");
        session.setCategory("MainCourse");
        session.setStep(AddFoodSession.Step.AWAITING_PHOTO);
        session.setKeyboardMessageId(42); // the recorder returns no Message, so edit in place
        ctx.addFoodSessions.put(5L, session);

        ctx.addFood.handleAddFoodPhoto(5L, "FILE9", Lang.EN);

        assertEquals("FILE9", session.getPhotoFileId());
        assertEquals(AddFoodSession.Step.REVIEW, session.getStep());
    }

    @Test
    void addFoodPhotoOutsideThePhotoStepIsRejected() {
        AddFoodSession session = new AddFoodSession();
        session.setStep(AddFoodSession.Step.AWAITING_NAME);
        ctx.addFoodSessions.put(5L, session);

        ctx.addFood.handleAddFoodPhoto(5L, "FILE9", Lang.EN);

        assertNull(session.getPhotoFileId());
        assertEquals(AddFoodSession.Step.AWAITING_NAME, session.getStep());
        assertEquals(1, sentMessages().size());
    }

    @Test
    void savedFoodKeepsThePhotoAndFollowersGetItToo() {
        ctx.languageRepository.set(6L, Lang.EN);
        ctx.followRepository.follow(6L, 5L);
        AddFoodSession session = new AddFoodSession();
        session.setName("Soup");
        session.setPrepTimeMinutes(10);
        session.setCategory("MainCourse");
        session.getSelectedIngredients().add("water");
        session.setPhotoFileId("FILE7");

        ctx.addFood.finalizeNewFood(5L, session, Lang.EN, null);

        Food saved = ctx.foodRepository.findCreatedBy(5L, Lang.EN).get(0);
        assertEquals("FILE7", saved.getPhotoFileId());
        assertEquals(1, photos.size());
        assertEquals("6", photos.get(0).getChatId());
    }

    private AddFoodSession soupSession() {
        AddFoodSession session = new AddFoodSession();
        session.setName("Soup");
        session.setPrepTimeMinutes(10);
        session.setCategory("MainCourse");
        session.getSelectedIngredients().add("water");
        return session;
    }

    @Test
    void mutedFollowerGetsNoNotification() {
        ctx.languageRepository.set(6L, Lang.EN);
        ctx.followRepository.follow(6L, 5L);
        ctx.followRepository.setMuted(6L, 5L, true);

        ctx.addFood.finalizeNewFood(5L, soupSession(), Lang.EN, null);

        assertTrue(sentMessages().stream().noneMatch(m -> m.getChatId().equals("6")));
    }

    @Test
    void digestFollowerGetsOneSummaryInsteadOfInstantMessages() {
        ctx.languageRepository.set(6L, Lang.EN);
        ctx.followRepository.follow(6L, 5L);
        ctx.followRepository.setDigest(6L, true);

        ctx.addFood.finalizeNewFood(5L, soupSession(), Lang.EN, null);
        ctx.addFood.finalizeNewFood(5L, soupSession(), Lang.EN, null);
        assertTrue(sentMessages().stream().noneMatch(m -> m.getChatId().equals("6")));

        ctx.follow.sendDigests();

        List<SendMessage> toFollower = sentMessages().stream().filter(m -> m.getChatId().equals("6")).toList();
        assertEquals(1, toFollower.size());
        assertTrue(toFollower.get(0).getText().contains("2 new recipe"));
        assertTrue(toFollower.get(0).getText().contains("Soup"));

        sent.clear();
        ctx.follow.sendDigests();
        assertTrue(sent.isEmpty());
    }

    private EditFoodSession startEdit(String id, long chat) {
        ctx.languageRepository.set(chat, Lang.EN);
        ctx.editFood.handleEditStartCallback(callback(), chat, "fe:mine:0:" + id);
        return ctx.editSessions.get(chat);
    }

    @Test
    void editStartShowsReviewScreenWithoutChangingAnything() {
        addFood("f1", 5L);

        EditFoodSession session = startEdit("f1", 5L);

        assertEquals(EditFoodSession.Step.REVIEW, session.getStep());
        EditMessageText review = sent.stream().filter(EditMessageText.class::isInstance)
                .map(EditMessageText.class::cast).findFirst().orElseThrow();
        assertTrue(review.getText().contains("Soup f1"));
        String buttons = ((InlineKeyboardMarkup) review.getReplyMarkup()).getKeyboard().toString();
        assertTrue(buttons.contains("fesave"));
        assertTrue(buttons.contains("fef:photo"));
    }

    @Test
    void editingSeveralFieldsIsOnlySavedOnSave() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);

        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:name");
        ctx.editFood.handleEditText(5L, "Stew", Lang.EN);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:time");
        ctx.editFood.handleEditText(5L, "45", Lang.EN);

        assertEquals(EditFoodSession.Step.REVIEW, session.getStep());
        assertEquals("Soup f1", ctx.foodRepository.findById("f1").orElseThrow().getName());

        ctx.editFood.handleEditSaveCallback(callback(), 5L, "fesave");

        Food saved = ctx.foodRepository.findById("f1").orElseThrow();
        assertEquals("Stew", saved.getName());
        assertEquals(45, saved.getPrepTimeMinutes());
        assertFalse(ctx.editSessions.containsKey(5L));
    }

    @Test
    void invalidTimeKeepsEditingTheField() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:time");

        ctx.editFood.handleEditText(5L, "soon", Lang.EN);

        assertEquals(EditFoodSession.Step.EDITING_TIME, session.getStep());
    }

    @Test
    void backFromAFieldReturnsToReviewKeepingTheDraft() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:name");

        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:back");

        assertEquals(EditFoodSession.Step.REVIEW, session.getStep());
        assertEquals("Soup f1", session.getName());
    }

    @Test
    void leavingTheEditorDiscardsTheDraft() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);
        session.setName("Changed");

        ctx.editFood.handleEditExitCallback(callback(), 5L, "feexit");

        assertFalse(ctx.editSessions.containsKey(5L));
        assertEquals("Soup f1", ctx.foodRepository.findById("f1").orElseThrow().getName());
    }

    @Test
    void editingThePhotoAttachesRemovesAndSurvivesOtherEdits() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:photo");
        ctx.editFood.handleEditPhoto(5L, "FILE1", Lang.EN);
        ctx.editFood.handleEditSaveCallback(callback(), 5L, "fesave");
        assertEquals("FILE1", ctx.foodRepository.findById("f1").orElseThrow().getPhotoFileId());

        startEdit("f1", 5L);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:name");
        ctx.editFood.handleEditText(5L, "Stew", Lang.EN);
        ctx.editFood.handleEditSaveCallback(callback(), 5L, "fesave");
        assertEquals("FILE1", ctx.foodRepository.findById("f1").orElseThrow().getPhotoFileId());

        startEdit("f1", 5L);
        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:photo");
        ctx.editFood.handleEditPhotoButtonCallback(callback(), 5L, "fephrm");
        ctx.editFood.handleEditSaveCallback(callback(), 5L, "fesave");
        assertNull(ctx.foodRepository.findById("f1").orElseThrow().getPhotoFileId());
    }

    @Test
    void editPhotoOutsideThePhotoStepIsRejected() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);

        ctx.editFood.handleEditPhoto(5L, "FILE1", Lang.EN);

        assertNull(session.getPhotoFileId());
    }

    @Test
    void editingIngredientsAndRecipeComesBackToReviewNotSave() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);

        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:ingredients");
        assertEquals(EditFoodSession.Step.EDITING_INGREDIENTS, session.getStep());
        ctx.editFood.handleEditIngredientCallback(callback(), 5L, "fei:done");
        assertEquals(EditFoodSession.Step.EDITING_AMOUNTS, session.getStep());
        ctx.editFood.handleEditAmountCallback(callback(), 5L, "feam:done");
        assertEquals(EditFoodSession.Step.REVIEW, session.getStep());

        ctx.editFood.handleEditFieldCallback(callback(), 5L, "fef:recipe");
        ctx.editFood.handleEditText(5L, "Boil it", Lang.EN);
        ctx.editFood.handleEditRecipeDoneCallback(callback(), 5L, "ferdone");

        assertEquals(EditFoodSession.Step.REVIEW, session.getStep());
        assertNull(ctx.foodRepository.findById("f1").orElseThrow().getRecipe());
        ctx.editFood.handleEditSaveCallback(callback(), 5L, "fesave");
        assertTrue(ctx.foodRepository.findById("f1").orElseThrow().getRecipe().contains("Boil it"));
    }

    @Test
    void nonOwnerCannotSaveAnEdit() {
        addFood("f1", 5L);
        EditFoodSession session = startEdit("f1", 5L);
        session.setName("Hijacked");
        ctx.editSessions.put(6L, session);

        ctx.editFood.handleEditSaveCallback(callback(), 6L, "fesave");

        assertEquals("Soup f1", ctx.foodRepository.findById("f1").orElseThrow().getName());
    }

    @Test
    void refreshDropsInProgressStateAndRestoresTheMainMenu() {
        ctx.addFoodSessions.put(5L, new AddFoodSession());
        ctx.awaitingSearch.add(5L);
        ctx.awaitingFeedback.add(5L);
        ctx.pendingSearchQuery.put(5L, "soup");

        ctx.ui.refresh(5L, Lang.EN);

        assertFalse(ctx.addFoodSessions.containsKey(5L));
        assertFalse(ctx.awaitingSearch.contains(5L));
        assertFalse(ctx.awaitingFeedback.contains(5L));
        assertFalse(ctx.pendingSearchQuery.containsKey(5L));
        SendMessage reply = sentMessages().get(sentMessages().size() - 1);
        assertEquals(Messages.get(Lang.EN, "refresh.done"), reply.getText());
        assertNotNull(reply.getReplyMarkup());
    }

    @Test
    void refreshPicksUpFoodsChangedOnDisk(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("shared.json");
        FoodRepository repo = new FoodRepository(file);
        BotContext local = new BotContext(ctx.sender, ADMIN, FEEDBACK_CHAT, repo, ctx.languageRepository,
                ctx.followRepository, ctx.usernameRepository, ctx.favoriteRepository, ctx.shoppingListRepository,
                ctx.partnerRepository, ctx.aiSuggestionService, ctx.mealDbSuggestionService);
        new FoodRepository(file).add(new Food("f9", "Outside", 10, "Dinner", List.of("rice"), 5L, 5L, null,
                Lang.EN, null, null, null, null));
        assertTrue(repo.findById("f9").isEmpty());

        local.ui.refresh(5L, Lang.EN);

        assertTrue(repo.findById("f9").isPresent());
    }

    @Test
    void followPromptOffersTheContactPicker() {
        ctx.follow.sendFollowPrompt(5L, Lang.EN);

        org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup keyboard =
                (org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup)
                        sentMessages().get(0).getReplyMarkup();
        assertNotNull(keyboard.getKeyboard().get(0).get(0).getRequestUser());
    }

    @Test
    void followingByUsernameResolvesTheRememberedUser() {
        ctx.languageRepository.set(5L, Lang.EN);
        ctx.languageRepository.set(6L, Lang.EN);
        ctx.usernameRepository.record(6L, "Chef_Anna");
        ctx.awaitingFollowId.add(5L);

        ctx.follow.handleFollowId(5L, "@chef_anna", Lang.EN, null);

        assertTrue(ctx.followRepository.isFollowing(5L, 6L));
        assertFalse(ctx.awaitingFollowId.contains(5L));
    }

    @Test
    void displayNameFallsBackToRememberedNameAndUsernameWhenTelegramLookupFails() {
        assertEquals(Messages.get(ctx.follow.lang(77L), "profile.unknown_name"), ctx.follow.displayName(77L));

        ctx.usernameRepository.record(77L, "chef_anna");
        ctx.usernameRepository.recordName(77L, "Anna Lee");

        assertEquals("Anna Lee (@chef_anna)", ctx.follow.displayName(77L));
        assertEquals("Anna Lee", ctx.follow.plainName(77L));
    }

    @Test
    void followingByNameResolvesAUniqueMatchAndRejectsAmbiguousOnes() {
        for (long id : new long[]{5L, 6L, 7L, 8L}) {
            ctx.languageRepository.set(id, Lang.EN);
        }
        ctx.usernameRepository.recordName(6L, "Anna Lee");
        ctx.usernameRepository.recordName(7L, "Sara Kim");
        ctx.usernameRepository.recordName(8L, "Sara Moon");

        ctx.awaitingFollowId.add(5L);
        ctx.follow.handleFollowId(5L, "anna lee", Lang.EN, null);
        assertTrue(ctx.followRepository.isFollowing(5L, 6L));

        ctx.awaitingFollowId.add(5L);
        ctx.follow.handleFollowId(5L, "sara", Lang.EN, null);
        assertFalse(ctx.followRepository.isFollowing(5L, 7L));
        assertFalse(ctx.followRepository.isFollowing(5L, 8L));
        assertTrue(ctx.awaitingFollowId.contains(5L));
    }

    @Test
    void followingAnUnknownUsernameFollowsNobody() {
        ctx.languageRepository.set(5L, Lang.EN);

        ctx.follow.handleFollowId(5L, "@nobody_here", Lang.EN, null);

        assertTrue(ctx.followRepository.followingOf(5L).isEmpty());
    }

    @Test
    void handleLabelShowsNameAndUsernameButNeverAnId() {
        assertEquals("Sara K (@sara)", FlowHandler.handleLabel("Sara K", "sara"));
        assertEquals("Sara K", FlowHandler.handleLabel("Sara K", null));
        assertEquals("@sara", FlowHandler.handleLabel("", "sara"));
        assertNull(FlowHandler.handleLabel("", null));
    }

    private InlineKeyboardMarkup lastEditedMarkup() {
        EditMessageText edit = sent.stream().filter(EditMessageText.class::isInstance)
                .map(EditMessageText.class::cast).reduce((a, b) -> b).orElseThrow();
        return (InlineKeyboardMarkup) edit.getReplyMarkup();
    }

    private static String lastCallback(InlineKeyboardMarkup markup) {
        var rows = markup.getKeyboard();
        return rows.get(rows.size() - 1).get(0).getCallbackData();
    }

    @Test
    void homeButtonDropsPendingInputAndShowsTheMainMenu() {
        ctx.awaitingSearch.add(5L);
        ctx.awaitingFollowId.add(5L);
        ctx.cookSessions.put(5L, new com.foodbot.food.CookSession());

        ctx.ui.handleHomeCallback(callback(), 5L);

        assertFalse(ctx.awaitingSearch.contains(5L));
        assertFalse(ctx.awaitingFollowId.contains(5L));
        assertFalse(ctx.cookSessions.containsKey(5L));
        assertTrue(sentMessages().isEmpty(), "going home must not post a new chat message");
    }

    @Test
    void topLevelScreensEndWithABackButton() {
        ctx.languageRepository.set(5L, Lang.EN);

        ctx.browse.sendViewFoodsPrompt(5L, Lang.EN);
        assertEquals("home", lastCallback((InlineKeyboardMarkup) sentMessages().get(0).getReplyMarkup()));
        ctx.follow.sendFriendsMenu(5L, Lang.EN, null);
        assertEquals("home", lastCallback((InlineKeyboardMarkup) sentMessages().get(1).getReplyMarkup()));
        ctx.cook.sendCookTimePrompt(5L, Lang.EN);
        assertEquals("home", lastCallback((InlineKeyboardMarkup) sentMessages().get(2).getReplyMarkup()));
        ctx.search.sendSearchPrompt(5L, Lang.EN);
        assertEquals("home", lastCallback((InlineKeyboardMarkup) sentMessages().get(3).getReplyMarkup()));
    }

    @Test
    void recipeListBacksOutToWhereItWasOpenedFrom() {
        ctx.languageRepository.set(5L, Lang.EN);
        addFood("f1", null);

        ctx.browse.sendFoodListPage(5L, Lang.EN, SCOPE_ALL, 0, null);

        assertEquals("vf:menu", lastCallback((InlineKeyboardMarkup) sentMessages().get(0).getReplyMarkup()));
    }

    @Test
    void cookBackStepsThroughTheFlowOneScreenAtATime() {
        ctx.languageRepository.set(5L, Lang.EN);
        com.foodbot.food.CookSession session = new com.foodbot.food.CookSession();
        session.setStep(com.foodbot.food.CookSession.Step.SELECTING_CATEGORY);
        ctx.cookSessions.put(5L, session);

        ctx.cook.handleCookBackCallback(callback(), 5L);
        assertEquals(com.foodbot.food.CookSession.Step.ASK_SHOPPING, session.getStep());
        assertEquals("ckb", lastCallback(lastEditedMarkup()));

        ctx.cook.handleCookBackCallback(callback(), 5L);
        assertEquals(com.foodbot.food.CookSession.Step.SELECTING_INGREDIENTS, session.getStep());
        assertEquals(Integer.valueOf(42), session.getKeyboardMessageId());
        assertEquals("ckb", lastCallback(lastEditedMarkup()));

        ctx.cook.handleCookBackCallback(callback(), 5L);
        assertEquals(com.foodbot.food.CookSession.Step.AWAITING_TIME, session.getStep());
        assertEquals("home", lastCallback(lastEditedMarkup()));
    }

    @Test
    void cookBackWithoutASessionSaysExpired() {
        ctx.cook.handleCookBackCallback(callback(), 5L);

        assertTrue(sent.stream().noneMatch(EditMessageText.class::isInstance));
    }

    private List<SendMessage> messagesTo(long chatId) {
        return sentMessages().stream().filter(m -> m.getChatId().equals(String.valueOf(chatId))).toList();
    }

    private void linkPartners(long inviter, long invitee) {
        ctx.languageRepository.set(inviter, Lang.EN);
        ctx.languageRepository.set(invitee, Lang.EN);
        ctx.partner.handleCallback(callback(), invitee, "shp:ok:" + inviter);
    }

    @Test
    void acceptingAnInviteLinksPartnersAndMergesTheirLists() {
        ctx.shoppingListRepository.addAll(5L, List.of("egg"));
        ctx.shoppingListRepository.addAll(9L, List.of("milk"));

        linkPartners(5L, 9L);

        assertEquals(5L, ctx.partnerRepository.partnerOf(9L));
        assertEquals(9L, ctx.partnerRepository.partnerOf(5L));
        assertEquals(List.of("egg", "milk"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());
        assertTrue(ctx.shoppingListRepository.itemsOf(9L).isEmpty());
        assertTrue(!messagesTo(5L).isEmpty(), "inviter is told the invite was accepted");
        assertTrue(ctx.awaitingPartnerName.contains(9L), "invitee is asked to name their partner");
    }

    @Test
    void invitesToSelfOrToSomeoneAlreadyTakenAreRefused() {
        ctx.languageRepository.set(5L, Lang.EN);
        ctx.partner.handleInvite(5L, Lang.EN, 5L);
        assertEquals(Messages.get(Lang.EN, "partner.invite_self"), messagesTo(5L).get(0).getText());

        linkPartners(5L, 9L);
        ctx.partner.handleCallback(callback(), 7L, "shp:ok:5");
        assertNull(ctx.partnerRepository.partnerOf(7L));
        assertEquals(9L, ctx.partnerRepository.partnerOf(5L));
    }

    @Test
    void partnerBothSeesTheSameListAndIsPingedOnDone() {
        linkPartners(5L, 9L);
        sent.clear();
        ctx.awaitingShoppingItem.add(9L);

        ctx.shopping.handleItemText(9L, "milk, eggs", Lang.EN);
        assertTrue(messagesTo(5L).isEmpty(), "no ping until Done");
        ctx.shopping.handleListCallback(callback(), 9L, "shl:done");

        assertEquals(List.of("milk", "eggs"),
                ctx.shoppingListRepository.itemsOf(5L).stream().map(ShoppingListRepository.Item::ingredient).toList());
        List<SendMessage> pings = messagesTo(5L);
        assertEquals(1, pings.size());
        assertTrue(pings.get(0).getText().contains("milk, eggs"));
    }

    @Test
    void partnerNicknameShowsInTheNotification() {
        linkPartners(5L, 9L);
        ctx.partnerRepository.setNickname(5L, "Ali");
        sent.clear();

        ctx.shopping.handleItemText(9L, "milk", Lang.EN);
        ctx.shopping.handleListCallback(callback(), 9L, "shl:done");

        assertTrue(messagesTo(5L).get(0).getText().contains("Ali"));
    }

    @Test
    void addingFromACookResultPingsThePartnerImmediately() {
        addFoodWith("f1", List.of("egg", "milk"));
        ctx.lastCookResults.put(9L, new CookResultContext(60, java.util.Set.of("egg"), true, null));
        linkPartners(5L, 9L);
        sent.clear();

        ctx.shopping.handleAddMissingCallback(callback(), 9L, "shadd:f1");

        assertTrue(messagesTo(5L).get(0).getText().contains("milk"));
    }

    @Test
    void stoppingSharingGivesEachPersonACopy() {
        linkPartners(5L, 9L);
        ctx.shoppingListRepository.addAll(5L, List.of("egg"));

        ctx.partner.handleCallback(callback(), 9L, "shp:stopyes");

        assertNull(ctx.partnerRepository.partnerOf(5L));
        assertEquals(1, ctx.shoppingListRepository.itemsOf(5L).size());
        assertEquals(1, ctx.shoppingListRepository.itemsOf(9L).size());
        ctx.shoppingListRepository.addAll(9L, List.of("milk"));
        assertEquals(1, ctx.shoppingListRepository.itemsOf(5L).size());
    }

    @Test
    void partnerNameTextIsValidatedAndSaved() {
        linkPartners(5L, 9L);
        ctx.awaitingPartnerName.add(9L);

        ctx.partner.handleNameText(9L, "x".repeat(31), Lang.EN);
        assertTrue(ctx.awaitingPartnerName.contains(9L));
        assertNull(ctx.partnerRepository.nicknameOf(9L));

        ctx.partner.handleNameText(9L, " Sara ", Lang.EN);
        assertTrue(!ctx.awaitingPartnerName.contains(9L));
        assertEquals("Sara", ctx.partnerRepository.nicknameOf(9L));
    }

    @Test
    void partnerInviteLinkIsParsedFromStartPayload() {
        assertEquals(42L, FoodBot.partnerInviterId("/start partner_42"));
        assertNull(FoodBot.partnerInviterId("/start partner_x"));
        assertNull(FoodBot.partnerInviterId("/start user_42"));
    }

    @Test
    void partnerMenuShowsACopyableInviteLink() {
        ctx.botUsername = "foodbot";
        ctx.partner.sendPartnerMenu(5L, Lang.EN, null);
        assertTrue(messagesTo(5L).get(0).getText().contains("https://t.me/foodbot?start=partner_5"));
    }
}
