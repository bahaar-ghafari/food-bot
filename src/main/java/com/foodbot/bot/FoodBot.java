package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.CookSession;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import com.foodbot.social.FollowRepository;
import com.foodbot.social.UsernameRepository;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

import static com.foodbot.bot.BotConstants.*;

public class FoodBot extends TelegramLongPollingBot {
    private final String token;
    private final String username;
    private final BotContext ctx;

    public FoodBot(String token, String username, Long superAdminChatId, Long feedbackChatId) {
        this(token, username, superAdminChatId, feedbackChatId, null);
    }

    public FoodBot(String token, String username, Long superAdminChatId, Long feedbackChatId, String anthropicApiKey) {
        this.token = token;
        this.username = username;
        this.ctx = new BotContext(new ApiExecutor() {
            @Override
            public <T extends java.io.Serializable, M extends BotApiMethod<T>> T execute(M method)
                    throws TelegramApiException {
                return FoodBot.this.execute(method);
            }

            @Override
            public Message sendPhoto(SendPhoto photo) throws TelegramApiException {
                return FoodBot.this.execute(photo);
            }
        }, superAdminChatId, feedbackChatId, seededFoodRepository(),
                new LanguageRepository(), new FollowRepository(), new UsernameRepository(),
                new FavoriteRepository(), new ShoppingListRepository(),
                new AiSuggestionService(anthropicApiKey),
                new MealDbSuggestionService());
        this.ctx.botUsername = username;
        startDigestScheduler();
    }

    private static FoodRepository seededFoodRepository() {
        FoodRepository repository = new FoodRepository();
        repository.seedFrom("/seed_foods.json");
        return repository;
    }

