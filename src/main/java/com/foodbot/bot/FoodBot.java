package com.foodbot.bot;

import com.foodbot.food.AddFoodSession;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.CookSession;
import com.foodbot.food.EditFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodCategories;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.IngredientIcons;
import com.foodbot.food.IngredientPickerState;
import com.foodbot.food.IngredientSearch;
import com.foodbot.food.IngredientTranslations;
import com.foodbot.food.Paginator;
import com.foodbot.food.PantryStaples;
import com.foodbot.food.RecipeStepEditorState;
import com.foodbot.food.RecipeSteps;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FoodBot extends TelegramLongPollingBot {
    private static final int INGREDIENTS_PER_PAGE = 12;
    private static final String INGREDIENT_PAGE_INFIX = "page:";
    private static final int FOODS_PER_PAGE = 10;
    private static final String SCOPE_MINE = "mine";
    private static final String SCOPE_GLOBAL = "global";

    private static final String CB_LANG = "lang:";
    private static final String CB_ADDFOOD_CATEGORY = "afc:";
    private static final String CB_ADDFOOD_INGREDIENT = "afi:";
    private static final String CB_ADDFOOD_INGREDIENT_DONE = "afi:done";
    private static final String CB_ADDFOOD_INGREDIENT_CLEAR = "afi:clear";

    private static final String CB_COOK_INGREDIENT = "cki:";
    private static final String CB_COOK_INGREDIENT_DONE = "cki:done";
    private static final String CB_COOK_INGREDIENT_CLEAR = "cki:clear";
    private static final String CB_COOK_SHOP_YES = "cks:yes";
    private static final String CB_COOK_SHOP_NO = "cks:no";
    private static final String CB_COOK_CATEGORY = "ckc:";
    private static final String CB_COOK_TIME_PRESET = "ckt:";
    private static final String CB_COOK_VIEW = "cv:";
    private static final String CB_COOK_RESULT_PAGE = "ckrp:";
    private static final String GROUP_READY = "ready";
    private static final String GROUP_SHOP = "shop";
    private static final String[] COOK_TIME_PRESET_KEYS =
            {"cook.time.fast", "cook.time.2h", "cook.time.5h", "cook.time.1day", "cook.time.any"};
    private static final int[] COOK_TIME_PRESET_MINUTES = {30, 120, 300, 1440, Integer.MAX_VALUE};
    private static final String CATEGORY_ANY = "ANY";
    private static final String CB_ADDFOOD_SCOPE_MINE = "afs:mine";
    private static final String CB_ADDFOOD_SCOPE_GLOBAL = "afs:global";
    private static final String CB_VIEW_FOODS_MINE = "vf:mine";
    private static final String CB_VIEW_FOODS_GLOBAL = "vf:global";
    private static final String CB_VIEW_FOODS_PAGE = "vfp:";

    private static final String CB_FOOD_EDIT_START = "fe:";
    private static final String CB_FOOD_EDIT_FIELD = "fef:";
    private static final String CB_FOOD_EDIT_CATEGORY = "fec:";
    private static final String CB_FOOD_EDIT_INGREDIENT = "fei:";
    private static final String CB_FOOD_EDIT_INGREDIENT_DONE = "fei:done";
    private static final String CB_FOOD_EDIT_INGREDIENT_CLEAR = "fei:clear";
    private static final String CB_FOOD_EDIT_RECIPE_CLEAR = "ferclear";
    private static final String CB_FOOD_EDIT_RECIPE_DONE = "ferdone";
    private static final String CB_FOOD_EDIT_RECIPE_STEP = "fers:";
    private static final String CB_FOOD_DELETE_START = "fd:";
    private static final String CB_FOOD_DELETE_CONFIRM_YES = "fdy:";
    private static final String CB_FOOD_DELETE_CONFIRM_NO = "fdn:";
    private static final String CB_ADDFOOD_RECIPE_DONE = "afrdone";
    private static final String CB_ADDFOOD_RECIPE_STEP = "afrs:";
    private static final String CB_ADDFOOD_BACK = "afback";
    private static final String CB_ADDFOOD_CANCEL = "afcancel";
    private static final String CB_ADDFOOD_REVIEW_EDIT = "afrv:";
    private static final String CB_ADDFOOD_CONFIRM = "afconfirm";
    private static final String CB_FOOD_VIEW = "fv:";
    private static final String CB_FOOD_SETTINGS = "fset:";
    private static final int RECIPE_STEP_CHAR_LIMIT = 150;

    private final String token;
    private final String username;
    private final Long superAdminChatId;
    private final Long feedbackChatId;
    private final FoodRepository foodRepository = new FoodRepository();
    private final LanguageRepository languageRepository = new LanguageRepository();
    private final Map<Long, AddFoodSession> addFoodSessions = new ConcurrentHashMap<>();
    private final Map<Long, CookSession> cookSessions = new ConcurrentHashMap<>();
    private final Map<Long, EditFoodSession> editSessions = new ConcurrentHashMap<>();
    private final Map<Long, CookResultContext> lastCookResults = new ConcurrentHashMap<>();
    private final Set<Long> awaitingFeedback = ConcurrentHashMap.newKeySet();

    public FoodBot(String token, String username, Long superAdminChatId, Long feedbackChatId) {
        this.token = token;
        this.username = username;
        this.superAdminChatId = superAdminChatId;
        this.feedbackChatId = feedbackChatId;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            if (callbackQuery.getData().startsWith(CB_LANG)) {
                handleLanguageCallback(callbackQuery);
            } else {
                handleCallback(callbackQuery);
            }
            return;
        }
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if (text.equalsIgnoreCase("/chatid") || text.toLowerCase().startsWith("/chatid@")) {
            System.out.println("Chat ID requested: " + chatId);
            send(chatId, "✅");
            return;
        }

        if (text.equalsIgnoreCase("/lang")) {
            sendLanguagePrompt(chatId);
            return;
        }

        Lang lang = languageRepository.get(chatId);
        if (lang == null) {
            sendLanguagePrompt(chatId);
            return;
        }

        if (text.equalsIgnoreCase("/start")) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "welcome"));
        } else if (text.equalsIgnoreCase("/help") || text.equals(Messages.get(lang, "btn.help"))) {
            String helpText = Messages.get(lang, "help.text");
            if (isSuperAdmin(chatId)) {
                helpText += "\n" + Messages.get(lang, "help.text.admin");
            }
            sendWithMainMenu(chatId, lang, helpText);
        } else if (text.equals(Messages.get(lang, "btn.change_lang"))) {
            sendLanguagePrompt(chatId);
        } else if (text.equalsIgnoreCase("/addfood") || text.equals(Messages.get(lang, "btn.add_food"))) {
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            AddFoodSession newSession = new AddFoodSession();
            addFoodSessions.put(chatId, newSession);
            sendAddFoodScopePrompt(chatId, newSession, lang);
        } else if (text.equalsIgnoreCase("/cancel")) {
            AddFoodSession cancelledAddFood = addFoodSessions.remove(chatId);
            if (cancelledAddFood != null) {
                clearInlineKeyboard(chatId, cancelledAddFood.getKeyboardMessageId());
            }
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            awaitingFeedback.remove(chatId);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "cancelled"));
        } else if (text.equalsIgnoreCase("/menu") || text.equals(Messages.get(lang, "btn.all_foods"))) {
            sendViewFoodsPrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/cook") || text.equals(Messages.get(lang, "btn.what_can_cook"))) {
            addFoodSessions.remove(chatId);
            editSessions.remove(chatId);
            cookSessions.put(chatId, new CookSession());
            sendCookTimePrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/feedback") || text.equals(Messages.get(lang, "btn.feedback"))) {
            addFoodSessions.remove(chatId);
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            awaitingFeedback.add(chatId);
            send(chatId, Messages.get(lang, "feedback.ask"));
        } else if (addFoodSessions.containsKey(chatId)) {
            handleAddFoodText(chatId, text, lang);
        } else if (cookSessions.containsKey(chatId)) {
            handleCookText(chatId, text, lang);
        } else if (editSessions.containsKey(chatId)) {
            handleEditText(chatId, text, lang);
        } else if (awaitingFeedback.contains(chatId)) {
            handleFeedbackText(chatId, text, lang, update.getMessage().getFrom());
        } else {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "fallback"));
        }
    }

    private void handleFeedbackText(long chatId, String text, Lang lang, User from) {
        awaitingFeedback.remove(chatId);
        if (feedbackChatId != null) {
            String who = (from != null && from.getUserName() != null) ? "@" + from.getUserName()
                    : (from != null && from.getFirstName() != null) ? from.getFirstName() : String.valueOf(chatId);
            Lang feedbackLang = lang(feedbackChatId);
            send(feedbackChatId, Messages.get(feedbackLang, "feedback.notify_admin", who, text));
        }
        sendWithMainMenu(chatId, lang, Messages.get(lang, "feedback.sent"));
    }

    private Lang lang(long chatId) {
        Lang lang = languageRepository.get(chatId);
        return lang != null ? lang : Lang.FA;
    }

    private boolean isSuperAdmin(long chatId) {
        return superAdminChatId != null && superAdminChatId == chatId;
    }

    private boolean canModify(Food food, long chatId) {
        return isSuperAdmin(chatId) || (food.getOwnerChatId() != null && food.getOwnerChatId() == chatId);
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

    private void handleLanguageCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String code = callbackQuery.getData().substring(CB_LANG.length());
        Lang lang;
        try {
            lang = Lang.valueOf(code);
        } catch (IllegalArgumentException e) {
            answerCallback(callbackQuery.getId(), null, false);
            return;
        }
        languageRepository.set(chatId, lang);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "lang.confirmed") + " " + Messages.get(lang, "welcome"));
    }

    private void sendAddFoodScopePrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton mine = new InlineKeyboardButton(Messages.get(lang, "scope.mine"));
        mine.setCallbackData(CB_ADDFOOD_SCOPE_MINE);
        InlineKeyboardButton global = new InlineKeyboardButton(Messages.get(lang, "scope.global"));
        global.setCallbackData(CB_ADDFOOD_SCOPE_GLOBAL);
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_CANCEL);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_scope"),
                new InlineKeyboardMarkup(List.of(List.of(mine, global), List.of(back))));
    }

    private void sendAddFoodNamePrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_name"),
                new InlineKeyboardMarkup(List.of(List.of(back))));
    }

    private void sendAddFoodTimePrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_time", session.getName()),
                new InlineKeyboardMarkup(List.of(List.of(back))));
    }

    private void sendAddFoodCategoryPrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendCategoryKeyboard(chatId, CB_ADDFOOD_CATEGORY, FoodCategories.ALL, lang,
                Messages.get(lang, "addfood.ask_category"), session.getKeyboardMessageId(), back);
    }

    private void handleAddFoodScopeCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.ASK_SCOPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        session.setOwnerChatId(data.equals(CB_ADDFOOD_SCOPE_MINE) ? chatId : null);
        session.setStep(AddFoodSession.Step.AWAITING_NAME);
        answerCallback(callbackQuery.getId(), null, false);
        sendAddFoodNamePrompt(chatId, session, lang);
    }

    private void handleAddFoodCancelCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        if (session != null) {
            clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        }
        sendWithMainMenu(chatId, lang, Messages.get(lang, "cancelled"));
    }

    private void handleAddFoodBackCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        if (session.isReturnToReview()) {
            session.setReturnToReview(false);
            session.setStep(AddFoodSession.Step.REVIEW);
            renderAddFoodReview(chatId, session, lang);
            return;
        }
        switch (session.getStep()) {
            case AWAITING_NAME:
                session.setStep(AddFoodSession.Step.ASK_SCOPE);
                sendAddFoodScopePrompt(chatId, session, lang);
                break;
            case AWAITING_PREP_TIME:
                session.setStep(AddFoodSession.Step.AWAITING_NAME);
                sendAddFoodNamePrompt(chatId, session, lang);
                break;
            case SELECTING_CATEGORY:
                session.setStep(AddFoodSession.Step.AWAITING_PREP_TIME);
                sendAddFoodTimePrompt(chatId, session, lang);
                break;
            case SELECTING_INGREDIENTS:
                session.setStep(AddFoodSession.Step.SELECTING_CATEGORY);
                sendAddFoodCategoryPrompt(chatId, session, lang);
                break;
            case AWAITING_RECIPE:
                session.setStep(AddFoodSession.Step.SELECTING_INGREDIENTS);
                session.setEditingRecipeStepIndex(null);
                renderAddFoodIngredientKeyboard(chatId, session, lang);
                break;
            case REVIEW:
                session.setStep(AddFoodSession.Step.AWAITING_RECIPE);
                renderAddFoodRecipeManager(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    private void sendViewFoodsPrompt(long chatId, Lang lang) {
        InlineKeyboardButton mine = new InlineKeyboardButton(Messages.get(lang, "scope.mine"));
        mine.setCallbackData(CB_VIEW_FOODS_MINE);
        InlineKeyboardButton global = new InlineKeyboardButton(Messages.get(lang, "scope.global"));
        global.setCallbackData(CB_VIEW_FOODS_GLOBAL);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "foods.ask_scope"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(mine, global))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleViewFoodsCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        String scope = data.equals(CB_VIEW_FOODS_MINE) ? SCOPE_MINE : SCOPE_GLOBAL;
        sendFoodListPage(chatId, lang, scope, 0, null);
    }

    private void handleViewFoodsPageCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String remainder = data.substring(CB_VIEW_FOODS_PAGE.length());
        int separatorIndex = remainder.lastIndexOf(':');
        String scope = remainder.substring(0, separatorIndex);
        int page = Integer.parseInt(remainder.substring(separatorIndex + 1));
        answerCallback(callbackQuery.getId(), null, false);
        sendFoodListPage(chatId, lang, scope, page, callbackQuery.getMessage().getMessageId());
    }

    private void handleAddFoodText(long chatId, String text, Lang lang) {
        AddFoodSession session = addFoodSessions.get(chatId);
        switch (session.getStep()) {
            case ASK_SCOPE:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;

            case AWAITING_NAME:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                session.setName(text);
                if (session.isReturnToReview()) {
                    session.setReturnToReview(false);
                    session.setStep(AddFoodSession.Step.REVIEW);
                    renderAddFoodReview(chatId, session, lang);
                } else {
                    session.setStep(AddFoodSession.Step.AWAITING_PREP_TIME);
                    sendAddFoodTimePrompt(chatId, session, lang);
                }
                break;

            case AWAITING_PREP_TIME:
                Integer minutes = parseMinutes(text);
                if (minutes == null) {
                    send(chatId, Messages.get(lang, "addfood.invalid_time"));
                    return;
                }
                session.setPrepTimeMinutes(minutes);
                if (session.isReturnToReview()) {
                    session.setReturnToReview(false);
                    session.setStep(AddFoodSession.Step.REVIEW);
                    renderAddFoodReview(chatId, session, lang);
                } else {
                    session.setStep(AddFoodSession.Step.SELECTING_CATEGORY);
                    sendAddFoodCategoryPrompt(chatId, session, lang);
                }
                break;

            case SELECTING_CATEGORY:
                send(chatId, Messages.get(lang, "addfood.tap_category"));
                break;

            case SELECTING_INGREDIENTS:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                handleIngredientTyped(session, text);
                renderAddFoodIngredientKeyboard(chatId, session, lang);
                break;

            case AWAITING_RECIPE:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                if (text.length() > RECIPE_STEP_CHAR_LIMIT) {
                    send(chatId, Messages.get(lang, "recipe.step_too_long", text.length(), RECIPE_STEP_CHAR_LIMIT));
                    return;
                }
                if (session.getEditingRecipeStepIndex() != null) {
                    session.getRecipeSteps().set(session.getEditingRecipeStepIndex(), text);
                    session.setEditingRecipeStepIndex(null);
                } else {
                    session.getRecipeSteps().add(text);
                }
                renderAddFoodRecipeManager(chatId, session, lang);
                break;

            case REVIEW:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;
        }
    }

    private void handleCookText(long chatId, String text, Lang lang) {
        CookSession session = cookSessions.get(chatId);
        switch (session.getStep()) {
            case AWAITING_TIME:
                Integer minutes = parseMinutes(text);
                if (minutes == null) {
                    send(chatId, Messages.get(lang, "cook.invalid_time"));
                    return;
                }
                session.setTimeMinutes(minutes);
                session.setStep(CookSession.Step.SELECTING_INGREDIENTS);
                addNonStapleIngredients(session.getCandidateIngredients(), foodRepository.findAllIngredients(chatId, lang));
                sendCookIngredientKeyboard(chatId, session, lang);
                break;

            case SELECTING_INGREDIENTS:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                handleIngredientTyped(session, text);
                editCookIngredientKeyboard(chatId, session, lang);
                break;

            case ASK_SHOPPING:
            case SELECTING_CATEGORY:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;
        }
    }

    private void handleEditText(long chatId, String text, Lang lang) {
        EditFoodSession session = editSessions.get(chatId);
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            send(chatId, Messages.get(lang, "permission.denied"));
            return;
        }
        Food food = foodOpt.get();
        switch (session.getStep()) {
            case EDITING_NAME:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                Food updatedName = new Food(food.getId(), text, food.getPrepTimeMinutes(), food.getCategory(),
                        food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe(),
                        food.getLanguage());
                foodRepository.update(updatedName);
                editSessions.remove(chatId);
                sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updatedName, lang)));
                break;

            case EDITING_TIME:
                Integer minutes = parseMinutes(text);
                if (minutes == null) {
                    send(chatId, Messages.get(lang, "addfood.invalid_time"));
                    return;
                }
                Food updatedTime = new Food(food.getId(), food.getName(), minutes, food.getCategory(),
                        food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe(),
                        food.getLanguage());
                foodRepository.update(updatedTime);
                editSessions.remove(chatId);
                sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updatedTime, lang)));
                break;

            case EDITING_INGREDIENTS:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                handleIngredientTyped(session, text);
                editEditIngredientKeyboard(chatId, session, lang);
                break;

            case EDITING_RECIPE:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                if (text.length() > RECIPE_STEP_CHAR_LIMIT) {
                    send(chatId, Messages.get(lang, "recipe.step_too_long", text.length(), RECIPE_STEP_CHAR_LIMIT));
                    return;
                }
                if (session.getEditingRecipeStepIndex() != null) {
                    session.getRecipeSteps().set(session.getEditingRecipeStepIndex(), text);
                    session.setEditingRecipeStepIndex(null);
                } else {
                    session.getRecipeSteps().add(text);
                }
                renderEditRecipeManager(chatId, session, lang);
                break;

            case CHOOSING_FIELD:
            case EDITING_CATEGORY:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;
        }
    }

    private boolean matchesLanguage(String text, Lang lang) {
        boolean hasPersian = text.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        boolean hasLatin = text.chars().anyMatch(c -> Character.isLetter(c) && c < 128);
        if (lang == Lang.FA) {
            return hasPersian && !hasLatin;
        }
        return hasLatin && !hasPersian;
    }

    private Integer parseMinutes(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleIngredientTyped(IngredientPickerState state, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        List<String> candidates = state.getCandidateIngredients();
        state.setIngredientPage(0);

        String exact = IngredientSearch.findExactMatch(candidates, trimmed);
        if (exact != null) {
            state.getSelectedIngredients().add(exact);
            state.setIngredientFilter("");
            return;
        }

        if (IngredientSearch.hasPartialMatch(candidates, trimmed)) {
            state.setIngredientFilter(trimmed);
            return;
        }

        candidates.add(trimmed);
        state.getSelectedIngredients().add(trimmed);
        state.setIngredientFilter("");
    }

    private boolean handleIngredientPageNav(IngredientPickerState state, String data, String prefix) {
        String pagePrefix = prefix + INGREDIENT_PAGE_INFIX;
        if (!data.startsWith(pagePrefix)) {
            return false;
        }
        state.setIngredientPage(Integer.parseInt(data.substring(pagePrefix.length())));
        return true;
    }

    private void toggleAndAnswer(Set<String> selected, String ingredient, String callbackId, Lang lang) {
        if (selected.remove(ingredient)) {
            answerCallback(callbackId, Messages.get(lang, "removed", ingredient), false);
        } else {
            selected.add(ingredient);
            answerCallback(callbackId, Messages.get(lang, "added", ingredient), false);
        }
    }

    private record PagedRef(String key, int page, String id) {
    }

    private PagedRef parsePagedRef(String remainder) {
        int firstColon = remainder.indexOf(':');
        int secondColon = remainder.indexOf(':', firstColon + 1);
        String key = remainder.substring(0, firstColon);
        int page = Integer.parseInt(remainder.substring(firstColon + 1, secondColon));
        String id = remainder.substring(secondColon + 1);
        return new PagedRef(key, page, id);
    }

    private void editMessageTextAndMarkup(long chatId, Integer messageId, String text, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setReplyMarkup(markup);
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void clearInlineKeyboard(long chatId, Integer messageId) {
        if (messageId == null) {
            return;
        }
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setReplyMarkup(new InlineKeyboardMarkup(new ArrayList<>()));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendOrEditAddFood(long chatId, AddFoodSession session, String text, InlineKeyboardMarkup markup) {
        Integer messageId = session.getKeyboardMessageId();
        if (messageId != null) {
            editMessageTextAndMarkup(chatId, messageId, text, markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(markup);
        try {
            Message sent = execute(message);
            session.setKeyboardMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendOrEditEditFood(long chatId, EditFoodSession session, String text, InlineKeyboardMarkup markup) {
        Integer messageId = session.getKeyboardMessageId();
        if (messageId != null) {
            editMessageTextAndMarkup(chatId, messageId, text, markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(markup);
        try {
            Message sent = execute(message);
            session.setKeyboardMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup buildRecipeStepKeyboard(List<String> steps, String actionPrefix,
                                                          String doneCallback, InlineKeyboardButton extraButton,
                                                          Lang lang) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton edit = new InlineKeyboardButton("✏️ " + (i + 1));
            edit.setCallbackData(actionPrefix + "edit:" + i);
            row.add(edit);
            if (i > 0) {
                InlineKeyboardButton up = new InlineKeyboardButton("⬆️ " + (i + 1));
                up.setCallbackData(actionPrefix + "up:" + i);
                row.add(up);
            }
            if (i < steps.size() - 1) {
                InlineKeyboardButton down = new InlineKeyboardButton("⬇️ " + (i + 1));
                down.setCallbackData(actionPrefix + "down:" + i);
                row.add(down);
            }
            InlineKeyboardButton delete = new InlineKeyboardButton("🗑️ " + (i + 1));
            delete.setCallbackData(actionPrefix + "del:" + i);
            row.add(delete);
            rows.add(row);
        }
        InlineKeyboardButton done = new InlineKeyboardButton(Messages.get(lang, "btn.done_recipe"));
        done.setCallbackData(doneCallback);
        rows.add(extraButton != null ? List.of(done, extraButton) : List.of(done));
        return new InlineKeyboardMarkup(rows);
    }

    private String recipeStepManagerText(List<String> steps, Integer editingIndex, Lang lang) {
        StringBuilder builder = new StringBuilder();
        builder.append(Messages.get(lang, "recipe.manager.header")).append("\n");
        if (steps.isEmpty()) {
            builder.append(Messages.get(lang, "recipe.manager.empty"));
        } else {
            for (int i = 0; i < steps.size(); i++) {
                builder.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        }
        builder.append("\n");
        if (editingIndex != null) {
            builder.append(Messages.get(lang, "recipe.manager.editing_step", editingIndex + 1, RECIPE_STEP_CHAR_LIMIT));
        } else {
            builder.append(Messages.get(lang, "recipe.manager.prompt", RECIPE_STEP_CHAR_LIMIT));
        }
        return builder.toString();
    }

    private void applyRecipeStepAction(RecipeStepEditorState state, String verb, int index) {
        List<String> steps = state.getRecipeSteps();
        Integer editing = state.getEditingRecipeStepIndex();
        switch (verb) {
            case "edit":
                state.setEditingRecipeStepIndex(index);
                break;
            case "up":
                RecipeSteps.moveUp(steps, index);
                state.setEditingRecipeStepIndex(RecipeSteps.editingIndexAfterMoveUp(index, editing));
                break;
            case "down":
                RecipeSteps.moveDown(steps, index);
                state.setEditingRecipeStepIndex(RecipeSteps.editingIndexAfterMoveDown(index, editing));
                break;
            case "del":
                steps.remove(index);
                state.setEditingRecipeStepIndex(RecipeSteps.editingIndexAfterDelete(index, editing));
                break;
            default:
                break;
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.equals(CB_ADDFOOD_SCOPE_MINE) || data.equals(CB_ADDFOOD_SCOPE_GLOBAL)) {
            handleAddFoodScopeCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_CANCEL)) {
            handleAddFoodCancelCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_BACK)) {
            handleAddFoodBackCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_VIEW_FOODS_MINE) || data.equals(CB_VIEW_FOODS_GLOBAL)) {
            handleViewFoodsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_VIEW_FOODS_PAGE)) {
            handleViewFoodsPageCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_INGREDIENT)) {
            handleAddFoodIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_RECIPE_DONE)) {
            handleAddFoodRecipeDoneCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_RECIPE_STEP)) {
            handleAddFoodRecipeStepCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_REVIEW_EDIT)) {
            handleAddFoodReviewEditCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_CONFIRM)) {
            handleAddFoodConfirmCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_CATEGORY)) {
            handleAddFoodCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_TIME_PRESET)) {
            handleCookTimePresetCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_INGREDIENT)) {
            handleCookIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_COOK_SHOP_YES) || data.equals(CB_COOK_SHOP_NO)) {
            handleCookShopCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_CATEGORY)) {
            handleCookCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_RESULT_PAGE)) {
            handleCookResultPageCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_VIEW)) {
            handleCookViewCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_FIELD)) {
            handleEditFieldCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_CATEGORY)) {
            handleEditCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_INGREDIENT)) {
            handleEditIngredientCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_RECIPE_STEP)) {
            handleEditRecipeStepCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_RECIPE_CLEAR)) {
            handleEditRecipeClearCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_RECIPE_DONE)) {
            handleEditRecipeDoneCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_START)) {
            handleEditStartCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_CONFIRM_YES)) {
            handleDeleteConfirmYesCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_CONFIRM_NO)) {
            handleDeleteConfirmNoCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_DELETE_START)) {
            handleDeleteStartCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_SETTINGS)) {
            handleFoodSettingsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_VIEW)) {
            handleFoodViewCallback(callbackQuery, chatId, data);
        } else {
            answerCallback(callbackQuery.getId(), null, false);
        }
    }

    private void handleAddFoodIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.SELECTING_INGREDIENTS) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        if (data.equals(CB_ADDFOOD_INGREDIENT_DONE)) {
            if (session.getSelectedIngredients().isEmpty()) {
                answerCallback(callbackQuery.getId(), Messages.get(lang, "addfood.select_at_least_one"), true);
                return;
            }
            answerCallback(callbackQuery.getId(), null, false);
            if (session.isReturnToReview()) {
                session.setReturnToReview(false);
                session.setStep(AddFoodSession.Step.REVIEW);
                renderAddFoodReview(chatId, session, lang);
            } else {
                session.setStep(AddFoodSession.Step.AWAITING_RECIPE);
                session.getRecipeSteps().clear();
                session.setEditingRecipeStepIndex(null);
                renderAddFoodRecipeManager(chatId, session, lang);
            }
            return;
        }
        if (data.equals(CB_ADDFOOD_INGREDIENT_CLEAR)) {
            session.setIngredientFilter("");
            session.setIngredientPage(0);
            answerCallback(callbackQuery.getId(), null, false);
            renderAddFoodIngredientKeyboard(chatId, session, lang);
            return;
        }
        if (handleIngredientPageNav(session, data, CB_ADDFOOD_INGREDIENT)) {
            answerCallback(callbackQuery.getId(), null, false);
            renderAddFoodIngredientKeyboard(chatId, session, lang);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_ADDFOOD_INGREDIENT.length()));
        String ingredient = session.getCandidateIngredients().get(index);
        toggleAndAnswer(session.getSelectedIngredients(), ingredient, callbackQuery.getId(), lang);
        renderAddFoodIngredientKeyboard(chatId, session, lang);
    }

    private void handleAddFoodCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.SELECTING_CATEGORY) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_ADDFOOD_CATEGORY.length()));
        String category = FoodCategories.ALL.get(index);
        session.setCategory(category);
        session.getCandidateIngredients().clear();
        session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId, lang));
        answerCallback(callbackQuery.getId(), categoryLabel(category, lang), false);
        if (session.isReturnToReview()) {
            session.setReturnToReview(false);
            session.setStep(AddFoodSession.Step.REVIEW);
            renderAddFoodReview(chatId, session, lang);
        } else {
            session.setStep(AddFoodSession.Step.SELECTING_INGREDIENTS);
            renderAddFoodIngredientKeyboard(chatId, session, lang);
        }
    }

    private void renderAddFoodIngredientKeyboard(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = buildIngredientKeyboard(session, CB_ADDFOOD_INGREDIENT, CB_ADDFOOD_INGREDIENT_DONE,
                CB_ADDFOOD_INGREDIENT_CLEAR, lang, back);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ingredient_prompt"), markup);
    }

    private void renderAddFoodRecipeManager(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = buildRecipeStepKeyboard(session.getRecipeSteps(), CB_ADDFOOD_RECIPE_STEP,
                CB_ADDFOOD_RECIPE_DONE, back, lang);
        String text = recipeStepManagerText(session.getRecipeSteps(), session.getEditingRecipeStepIndex(), lang);
        sendOrEditAddFood(chatId, session, text, markup);
    }

    private void handleAddFoodRecipeStepCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        String action = data.substring(CB_ADDFOOD_RECIPE_STEP.length());
        int colon = action.indexOf(':');
        String verb = action.substring(0, colon);
        int index = Integer.parseInt(action.substring(colon + 1));
        if (index < 0 || index >= session.getRecipeSteps().size()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        applyRecipeStepAction(session, verb, index);
        answerCallback(callbackQuery.getId(), null, false);
        renderAddFoodRecipeManager(chatId, session, lang);
    }

    private void handleAddFoodRecipeDoneCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        session.setReturnToReview(false);
        session.setEditingRecipeStepIndex(null);
        session.setStep(AddFoodSession.Step.REVIEW);
        answerCallback(callbackQuery.getId(), null, false);
        renderAddFoodReview(chatId, session, lang);
    }

    private String formatAddFoodReview(AddFoodSession session, Lang lang) {
        String ingredients = session.getSelectedIngredients().stream()
                .map(i -> IngredientIcons.iconFor(i) + " " + IngredientTranslations.translate(i, lang))
                .collect(Collectors.joining(", "));
        String timeText = session.getPrepTimeMinutes() + " " + Messages.get(lang, "min_unit");
        String recipeText = session.getRecipeSteps().isEmpty()
                ? Messages.get(lang, "food.no_recipe")
                : RecipeSteps.join(session.getRecipeSteps());
        return Messages.get(lang, "addfood.review_header") + "\n\n"
                + "🍽️ " + session.getName() + "\n"
                + "🏷️ " + categoryLabel(session.getCategory(), lang) + "\n"
                + "⏱️ " + Messages.get(lang, "food.detail_time", timeText) + "\n"
                + "🧾 " + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n\n"
                + "📝 " + Messages.get(lang, "food.detail_recipe", recipeText) + "\n";
    }

    private void renderAddFoodReview(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton editName = new InlineKeyboardButton(Messages.get(lang, "edit.field.name"));
        editName.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "name");
        InlineKeyboardButton editTime = new InlineKeyboardButton(Messages.get(lang, "edit.field.time"));
        editTime.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "time");
        InlineKeyboardButton editCategory = new InlineKeyboardButton(Messages.get(lang, "edit.field.category"));
        editCategory.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "category");
        InlineKeyboardButton editIngredients = new InlineKeyboardButton(Messages.get(lang, "edit.field.ingredients"));
        editIngredients.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "ingredients");
        InlineKeyboardButton editRecipe = new InlineKeyboardButton(Messages.get(lang, "edit.field.recipe"));
        editRecipe.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "recipe");
        InlineKeyboardButton confirm = new InlineKeyboardButton(Messages.get(lang, "addfood.confirm_save"));
        confirm.setCallbackData(CB_ADDFOOD_CONFIRM);
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(
                List.of(editName, editTime),
                List.of(editCategory, editIngredients),
                List.of(editRecipe),
                List.of(confirm),
                List.of(back)));
        sendOrEditAddFood(chatId, session, formatAddFoodReview(session, lang), markup);
    }

    private void handleAddFoodReviewEditCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.REVIEW) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        String field = data.substring(CB_ADDFOOD_REVIEW_EDIT.length());
        answerCallback(callbackQuery.getId(), null, false);
        switch (field) {
            case "name":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.AWAITING_NAME);
                sendAddFoodNamePrompt(chatId, session, lang);
                break;
            case "time":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.AWAITING_PREP_TIME);
                sendAddFoodTimePrompt(chatId, session, lang);
                break;
            case "category":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.SELECTING_CATEGORY);
                sendAddFoodCategoryPrompt(chatId, session, lang);
                break;
            case "ingredients":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.SELECTING_INGREDIENTS);
                renderAddFoodIngredientKeyboard(chatId, session, lang);
                break;
            case "recipe":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.AWAITING_RECIPE);
                session.setEditingRecipeStepIndex(null);
                renderAddFoodRecipeManager(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    private void handleAddFoodConfirmCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.REVIEW) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        finalizeNewFood(chatId, session, lang);
    }

    private void finalizeNewFood(long chatId, AddFoodSession session, Lang lang) {
        Food food = new Food(UUID.randomUUID().toString(), session.getName(), session.getPrepTimeMinutes(),
                session.getCategory(), new ArrayList<>(session.getSelectedIngredients()), session.getOwnerChatId(),
                chatId, RecipeSteps.join(session.getRecipeSteps()), lang);
        foodRepository.add(food);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        addFoodSessions.remove(chatId);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "addfood.saved_message", formatFood(food, lang)));
    }

    private void handleCookIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        CookSession session = cookSessions.get(chatId);
        if (session == null || session.getStep() != CookSession.Step.SELECTING_INGREDIENTS) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        if (data.equals(CB_COOK_INGREDIENT_DONE)) {
            session.setStep(CookSession.Step.ASK_SHOPPING);
            answerCallback(callbackQuery.getId(), null, false);
            sendShoppingPrompt(chatId, lang);
            return;
        }
        if (data.equals(CB_COOK_INGREDIENT_CLEAR)) {
            session.setIngredientFilter("");
            session.setIngredientPage(0);
            answerCallback(callbackQuery.getId(), null, false);
            editCookIngredientKeyboard(chatId, session, lang);
            return;
        }
        if (handleIngredientPageNav(session, data, CB_COOK_INGREDIENT)) {
            answerCallback(callbackQuery.getId(), null, false);
            editCookIngredientKeyboard(chatId, session, lang);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_COOK_INGREDIENT.length()));
        String ingredient = session.getCandidateIngredients().get(index);
        toggleAndAnswer(session.getHaveIngredients(), ingredient, callbackQuery.getId(), lang);
        editCookIngredientKeyboard(chatId, session, lang);
    }

    private void handleCookShopCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        CookSession session = cookSessions.get(chatId);
        if (session == null || session.getStep() != CookSession.Step.ASK_SHOPPING) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        session.setCanShop(data.equals(CB_COOK_SHOP_YES));
        session.setStep(CookSession.Step.SELECTING_CATEGORY);
        answerCallback(callbackQuery.getId(), null, false);
        sendCategoryKeyboard(chatId, CB_COOK_CATEGORY, cookCategoryKeys(), lang,
                Messages.get(lang, "cook.ask_category_filter"));
    }

    private void handleCookCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        CookSession session = cookSessions.get(chatId);
        if (session == null || session.getStep() != CookSession.Step.SELECTING_CATEGORY) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_COOK_CATEGORY.length()));
        String chosen = cookCategoryKeys().get(index);
        String category = chosen.equals(CATEGORY_ANY) ? null : chosen;
        cookSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), categoryLabel(chosen, lang), false);

        CookResultContext ctx = new CookResultContext(session.getTimeMinutes(), session.getHaveIngredients(),
                session.isCanShop(), category);
        lastCookResults.put(chatId, ctx);
        sendCookResults(chatId, lang, ctx);
    }

    private List<Food> computeCookMatches(long chatId, Lang lang, CookResultContext ctx, boolean needsShopping) {
        List<Food> result = new ArrayList<>();
        for (Food food : foodRepository.findVisibleTo(chatId, lang)) {
            if (food.getPrepTimeMinutes() > ctx.getTimeMinutes()) {
                continue;
            }
            if (ctx.getCategory() != null && !ctx.getCategory().equalsIgnoreCase(food.getCategory())) {
                continue;
            }
            boolean hasEverything = food.getIngredients().stream()
                    .allMatch(ing -> effectivelyHas(ctx.getHaveIngredients(), ing));
            if (needsShopping) {
                if (!hasEverything && ctx.isCanShop()) {
                    result.add(food);
                }
            } else if (hasEverything) {
                result.add(food);
            }
        }
        return result;
    }

    private boolean effectivelyHas(Set<String> haveIngredients, String ingredient) {
        return PantryStaples.isStaple(ingredient) || haveIngredients.stream().anyMatch(h -> h.equalsIgnoreCase(ingredient));
    }

    private void addNonStapleIngredients(List<String> candidates, List<String> allIngredients) {
        for (String ingredient : allIngredients) {
            if (!PantryStaples.isStaple(ingredient)) {
                candidates.add(ingredient);
            }
        }
    }

    private void sendCookResults(long chatId, Lang lang, CookResultContext ctx) {
        List<Food> ready = computeCookMatches(chatId, lang, ctx, false);
        List<Food> shoppingNeeded = computeCookMatches(chatId, lang, ctx, true);

        if (ready.isEmpty() && shoppingNeeded.isEmpty()) {
            String key = ctx.isCanShop() ? "cook.nothing_matches" : "cook.nothing_matches_no_shop";
            sendWithMainMenu(chatId, lang, Messages.get(lang, key));
            return;
        }
        if (!ready.isEmpty()) {
            sendCookResultPage(chatId, lang, GROUP_READY, 0, null);
        }
        if (!shoppingNeeded.isEmpty()) {
            sendCookResultPage(chatId, lang, GROUP_SHOP, 0, null);
        }
    }

    private void sendCookResultPage(long chatId, Lang lang, String group, int page, Integer editMessageId) {
        CookResultContext ctx = lastCookResults.get(chatId);
        if (ctx == null) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "selection_expired"));
            return;
        }
        List<Food> matches = computeCookMatches(chatId, lang, ctx, group.equals(GROUP_SHOP));
        if (matches.isEmpty()) {
            return;
        }
        String headerKey = group.equals(GROUP_SHOP) ? "cook.shopping_header" : "cook.ready_header";
        String icon = group.equals(GROUP_SHOP) ? "🍽️" : "✅";

        int totalPages = Paginator.totalPages(matches.size(), FOODS_PER_PAGE);
        int clampedPage = Paginator.clampPage(page, totalPages);
        List<Food> pageItems = Paginator.pageSlice(matches, clampedPage, FOODS_PER_PAGE);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Food food : pageItems) {
            String label = icon + " " + FoodNameTranslations.translate(food.getName(), lang) + " ("
                    + food.getPrepTimeMinutes() + " " + Messages.get(lang, "min_unit") + ")";
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(CB_COOK_VIEW + group + ":" + clampedPage + ":" + food.getId());
            rows.add(List.of(button));
        }

        if (totalPages > 1) {
            List<InlineKeyboardButton> navRow = new ArrayList<>();
            if (clampedPage > 0) {
                InlineKeyboardButton prev = new InlineKeyboardButton("◀️");
                prev.setCallbackData(CB_COOK_RESULT_PAGE + group + ":" + (clampedPage - 1));
                navRow.add(prev);
            }
            InlineKeyboardButton indicator = new InlineKeyboardButton((clampedPage + 1) + "/" + totalPages);
            indicator.setCallbackData("noop");
            navRow.add(indicator);
            if (clampedPage < totalPages - 1) {
                InlineKeyboardButton next = new InlineKeyboardButton("▶️");
                next.setCallbackData(CB_COOK_RESULT_PAGE + group + ":" + (clampedPage + 1));
                navRow.add(next);
            }
            rows.add(navRow);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);

        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, Messages.get(lang, headerKey), markup);
            return;
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, headerKey));
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCookResultPageCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String remainder = data.substring(CB_COOK_RESULT_PAGE.length());
        int separatorIndex = remainder.lastIndexOf(':');
        String group = remainder.substring(0, separatorIndex);
        int page = Integer.parseInt(remainder.substring(separatorIndex + 1));
        answerCallback(callbackQuery.getId(), null, false);
        sendCookResultPage(chatId, lang, group, page, callbackQuery.getMessage().getMessageId());
    }

    private void handleCookViewCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_COOK_VIEW.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        CookResultContext ctx = lastCookResults.get(chatId);
        if (foodOpt.isEmpty() || ctx == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        String text = formatCookFoodDetail(foodOpt.get(), ctx, lang);
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_COOK_RESULT_PAGE + ref.key() + ":" + ref.page());
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(), text,
                new InlineKeyboardMarkup(List.of(List.of(back))));
    }

    private String formatCookFoodDetail(Food food, CookResultContext ctx, Lang lang) {
        List<String> have = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String ingredient : food.getIngredients()) {
            boolean hasIt = effectivelyHas(ctx.getHaveIngredients(), ingredient);
            String formatted = IngredientIcons.iconFor(ingredient) + " " + IngredientTranslations.translate(ingredient, lang);
            if (hasIt) {
                have.add(formatted);
            } else {
                missing.add(formatted);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("🍽️ ").append(FoodNameTranslations.translate(food.getName(), lang)).append("\n");
        builder.append("🏷️ ").append(categoryLabel(food.getCategory(), lang)).append("\n");
        builder.append("⏱️ ").append(food.getPrepTimeMinutes()).append(" ").append(Messages.get(lang, "min_unit"))
                .append("\n\n");

        if (missing.isEmpty()) {
            builder.append(Messages.get(lang, "cook.detail_have_everything")).append("\n");
        } else {
            if (!have.isEmpty()) {
                builder.append(Messages.get(lang, "cook.detail_have")).append("\n");
                for (String item : have) {
                    builder.append("- ").append(item).append("\n");
                }
                builder.append("\n");
            }
            builder.append(Messages.get(lang, "cook.detail_need")).append("\n");
            for (String item : missing) {
                builder.append("- ").append(item).append("\n");
            }
        }

        String recipeText = (food.getRecipe() == null || food.getRecipe().isBlank())
                ? Messages.get(lang, "food.no_recipe")
                : food.getRecipe();
        builder.append("\n📝 ").append(Messages.get(lang, "food.detail_recipe", recipeText)).append("\n");

        return builder.toString();
    }

    private void handleEditStartCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_EDIT_START.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        addFoodSessions.remove(chatId);
        cookSessions.remove(chatId);
        editSessions.put(chatId, new EditFoodSession(ref.id()));
        answerCallback(callbackQuery.getId(), null, false);
        sendEditFieldChoiceKeyboard(chatId, lang, ref, callbackQuery.getMessage().getMessageId());
    }

    private void sendEditFieldChoiceKeyboard(long chatId, Lang lang, PagedRef ref, Integer editMessageId) {
        InlineKeyboardButton name = new InlineKeyboardButton(Messages.get(lang, "edit.field.name"));
        name.setCallbackData(CB_FOOD_EDIT_FIELD + "name");
        InlineKeyboardButton time = new InlineKeyboardButton(Messages.get(lang, "edit.field.time"));
        time.setCallbackData(CB_FOOD_EDIT_FIELD + "time");
        InlineKeyboardButton category = new InlineKeyboardButton(Messages.get(lang, "edit.field.category"));
        category.setCallbackData(CB_FOOD_EDIT_FIELD + "category");
        InlineKeyboardButton ingredients = new InlineKeyboardButton(Messages.get(lang, "edit.field.ingredients"));
        ingredients.setCallbackData(CB_FOOD_EDIT_FIELD + "ingredients");
        InlineKeyboardButton recipe = new InlineKeyboardButton(Messages.get(lang, "edit.field.recipe"));
        recipe.setCallbackData(CB_FOOD_EDIT_FIELD + "recipe");
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_FOOD_SETTINGS + ref.key() + ":" + ref.page() + ":" + ref.id());
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                List.of(List.of(name, time), List.of(category, ingredients), List.of(recipe), List.of(back)));
        editMessageTextAndMarkup(chatId, editMessageId, Messages.get(lang, "edit.choose_field"), markup);
    }

    private void handleEditFieldCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        Food food = foodOpt.get();
        String field = data.substring(CB_FOOD_EDIT_FIELD.length());
        answerCallback(callbackQuery.getId(), null, false);
        switch (field) {
            case "name":
                session.setStep(EditFoodSession.Step.EDITING_NAME);
                send(chatId, Messages.get(lang, "edit.ask_name"));
                break;
            case "time":
                session.setStep(EditFoodSession.Step.EDITING_TIME);
                send(chatId, Messages.get(lang, "edit.ask_time"));
                break;
            case "category":
                session.setStep(EditFoodSession.Step.EDITING_CATEGORY);
                sendCategoryKeyboard(chatId, CB_FOOD_EDIT_CATEGORY, FoodCategories.ALL, lang,
                        Messages.get(lang, "edit.ask_category"));
                break;
            case "ingredients":
                session.setStep(EditFoodSession.Step.EDITING_INGREDIENTS);
                session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId, lang));
                for (String ingredient : food.getIngredients()) {
                    boolean exists = session.getCandidateIngredients().stream()
                            .anyMatch(i -> i.equalsIgnoreCase(ingredient));
                    if (!exists) {
                        session.getCandidateIngredients().add(ingredient);
                    }
                    session.getSelectedIngredients().add(ingredient);
                }
                sendEditIngredientKeyboard(chatId, session, lang);
                break;
            case "recipe":
                session.setStep(EditFoodSession.Step.EDITING_RECIPE);
                session.getRecipeSteps().clear();
                session.getRecipeSteps().addAll(RecipeSteps.parse(food.getRecipe()));
                session.setEditingRecipeStepIndex(null);
                session.setKeyboardMessageId(callbackQuery.getMessage().getMessageId());
                renderEditRecipeManager(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    private void renderEditRecipeManager(long chatId, EditFoodSession session, Lang lang) {
        InlineKeyboardButton clear = new InlineKeyboardButton(Messages.get(lang, "btn.clear_recipe"));
        clear.setCallbackData(CB_FOOD_EDIT_RECIPE_CLEAR);
        InlineKeyboardMarkup markup = buildRecipeStepKeyboard(session.getRecipeSteps(), CB_FOOD_EDIT_RECIPE_STEP,
                CB_FOOD_EDIT_RECIPE_DONE, clear, lang);
        String text = recipeStepManagerText(session.getRecipeSteps(), session.getEditingRecipeStepIndex(), lang);
        sendOrEditEditFood(chatId, session, text, markup);
    }

    private void handleEditRecipeStepCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        String action = data.substring(CB_FOOD_EDIT_RECIPE_STEP.length());
        int colon = action.indexOf(':');
        String verb = action.substring(0, colon);
        int index = Integer.parseInt(action.substring(colon + 1));
        if (index < 0 || index >= session.getRecipeSteps().size()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        applyRecipeStepAction(session, verb, index);
        answerCallback(callbackQuery.getId(), null, false);
        renderEditRecipeManager(chatId, session, lang);
    }

    private void handleEditRecipeDoneCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        Food food = foodOpt.get();
        Food updated = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(), food.getCategory(),
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(),
                RecipeSteps.join(session.getRecipeSteps()), food.getLanguage());
        foodRepository.update(updated);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    private void handleEditRecipeClearCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        Food food = foodOpt.get();
        Food updated = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(), food.getCategory(),
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), null, food.getLanguage());
        foodRepository.update(updated);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    private void handleEditCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_CATEGORY) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_FOOD_EDIT_CATEGORY.length()));
        String category = FoodCategories.ALL.get(index);
        Food food = foodOpt.get();
        Food updated = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(), category,
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe(),
                food.getLanguage());
        foodRepository.update(updated);
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), categoryLabel(category, lang), false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    private void handleEditIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_INGREDIENTS) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        if (data.equals(CB_FOOD_EDIT_INGREDIENT_DONE)) {
            if (session.getSelectedIngredients().isEmpty()) {
                answerCallback(callbackQuery.getId(), Messages.get(lang, "addfood.select_at_least_one"), true);
                return;
            }
            Food food = foodOpt.get();
            Food updated = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(), food.getCategory(),
                    new ArrayList<>(session.getSelectedIngredients()), food.getOwnerChatId(), food.getCreatedByChatId(),
                    food.getRecipe(), food.getLanguage());
            foodRepository.update(updated);
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "addfood.saved_toast"), false);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
            return;
        }
        if (data.equals(CB_FOOD_EDIT_INGREDIENT_CLEAR)) {
            session.setIngredientFilter("");
            session.setIngredientPage(0);
            answerCallback(callbackQuery.getId(), null, false);
            editEditIngredientKeyboard(chatId, session, lang);
            return;
        }
        if (handleIngredientPageNav(session, data, CB_FOOD_EDIT_INGREDIENT)) {
            answerCallback(callbackQuery.getId(), null, false);
            editEditIngredientKeyboard(chatId, session, lang);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_FOOD_EDIT_INGREDIENT.length()));
        String ingredient = session.getCandidateIngredients().get(index);
        toggleAndAnswer(session.getSelectedIngredients(), ingredient, callbackQuery.getId(), lang);
        editEditIngredientKeyboard(chatId, session, lang);
    }

    private void handleDeleteStartCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_DELETE_START.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        Food food = foodOpt.get();
        InlineKeyboardButton yes = new InlineKeyboardButton(Messages.get(lang, "delete.confirm_yes"));
        yes.setCallbackData(CB_FOOD_DELETE_CONFIRM_YES + ref.id());
        InlineKeyboardButton no = new InlineKeyboardButton(Messages.get(lang, "delete.confirm_no"));
        no.setCallbackData(CB_FOOD_DELETE_CONFIRM_NO + ref.key() + ":" + ref.page() + ":" + ref.id());
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(),
                Messages.get(lang, "delete.confirm", FoodNameTranslations.translate(food.getName(), lang)),
                new InlineKeyboardMarkup(List.of(List.of(yes, no))));
    }

    private void handleDeleteConfirmYesCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String foodId = data.substring(CB_FOOD_DELETE_CONFIRM_YES.length());
        Optional<Food> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        String name = FoodNameTranslations.translate(foodOpt.get().getName(), lang);
        foodRepository.delete(foodId);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "delete.done", name));
    }

    private void handleDeleteConfirmNoCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_DELETE_CONFIRM_NO.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        InlineKeyboardButton edit = new InlineKeyboardButton(Messages.get(lang, "btn.edit"));
        edit.setCallbackData(CB_FOOD_EDIT_START + ref.key() + ":" + ref.page() + ":" + ref.id());
        InlineKeyboardButton delete = new InlineKeyboardButton(Messages.get(lang, "btn.delete"));
        delete.setCallbackData(CB_FOOD_DELETE_START + ref.key() + ":" + ref.page() + ":" + ref.id());
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_FOOD_VIEW + ref.key() + ":" + ref.page() + ":" + ref.id());
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(),
                Messages.get(lang, "settings.choose_action"),
                new InlineKeyboardMarkup(List.of(List.of(edit, delete), List.of(back))));
    }

    private List<String> cookCategoryKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(CATEGORY_ANY);
        keys.addAll(FoodCategories.ALL);
        return keys;
    }

    private String categoryLabel(String key, Lang lang) {
        if (key == null || key.equals(CATEGORY_ANY)) {
            return Messages.get(lang, "category.any");
        }
        return Messages.get(lang, "category." + key);
    }

    private String formatFood(Food food, Lang lang) {
        String ingredients = food.getIngredients().stream()
                .map(i -> IngredientIcons.iconFor(i) + " " + IngredientTranslations.translate(i, lang))
                .collect(Collectors.joining(", "));
        return FoodNameTranslations.translate(food.getName(), lang) + " [" + categoryLabel(food.getCategory(), lang)
                + "] (" + food.getPrepTimeMinutes() + " " + Messages.get(lang, "min_unit") + ") - " + ingredients;
    }

    private void sendCookTimePrompt(long chatId, Lang lang) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        for (int i = 0; i < COOK_TIME_PRESET_KEYS.length; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton(Messages.get(lang, COOK_TIME_PRESET_KEYS[i]));
            button.setCallbackData(CB_COOK_TIME_PRESET + COOK_TIME_PRESET_MINUTES[i]);
            currentRow.add(button);
            if (currentRow.size() == 2) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "cook.ask_time"));
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCookTimePresetCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        CookSession session = cookSessions.get(chatId);
        if (session == null || session.getStep() != CookSession.Step.AWAITING_TIME) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        int minutes = Integer.parseInt(data.substring(CB_COOK_TIME_PRESET.length()));
        session.setTimeMinutes(minutes);
        session.setStep(CookSession.Step.SELECTING_INGREDIENTS);
        addNonStapleIngredients(session.getCandidateIngredients(), foodRepository.findAllIngredients(chatId, lang));
        answerCallback(callbackQuery.getId(), null, false);
        sendCookIngredientKeyboard(chatId, session, lang);
    }

    private void sendShoppingPrompt(long chatId, Lang lang) {
        InlineKeyboardButton yes = new InlineKeyboardButton(Messages.get(lang, "yes"));
        yes.setCallbackData(CB_COOK_SHOP_YES);
        InlineKeyboardButton no = new InlineKeyboardButton(Messages.get(lang, "no"));
        no.setCallbackData(CB_COOK_SHOP_NO);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "cook.ask_shopping"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(yes, no))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendCategoryKeyboard(long chatId, String callbackPrefix, List<String> categoryKeys, Lang lang,
                                       String prompt) {
        sendCategoryKeyboard(chatId, callbackPrefix, categoryKeys, lang, prompt, null, null);
    }

    private void sendCategoryKeyboard(long chatId, String callbackPrefix, List<String> categoryKeys, Lang lang,
                                       String prompt, Integer editMessageId, InlineKeyboardButton backButton) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        for (int i = 0; i < categoryKeys.size(); i++) {
            InlineKeyboardButton button = new InlineKeyboardButton(categoryLabel(categoryKeys.get(i), lang));
            button.setCallbackData(callbackPrefix + i);
            currentRow.add(button);
            if (currentRow.size() == 2) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }
        if (backButton != null) {
            rows.add(List.of(backButton));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);

        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, prompt, markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), prompt);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup buildIngredientKeyboard(IngredientPickerState state, String prefix,
                                                          String doneCallback, String clearCallback, Lang lang) {
        return buildIngredientKeyboard(state, prefix, doneCallback, clearCallback, lang, null);
    }

    private InlineKeyboardMarkup buildIngredientKeyboard(IngredientPickerState state, String prefix,
                                                          String doneCallback, String clearCallback, Lang lang,
                                                          InlineKeyboardButton backButton) {
        List<String> candidates = state.getCandidateIngredients();
        Set<String> selected = state.getSelectedIngredients();
        String filter = state.getIngredientFilter();
        String needle = (filter == null) ? "" : filter.toLowerCase();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (!needle.isEmpty()) {
            InlineKeyboardButton filterLabel = new InlineKeyboardButton("🔍 \"" + filter + "\"");
            filterLabel.setCallbackData("noop");
            InlineKeyboardButton clear = new InlineKeyboardButton("❌");
            clear.setCallbackData(clearCallback);
            rows.add(List.of(filterLabel, clear));
        }

        List<String> ordered = IngredientSearch.orderedCandidates(candidates, selected, filter);
        int totalPages = Paginator.totalPages(ordered.size(), INGREDIENTS_PER_PAGE);
        int page = Paginator.clampPage(state.getIngredientPage(), totalPages);
        List<String> pageItems = Paginator.pageSlice(ordered, page, INGREDIENTS_PER_PAGE);

        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        for (String name : pageItems) {
            int index = candidates.indexOf(name);
            boolean isSelected = selected.contains(name);
            String label = (isSelected ? "✅ " : "⬜ ") + IngredientIcons.iconFor(name) + " "
                    + IngredientTranslations.translate(name, lang);
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(prefix + index);
            currentRow.add(button);
            if (currentRow.size() == 2) {
                rows.add(currentRow);
                currentRow = new ArrayList<>();
            }
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        if (totalPages > 1) {
            List<InlineKeyboardButton> navRow = new ArrayList<>();
            if (page > 0) {
                InlineKeyboardButton prev = new InlineKeyboardButton("◀️");
                prev.setCallbackData(prefix + INGREDIENT_PAGE_INFIX + (page - 1));
                navRow.add(prev);
            }
            InlineKeyboardButton indicator = new InlineKeyboardButton((page + 1) + "/" + totalPages);
            indicator.setCallbackData("noop");
            navRow.add(indicator);
            if (page < totalPages - 1) {
                InlineKeyboardButton next = new InlineKeyboardButton("▶️");
                next.setCallbackData(prefix + INGREDIENT_PAGE_INFIX + (page + 1));
                navRow.add(next);
            }
            rows.add(navRow);
        }

        InlineKeyboardButton doneButton = new InlineKeyboardButton(
                Messages.get(lang, "done_button", selected.size()));
        doneButton.setCallbackData(doneCallback);
        rows.add(List.of(doneButton));
        if (backButton != null) {
            rows.add(List.of(backButton));
        }
        return new InlineKeyboardMarkup(rows);
    }

    private void sendCookIngredientKeyboard(long chatId, CookSession session, Lang lang) {
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "cook.ingredient_prompt"));
        message.setReplyMarkup(buildIngredientKeyboard(session, CB_COOK_INGREDIENT, CB_COOK_INGREDIENT_DONE,
                CB_COOK_INGREDIENT_CLEAR, lang));
        try {
            Message sent = execute(message);
            session.setKeyboardMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editCookIngredientKeyboard(long chatId, CookSession session, Lang lang) {
        if (session.getKeyboardMessageId() == null) {
            sendCookIngredientKeyboard(chatId, session, lang);
            return;
        }
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(session.getKeyboardMessageId());
        edit.setReplyMarkup(buildIngredientKeyboard(session, CB_COOK_INGREDIENT, CB_COOK_INGREDIENT_DONE,
                CB_COOK_INGREDIENT_CLEAR, lang));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendEditIngredientKeyboard(long chatId, EditFoodSession session, Lang lang) {
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "edit.ingredient_prompt"));
        message.setReplyMarkup(buildIngredientKeyboard(session, CB_FOOD_EDIT_INGREDIENT, CB_FOOD_EDIT_INGREDIENT_DONE,
                CB_FOOD_EDIT_INGREDIENT_CLEAR, lang));
        try {
            Message sent = execute(message);
            session.setKeyboardMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editEditIngredientKeyboard(long chatId, EditFoodSession session, Lang lang) {
        if (session.getKeyboardMessageId() == null) {
            sendEditIngredientKeyboard(chatId, session, lang);
            return;
        }
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(session.getKeyboardMessageId());
        edit.setReplyMarkup(buildIngredientKeyboard(session, CB_FOOD_EDIT_INGREDIENT, CB_FOOD_EDIT_INGREDIENT_DONE,
                CB_FOOD_EDIT_INGREDIENT_CLEAR, lang));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendFoodListPage(long chatId, Lang lang, String scope, int page, Integer editMessageId) {
        List<Food> foods = scope.equals(SCOPE_MINE)
                ? foodRepository.findOwnedBy(chatId, lang)
                : foodRepository.findGlobal(lang);
        String headerKey = scope.equals(SCOPE_MINE) ? "foods.header.mine" : "foods.header.global";

        if (foods.isEmpty()) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "foods.none"));
            return;
        }

        int totalPages = Paginator.totalPages(foods.size(), FOODS_PER_PAGE);
        int clampedPage = Paginator.clampPage(page, totalPages);
        List<Food> pageItems = Paginator.pageSlice(foods, clampedPage, FOODS_PER_PAGE);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Food food : pageItems) {
            String label = FoodNameTranslations.translate(food.getName(), lang) + " (" + food.getPrepTimeMinutes()
                    + " " + Messages.get(lang, "min_unit") + ")";
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(CB_FOOD_VIEW + scope + ":" + clampedPage + ":" + food.getId());
            rows.add(List.of(button));
        }

        if (totalPages > 1) {
            List<InlineKeyboardButton> navRow = new ArrayList<>();
            if (clampedPage > 0) {
                InlineKeyboardButton prev = new InlineKeyboardButton("◀️");
                prev.setCallbackData(CB_VIEW_FOODS_PAGE + scope + ":" + (clampedPage - 1));
                navRow.add(prev);
            }
            InlineKeyboardButton pageIndicator = new InlineKeyboardButton((clampedPage + 1) + "/" + totalPages);
            pageIndicator.setCallbackData("noop");
            navRow.add(pageIndicator);
            if (clampedPage < totalPages - 1) {
                InlineKeyboardButton next = new InlineKeyboardButton("▶️");
                next.setCallbackData(CB_VIEW_FOODS_PAGE + scope + ":" + (clampedPage + 1));
                navRow.add(next);
            }
            rows.add(navRow);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);

        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, Messages.get(lang, headerKey), markup);
            return;
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, headerKey));
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String formatFoodDetail(Food food, Lang lang) {
        String ingredients = food.getIngredients().stream()
                .map(i -> IngredientIcons.iconFor(i) + " " + IngredientTranslations.translate(i, lang))
                .collect(Collectors.joining(", "));
        String timeText = food.getPrepTimeMinutes() + " " + Messages.get(lang, "min_unit");
        String recipeText = (food.getRecipe() == null || food.getRecipe().isBlank())
                ? Messages.get(lang, "food.no_recipe")
                : food.getRecipe();
        return "🍽️ " + FoodNameTranslations.translate(food.getName(), lang) + "\n"
                + "🏷️ " + Messages.get(lang, "food.detail_category", categoryLabel(food.getCategory(), lang)) + "\n"
                + "⏱️ " + Messages.get(lang, "food.detail_time", timeText) + "\n"
                + "🧾 " + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n\n"
                + "📝 " + Messages.get(lang, "food.detail_recipe", recipeText) + "\n";
    }

    private void handleFoodViewCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_VIEW.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Food food = foodOpt.get();
        answerCallback(callbackQuery.getId(), null, false);
        String text = formatFoodDetail(food, lang);
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_VIEW_FOODS_PAGE + ref.key() + ":" + ref.page());
        row.add(back);
        if (canModify(food, chatId)) {
            InlineKeyboardButton settings = new InlineKeyboardButton(Messages.get(lang, "btn.settings"));
            settings.setCallbackData(CB_FOOD_SETTINGS + ref.key() + ":" + ref.page() + ":" + ref.id());
            row.add(settings);
        }
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(), text,
                new InlineKeyboardMarkup(List.of(row)));
    }

    private void handleFoodSettingsCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_SETTINGS.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        InlineKeyboardButton edit = new InlineKeyboardButton(Messages.get(lang, "btn.edit"));
        edit.setCallbackData(CB_FOOD_EDIT_START + ref.key() + ":" + ref.page() + ":" + ref.id());
        InlineKeyboardButton delete = new InlineKeyboardButton(Messages.get(lang, "btn.delete"));
        delete.setCallbackData(CB_FOOD_DELETE_START + ref.key() + ":" + ref.page() + ":" + ref.id());
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_FOOD_VIEW + ref.key() + ":" + ref.page() + ":" + ref.id());
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(),
                Messages.get(lang, "settings.choose_action"),
                new InlineKeyboardMarkup(List.of(List.of(edit, delete), List.of(back))));
    }

    private ReplyKeyboardMarkup mainMenuKeyboard(Lang lang) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(Messages.get(lang, "btn.add_food"));
        row1.add(Messages.get(lang, "btn.all_foods"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(Messages.get(lang, "btn.what_can_cook"));
        row2.add(Messages.get(lang, "btn.change_lang"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(Messages.get(lang, "btn.help"));
        row3.add(Messages.get(lang, "btn.feedback"));
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private void sendWithMainMenu(long chatId, Lang lang, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(mainMenuKeyboard(lang));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void send(long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void answerCallback(String callbackId, String text, boolean alert) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        answer.setText(text);
        answer.setShowAlert(alert);
        try {
            execute(answer);
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