    /** Sends queued follow digests once a day at 18:00 UTC. */
    private void startDigestScheduler() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        java.time.ZonedDateTime next = now.withHour(18).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        java.util.concurrent.ScheduledExecutorService scheduler =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "follow-digest");
                    t.setDaemon(true);
                    return t;
                });
        scheduler.scheduleAtFixedRate(() -> {
            try {
                ctx.follow.sendDigests();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }, java.time.Duration.between(now, next).toSeconds(), java.time.Duration.ofDays(1).toSeconds(),
                java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            processUpdate(update);
        } catch (Exception e) {
            e.printStackTrace();
            Long chatId = extractChatId(update);
            if (chatId != null) {
                try {
                    execute(new SendMessage(String.valueOf(chatId), MAINTENANCE_MESSAGE));
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private Long extractChatId(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                return update.getCallbackQuery().getMessage().getChatId();
            }
            if (update.hasMessage()) {
                return update.getMessage().getChatId();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void rememberUsername(Update update) {
        User from = update.hasMessage() ? update.getMessage().getFrom()
                : update.hasCallbackQuery() ? update.getCallbackQuery().getFrom()
                : update.hasInlineQuery() ? update.getInlineQuery().getFrom() : null;
        if (from != null && !Boolean.TRUE.equals(from.getIsBot())) {
            ctx.usernameRepository.record(from.getId(), from.getUserName());
            ctx.usernameRepository.recordName(from.getId(), FlowHandler.fullName(from.getFirstName(), from.getLastName()));
        }
    }

    private void processUpdate(Update update) {
        rememberUsername(update);
        if (update.hasInlineQuery()) {
            ctx.inline.handleInlineQuery(update.getInlineQuery());
            return;
        }
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            if (callbackQuery.getData().startsWith(CB_LANG)) {
                handleLanguageCallback(callbackQuery);
            } else {
                handleCallback(callbackQuery);
            }
            return;
        }
        if (update.hasMessage() && update.getMessage().getUserShared() != null) {
            long sharer = update.getMessage().getChatId();
            Lang sharerLang = ctx.languageRepository.get(sharer);
            if (sharerLang != null) {
                ctx.follow.handleUserShared(sharer, update.getMessage().getUserShared().getUserId(), sharerLang,
                        update.getMessage().getFrom());
            }
            return;
        }
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            handlePhotoMessage(update.getMessage());
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if (text.equalsIgnoreCase("/chatid") || text.toLowerCase().startsWith("/chatid@")) {
            System.out.println("Chat ID requested: " + chatId);
            ctx.ui.send(chatId, "✅");
            return;
        }

        if (text.equalsIgnoreCase("/lang")) {
            sendLanguagePrompt(chatId);
            return;
        }

        String sharedRecipeId = sharedRecipeId(text);
        Long sharedProfileId = sharedProfileId(text);
        Lang lang = ctx.languageRepository.get(chatId);
        if (lang == null) {
            if (sharedRecipeId != null) {
                ctx.pendingSharedRecipe.put(chatId, sharedRecipeId);
            }
            if (sharedProfileId != null) {
                ctx.pendingSharedProfile.put(chatId, sharedProfileId);
            }
            sendLanguagePrompt(chatId);
            return;
        }

        if (sharedRecipeId != null) {
            ctx.browse.openSharedRecipe(chatId, lang, sharedRecipeId);
        } else if (sharedProfileId != null) {
            ctx.follow.sendProfile(chatId, lang, sharedProfileId, null);
        } else if (text.equalsIgnoreCase("/start")) {
            ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "welcome"));
        } else if (text.equalsIgnoreCase("/help") || text.equals(Messages.get(lang, "btn.help"))) {
            ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "help.text"));
        } else if (text.equalsIgnoreCase("/settings") || text.equals(Messages.get(lang, "btn.settings"))) {
            ctx.ui.sendSettingsMenu(chatId, lang);
        } else if (text.equals(Messages.get(lang, "btn.back"))
                && !ctx.addFoodSessions.containsKey(chatId) && !ctx.cookSessions.containsKey(chatId)
                && !ctx.editSessions.containsKey(chatId)) {
            ctx.awaitingFollowId.remove(chatId);
            ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "settings.back"));
        } else if (text.equalsIgnoreCase("/lang") || text.equals(Messages.get(lang, "btn.change_lang"))) {
            sendLanguagePrompt(chatId);
        } else if (text.equalsIgnoreCase("/addfood") || text.equals(Messages.get(lang, "btn.add_food"))) {
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            AddFoodSession newSession = new AddFoodSession();
            ctx.addFoodSessions.put(chatId, newSession);
            ctx.addFood.sendAddFoodNamePrompt(chatId, newSession, lang);
        } else if (text.equalsIgnoreCase("/refresh") || text.equals(Messages.get(lang, "btn.refresh"))) {
            ctx.ui.refresh(chatId, lang);
        } else if (text.equalsIgnoreCase("/cancel")) {
            AddFoodSession cancelledAddFood = ctx.addFoodSessions.remove(chatId);
            if (cancelledAddFood != null) {
                ctx.ui.clearInlineKeyboard(chatId, cancelledAddFood.getKeyboardMessageId());
            }
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingFeedback.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingFollowId.remove(chatId);
            ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "cancelled"));
        } else if (text.equalsIgnoreCase("/menu") || text.equals(Messages.get(lang, "btn.all_foods"))) {
            ctx.browse.sendViewFoodsPrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/cook") || text.equals(Messages.get(lang, "btn.what_can_cook"))) {
            ctx.addFoodSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.cookSessions.put(chatId, new CookSession());
            ctx.cook.sendCookTimePrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/shopping") || text.equals(Messages.get(lang, "btn.shopping"))) {
            ctx.addFoodSessions.remove(chatId);
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.shopping.sendShoppingList(chatId, lang, null);
        } else if (text.equalsIgnoreCase("/feedback") || text.equals(Messages.get(lang, "btn.feedback"))) {
            ctx.addFoodSessions.remove(chatId);
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingFeedback.add(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.ui.sendWithHomeButton(chatId, lang, Messages.get(lang, "feedback.ask"));
        } else if (text.equalsIgnoreCase("/search") || text.equals(Messages.get(lang, "btn.search"))
                || text.toLowerCase().startsWith("/search ")) {
            ctx.addFoodSessions.remove(chatId);
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingFeedback.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            String query = text.toLowerCase().startsWith("/search ") ? text.substring(8).trim() : "";
            if (query.isEmpty()) {
                ctx.awaitingSearch.add(chatId);
                ctx.search.sendSearchPrompt(chatId, lang);
            } else {
                ctx.search.handleSearchQuery(chatId, query, lang);
            }
        } else if (text.equalsIgnoreCase("/friends") || text.equalsIgnoreCase("/profile") || text.equals(Messages.get(lang, "btn.friends"))) {
            ctx.addFoodSessions.remove(chatId);
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingFeedback.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingFollowId.remove(chatId);
            ctx.follow.sendFriendsMenu(chatId, lang, null);
        } else if (text.equalsIgnoreCase("/follow") || text.toLowerCase().startsWith("/follow ")) {
            ctx.addFoodSessions.remove(chatId);
            ctx.cookSessions.remove(chatId);
            ctx.editSessions.remove(chatId);
            ctx.awaitingFeedback.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            ctx.awaitingSearch.remove(chatId);
            ctx.awaitingShoppingItem.remove(chatId);
            String arg = text.length() > 7 ? text.substring(7).trim() : "";
            if (arg.isEmpty()) {
                ctx.awaitingFollowId.add(chatId);
                ctx.follow.sendFollowPrompt(chatId, lang);
            } else {
                ctx.follow.handleFollowId(chatId, arg, lang, update.getMessage().getFrom());
            }
        } else if (ctx.addFoodSessions.containsKey(chatId)) {
            ctx.addFood.handleAddFoodText(chatId, text, lang);
        } else if (ctx.cookSessions.containsKey(chatId)) {
            ctx.cook.handleCookText(chatId, text, lang);
        } else if (ctx.editSessions.containsKey(chatId)) {
            ctx.editFood.handleEditText(chatId, text, lang);
        } else if (ctx.awaitingFeedback.contains(chatId)) {
            ctx.feedback.handleFeedbackText(chatId, text, lang, update.getMessage().getFrom());
        } else if (ctx.awaitingShoppingItem.contains(chatId)) {
            ctx.shopping.handleItemText(chatId, text, lang);
        } else if (ctx.awaitingSearch.contains(chatId)) {
            ctx.search.handleSearchQuery(chatId, text, lang);
        } else if (ctx.awaitingFollowId.contains(chatId)) {
            ctx.follow.handleFollowId(chatId, text, lang, update.getMessage().getFrom());
        } else {
            ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "fallback"));
        }
    }

    private void handlePhotoMessage(Message message) {
        long chatId = message.getChatId();
        Lang lang = ctx.languageRepository.get(chatId);
        if (lang == null) {
            return;
        }
        boolean adding = ctx.addFoodSessions.containsKey(chatId);
        if (!adding && !ctx.editSessions.containsKey(chatId)) {
            return;
        }
        // Telegram sends several sizes of the same photo; the last is the largest
        List<PhotoSize> sizes = message.getPhoto();
        String fileId = sizes.get(sizes.size() - 1).getFileId();
        if (adding) {
            ctx.addFood.handleAddFoodPhoto(chatId, fileId, lang);
        } else {
            ctx.editFood.handleEditPhoto(chatId, fileId, lang);
        }
    }

    private void handleLanguageCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String code = callbackQuery.getData().substring(CB_LANG.length());
        Lang lang;
        try {
            lang = Lang.valueOf(code);
        } catch (IllegalArgumentException e) {
            ctx.ui.answerCallback(callbackQuery.getId(), null, false);
            return;
        }
        ctx.languageRepository.set(chatId, lang);
        ctx.ui.answerCallback(callbackQuery.getId(), null, false);
        ctx.ui.sendWithMainMenu(chatId, lang, Messages.get(lang, "lang.confirmed") + " " + Messages.get(lang, "welcome"));
        String pendingRecipe = ctx.pendingSharedRecipe.remove(chatId);
        if (pendingRecipe != null) {
            ctx.browse.openSharedRecipe(chatId, lang, pendingRecipe);
        }
        Long pendingProfile = ctx.pendingSharedProfile.remove(chatId);
        if (pendingProfile != null) {
            ctx.follow.sendProfile(chatId, lang, pendingProfile, null);
        }
    }

    /** The recipe id in a {@code /start recipe_<id>} deep link, or null for any other text. */
    static String sharedRecipeId(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 2 || !(parts[0].equalsIgnoreCase("/start")
                || parts[0].toLowerCase().startsWith("/start@"))
                || !parts[1].startsWith(START_RECIPE_PREFIX)) {
            return null;
        }
        String id = parts[1].substring(START_RECIPE_PREFIX.length());
        return id.isEmpty() ? null : id;
    }

    /** The chat id in a {@code /start user_<id>} deep link, or null for any other text. */
    static Long sharedProfileId(String text) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 2 || !(parts[0].equalsIgnoreCase("/start")
                || parts[0].toLowerCase().startsWith("/start@"))
                || !parts[1].startsWith(START_PROFILE_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(parts[1].substring(START_PROFILE_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendLanguagePrompt(long chatId) {
        InlineKeyboardButton fa = new InlineKeyboardButton("🦁☀️ فارسی");
        fa.setCallbackData(CB_LANG + Lang.FA.name());
        InlineKeyboardButton en = new InlineKeyboardButton("🇬🇧 English");
        en.setCallbackData(CB_LANG + Lang.EN.name());
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(Lang.FA, "lang.prompt"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(fa, en))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.equals(CB_HOME)) {
            ctx.ui.handleHomeCallback(callbackQuery, chatId);
        } else if (data.equals(CB_COOK_BACK)) {
            ctx.cook.handleCookBackCallback(callbackQuery, chatId);
        } else if (data.equals(CB_VIEW_FOODS_MENU)) {
            ctx.browse.sendViewFoodsPrompt(chatId, ctx.ui.lang(chatId), callbackQuery.getMessage().getMessageId());
            ctx.ui.answerCallback(callbackQuery.getId(), null, false);
        } else if (data.equals(CB_ADDFOOD_CANCEL)) {
            ctx.addFood.handleAddFoodCancelCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_BACK)) {
            ctx.addFood.handleAddFoodBackCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_VIEW_FOODS_MINE) || data.equals(CB_VIEW_FOODS_GLOBAL)
                || data.equals(CB_VIEW_FOODS_ALL) || data.equals(CB_VIEW_FOODS_CREATED)
                || data.equals(CB_VIEW_FOODS_FAVORITES)) {
            ctx.browse.handleViewFoodsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_VIEW_FOODS_PAGE)) {
            ctx.browse.handleViewFoodsPageCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FRIENDS_FOLLOW) || data.equals(CB_FRIENDS_LIST) || data.equals(CB_FRIENDS_VISIBILITY)
                || data.startsWith(CB_FRIENDS_USER) || data.startsWith(CB_FRIENDS_UNFOLLOW)
                || data.startsWith(CB_FRIENDS_APPROVE) || data.startsWith(CB_FRIENDS_APPROVE_VISIBLE)
                || data.startsWith(CB_FRIENDS_DECLINE) || data.equals(CB_FRIENDS_DIGEST)
                || data.startsWith(CB_FRIENDS_MUTE) || data.equals(CB_FRIENDS_FOLLOWERS) || data.equals(CB_FRIENDS_MENU)
                || data.startsWith(CB_PROFILE_VIEW) || data.startsWith(CB_PROFILE_FOLLOW)
                || data.startsWith(CB_PROFILE_UNFOLLOW)) {
            ctx.follow.handleFriendsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADMIN_RELOAD)) {
            ctx.browse.handleAdminReloadCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_INGREDIENT)) {
            ctx.addFood.handleAddFoodIngredientCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_AMOUNT)) {
            ctx.addFood.handleAddFoodAmountCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_RECIPE_DONE)) {
            ctx.addFood.handleAddFoodRecipeDoneCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_RECIPE_STEP)) {
            ctx.addFood.handleAddFoodRecipeStepCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_PHOTO_SKIP) || data.equals(CB_ADDFOOD_PHOTO_REMOVE)) {
            ctx.addFood.handleAddFoodPhotoButtonCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_REVIEW_EDIT)) {
            ctx.addFood.handleAddFoodReviewEditCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_CONFIRM)) {
            ctx.addFood.handleAddFoodConfirmCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_CATEGORY)) {
            ctx.addFood.handleAddFoodCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_TIME_PRESET)) {
            ctx.cook.handleCookTimePresetCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_INGREDIENT)) {
            ctx.cook.handleCookIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_COOK_SHOP_YES) || data.equals(CB_COOK_SHOP_NO)) {
            ctx.cook.handleCookShopCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_CATEGORY)) {
            ctx.cook.handleCookCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_RESULT_PAGE)) {
            ctx.cook.handleCookResultPageCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_VIEW)) {
            ctx.cook.handleCookViewCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_SHOP_ADD)) {
            ctx.shopping.handleAddMissingCallback(callbackQuery, chatId, data);
        } else if (data.startsWith("shl:")) {
            ctx.shopping.handleListCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_SAVE)) {
            ctx.editFood.handleEditSaveCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_EXIT)) {
            ctx.editFood.handleEditExitCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_PHOTO_SKIP) || data.equals(CB_FOOD_EDIT_PHOTO_REMOVE)) {
            ctx.editFood.handleEditPhotoButtonCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_FIELD)) {
            ctx.editFood.handleEditFieldCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_CATEGORY)) {
            ctx.editFood.handleEditCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_INGREDIENT)) {
            ctx.editFood.handleEditIngredientCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_AMOUNT)) {
            ctx.editFood.handleEditAmountCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_RECIPE_STEP)) {
            ctx.editFood.handleEditRecipeStepCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_RECIPE_CLEAR)) {
            ctx.editFood.handleEditRecipeClearCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_RECIPE_DONE)) {
            ctx.editFood.handleEditRecipeDoneCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_START)) {
            ctx.editFood.handleEditStartCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_CONFIRM_YES)) {
            ctx.editFood.handleDeleteConfirmYesCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_CONFIRM_NO)) {
            ctx.editFood.handleDeleteConfirmNoCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_START)) {
            ctx.editFood.handleDeleteStartCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_FAVORITE)) {
            ctx.browse.handleFavoriteToggleCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_SCALE)) {
            ctx.browse.handleFoodScaleCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_SETTINGS)) {
            ctx.browse.handleFoodSettingsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_VIEW)) {
            ctx.browse.handleFoodViewCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_SEARCH_FUZZY_YES)) {
            ctx.search.handleSearchFuzzyYesCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_SEARCH_FUZZY_NO)) {
            ctx.search.handleSearchFuzzyNoCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_SEARCH_OTHERLANG_YES)) {
            ctx.search.handleSearchOtherLangYesCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_SEARCH_OTHERLANG_NO)) {
            ctx.search.handleSearchOtherLangNoCallback(callbackQuery, chatId, data);
        } else {
            ctx.ui.answerCallback(callbackQuery.getId(), null, false);
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
