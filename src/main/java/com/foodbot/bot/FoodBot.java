package com.foodbot.bot;

import com.foodbot.food.AddFoodSession;
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
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String CB_FOOD_DELETE_START = "fd:";
    private static final String CB_FOOD_DELETE_CONFIRM_YES = "fdy:";
    private static final String CB_FOOD_DELETE_CONFIRM_NO = "fdn:";
    private static final String CB_ADDFOOD_RECIPE_SKIP = "afrskip";
    private static final String CB_FOOD_VIEW = "fv:";
    private static final String CB_FOOD_SETTINGS = "fset:";

    private final String token;
    private final String username;
    private final Long superAdminChatId;
    private final FoodRepository foodRepository = new FoodRepository();
    private final LanguageRepository languageRepository = new LanguageRepository();
    private final Map<Long, AddFoodSession> addFoodSessions = new ConcurrentHashMap<>();
    private final Map<Long, CookSession> cookSessions = new ConcurrentHashMap<>();
    private final Map<Long, EditFoodSession> editSessions = new ConcurrentHashMap<>();

    public FoodBot(String token, String username, Long superAdminChatId) {
        this.token = token;
        this.username = username;
        this.superAdminChatId = superAdminChatId;
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
        } else if (text.equalsIgnoreCase("/whoami")) {
            String handle = update.getMessage().getFrom().getUserName();
            String displayHandle = (handle != null && !handle.isBlank())
                    ? "@" + handle
                    : Messages.get(lang, "whoami.no_username");
            send(chatId, Messages.get(lang, "whoami.reply", String.valueOf(chatId), displayHandle));
        } else if (text.equals(Messages.get(lang, "btn.change_lang"))) {
            sendLanguagePrompt(chatId);
        } else if (text.equalsIgnoreCase("/addfood") || text.equals(Messages.get(lang, "btn.add_food"))) {
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            addFoodSessions.put(chatId, new AddFoodSession());
            sendAddFoodScopePrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/cancel")) {
            addFoodSessions.remove(chatId);
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "cancelled"));
        } else if (isSuperAdmin(chatId)
                && (text.equalsIgnoreCase("/menu") || text.equals(Messages.get(lang, "btn.all_foods")))) {
            sendViewFoodsPrompt(chatId, lang);
        } else if (text.equalsIgnoreCase("/cook") || text.equals(Messages.get(lang, "btn.what_can_cook"))) {
            addFoodSessions.remove(chatId);
            editSessions.remove(chatId);
            cookSessions.put(chatId, new CookSession());
            send(chatId, Messages.get(lang, "cook.ask_time"));
        } else if (addFoodSessions.containsKey(chatId)) {
            handleAddFoodText(chatId, text, lang);
        } else if (cookSessions.containsKey(chatId)) {
            handleCookText(chatId, text, lang);
        } else if (editSessions.containsKey(chatId)) {
            handleEditText(chatId, text, lang);
        } else {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "fallback"));
        }
    }

    private Lang lang(long chatId) {
        Lang lang = languageRepository.get(chatId);
        return lang != null ? lang : Lang.EN;
    }

    private boolean isSuperAdmin(long chatId) {
        return superAdminChatId != null && superAdminChatId == chatId;
    }

    private boolean canModify(Food food, long chatId) {
        return isSuperAdmin(chatId) || (food.getCreatedByChatId() != null && food.getCreatedByChatId() == chatId);
    }

    private void sendLanguagePrompt(long chatId) {
        InlineKeyboardButton en = new InlineKeyboardButton("🇬🇧 English");
        en.setCallbackData(CB_LANG + Lang.EN.name());
        InlineKeyboardButton fa = new InlineKeyboardButton("🇮🇷 فارسی");
        fa.setCallbackData(CB_LANG + Lang.FA.name());
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(Lang.EN, "lang.prompt"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(en, fa))));
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

    private void sendAddFoodScopePrompt(long chatId, Lang lang) {
        InlineKeyboardButton mine = new InlineKeyboardButton(Messages.get(lang, "scope.mine"));
        mine.setCallbackData(CB_ADDFOOD_SCOPE_MINE);
        InlineKeyboardButton global = new InlineKeyboardButton(Messages.get(lang, "scope.global"));
        global.setCallbackData(CB_ADDFOOD_SCOPE_GLOBAL);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "addfood.ask_scope"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(mine, global))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
        send(chatId, Messages.get(lang, "addfood.ask_name"));
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
                session.setStep(AddFoodSession.Step.AWAITING_PREP_TIME);
                send(chatId, Messages.get(lang, "addfood.ask_time", text));
                break;

            case AWAITING_PREP_TIME:
                Integer minutes = parseMinutes(text);
                if (minutes == null) {
                    send(chatId, Messages.get(lang, "addfood.invalid_time"));
                    return;
                }
                session.setPrepTimeMinutes(minutes);
                session.setStep(AddFoodSession.Step.SELECTING_CATEGORY);
                sendCategoryKeyboard(chatId, CB_ADDFOOD_CATEGORY, FoodCategories.ALL, lang,
                        Messages.get(lang, "addfood.ask_category"));
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
                editAddFoodIngredientKeyboard(chatId, session, lang);
                break;

            case AWAITING_RECIPE:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                session.setRecipe(text);
                finalizeNewFood(chatId, session, lang);
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
                session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId));
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
                        food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe());
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
                        food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe());
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
                Food updatedRecipe = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(),
                        food.getCategory(), food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(),
                        text);
                foodRepository.update(updatedRecipe);
                editSessions.remove(chatId);
                sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updatedRecipe, lang)));
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

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        if (data.equals(CB_ADDFOOD_SCOPE_MINE) || data.equals(CB_ADDFOOD_SCOPE_GLOBAL)) {
            handleAddFoodScopeCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_VIEW_FOODS_MINE) || data.equals(CB_VIEW_FOODS_GLOBAL)) {
            handleViewFoodsCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_VIEW_FOODS_PAGE)) {
            handleViewFoodsPageCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_INGREDIENT)) {
            handleAddFoodIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_ADDFOOD_RECIPE_SKIP)) {
            handleAddFoodRecipeSkipCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_ADDFOOD_CATEGORY)) {
            handleAddFoodCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_INGREDIENT)) {
            handleCookIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_COOK_SHOP_YES) || data.equals(CB_COOK_SHOP_NO)) {
            handleCookShopCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_COOK_CATEGORY)) {
            handleCookCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_FIELD)) {
            handleEditFieldCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_CATEGORY)) {
            handleEditCategoryCallback(callbackQuery, chatId, data);
        } else if (data.startsWith(CB_FOOD_EDIT_INGREDIENT)) {
            handleEditIngredientCallback(callbackQuery, chatId, data);
        } else if (data.equals(CB_FOOD_EDIT_RECIPE_CLEAR)) {
            handleEditRecipeClearCallback(callbackQuery, chatId, data);
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
            session.setStep(AddFoodSession.Step.AWAITING_RECIPE);
            answerCallback(callbackQuery.getId(), null, false);
            sendAddFoodRecipePrompt(chatId, lang);
            return;
        }
        if (data.equals(CB_ADDFOOD_INGREDIENT_CLEAR)) {
            session.setIngredientFilter("");
            session.setIngredientPage(0);
            answerCallback(callbackQuery.getId(), null, false);
            editAddFoodIngredientKeyboard(chatId, session, lang);
            return;
        }
        if (handleIngredientPageNav(session, data, CB_ADDFOOD_INGREDIENT)) {
            answerCallback(callbackQuery.getId(), null, false);
            editAddFoodIngredientKeyboard(chatId, session, lang);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_ADDFOOD_INGREDIENT.length()));
        String ingredient = session.getCandidateIngredients().get(index);
        toggleAndAnswer(session.getSelectedIngredients(), ingredient, callbackQuery.getId(), lang);
        editAddFoodIngredientKeyboard(chatId, session, lang);
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
        session.setStep(AddFoodSession.Step.SELECTING_INGREDIENTS);
        session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId));
        answerCallback(callbackQuery.getId(), categoryLabel(category, lang), false);
        sendAddFoodIngredientKeyboard(chatId, session, lang);
    }

    private void sendAddFoodRecipePrompt(long chatId, Lang lang) {
        InlineKeyboardButton skip = new InlineKeyboardButton(Messages.get(lang, "btn.skip"));
        skip.setCallbackData(CB_ADDFOOD_RECIPE_SKIP);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "addfood.ask_recipe"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(skip))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleAddFoodRecipeSkipCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        finalizeNewFood(chatId, session, lang);
    }

    private void finalizeNewFood(long chatId, AddFoodSession session, Lang lang) {
        Food food = new Food(UUID.randomUUID().toString(), session.getName(), session.getPrepTimeMinutes(),
                session.getCategory(), new ArrayList<>(session.getSelectedIngredients()), session.getOwnerChatId(),
                chatId, session.getRecipe());
        foodRepository.add(food);
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
        session.setCategory(chosen.equals(CATEGORY_ANY) ? null : chosen);
        cookSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), categoryLabel(chosen, lang), false);
        sendWithMainMenu(chatId, lang, buildCookSuggestions(session, lang, chatId));
    }

    private void handleEditStartCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String foodId = data.substring(CB_FOOD_EDIT_START.length());
        Optional<Food> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        addFoodSessions.remove(chatId);
        cookSessions.remove(chatId);
        editSessions.put(chatId, new EditFoodSession(foodId));
        answerCallback(callbackQuery.getId(), null, false);
        sendEditFieldChoiceKeyboard(chatId, lang);
    }

    private void sendEditFieldChoiceKeyboard(long chatId, Lang lang) {
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
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "edit.choose_field"));
        message.setReplyMarkup(new InlineKeyboardMarkup(
                List.of(List.of(name, time), List.of(category, ingredients), List.of(recipe))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
                session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId));
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
                sendEditRecipePrompt(chatId, lang);
                break;
            default:
                break;
        }
    }

    private void sendEditRecipePrompt(long chatId, Lang lang) {
        InlineKeyboardButton clear = new InlineKeyboardButton(Messages.get(lang, "btn.clear_recipe"));
        clear.setCallbackData(CB_FOOD_EDIT_RECIPE_CLEAR);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "edit.ask_recipe"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(clear))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), null);
        foodRepository.update(updated);
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
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), food.getRecipe());
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
                    food.getRecipe());
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
        String foodId = data.substring(CB_FOOD_DELETE_START.length());
        Optional<Food> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        Food food = foodOpt.get();
        InlineKeyboardButton yes = new InlineKeyboardButton(Messages.get(lang, "delete.confirm_yes"));
        yes.setCallbackData(CB_FOOD_DELETE_CONFIRM_YES + foodId);
        InlineKeyboardButton no = new InlineKeyboardButton(Messages.get(lang, "delete.confirm_no"));
        no.setCallbackData(CB_FOOD_DELETE_CONFIRM_NO + foodId);
        SendMessage message = new SendMessage(String.valueOf(chatId),
                Messages.get(lang, "delete.confirm", FoodNameTranslations.translate(food.getName(), lang)));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(yes, no))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "delete.cancelled"));
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

    private String buildCookSuggestions(CookSession session, Lang lang, long chatId) {
        List<Food> ready = new ArrayList<>();
        List<Food> shoppingNeeded = new ArrayList<>();
        Map<Food, List<String>> missingByFood = new HashMap<>();

        for (Food food : foodRepository.findVisibleTo(chatId)) {
            if (food.getPrepTimeMinutes() > session.getTimeMinutes()) {
                continue;
            }
            if (session.getCategory() != null && !session.getCategory().equalsIgnoreCase(food.getCategory())) {
                continue;
            }
            List<String> missing = new ArrayList<>();
            for (String ingredient : food.getIngredients()) {
                boolean have = session.getHaveIngredients().stream().anyMatch(h -> h.equalsIgnoreCase(ingredient));
                if (!have) {
                    missing.add(ingredient);
                }
            }
            if (missing.isEmpty()) {
                ready.add(food);
            } else if (session.isCanShop()) {
                shoppingNeeded.add(food);
                missingByFood.put(food, missing);
            }
        }

        if (ready.isEmpty() && shoppingNeeded.isEmpty()) {
            return Messages.get(lang, "cook.nothing_matches");
        }

        StringBuilder builder = new StringBuilder();
        if (!ready.isEmpty()) {
            builder.append(Messages.get(lang, "cook.ready_header")).append("\n");
            for (Food food : ready) {
                builder.append("- ").append(formatFood(food, lang)).append("\n");
            }
        }
        if (!shoppingNeeded.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(Messages.get(lang, "cook.shopping_header")).append("\n");
            String missingLabel = Messages.get(lang, "cook.missing_label");
            for (Food food : shoppingNeeded) {
                String missingText = missingByFood.get(food).stream()
                        .map(i -> IngredientIcons.iconFor(i) + " " + IngredientTranslations.translate(i, lang))
                        .collect(Collectors.joining(", "));
                builder.append("- ").append(FoodNameTranslations.translate(food.getName(), lang))
                        .append(" [").append(categoryLabel(food.getCategory(), lang)).append("] (")
                        .append(food.getPrepTimeMinutes()).append(" ").append(Messages.get(lang, "min_unit"))
                        .append(") - ").append(missingLabel).append(": ")
                        .append(missingText)
                        .append("\n");
            }
        }
        return builder.toString().trim();
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
        SendMessage message = new SendMessage(String.valueOf(chatId), prompt);
        message.setReplyMarkup(new InlineKeyboardMarkup(rows));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup buildIngredientKeyboard(IngredientPickerState state, String prefix,
                                                          String doneCallback, String clearCallback, Lang lang) {
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
        int totalPages = Math.max(1, (ordered.size() + INGREDIENTS_PER_PAGE - 1) / INGREDIENTS_PER_PAGE);
        int page = Math.max(0, Math.min(state.getIngredientPage(), totalPages - 1));
        int start = page * INGREDIENTS_PER_PAGE;
        int end = Math.min(start + INGREDIENTS_PER_PAGE, ordered.size());

        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        for (String name : ordered.subList(start, end)) {
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
        return new InlineKeyboardMarkup(rows);
    }

    private void sendAddFoodIngredientKeyboard(long chatId, AddFoodSession session, Lang lang) {
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "addfood.ingredient_prompt"));
        message.setReplyMarkup(buildIngredientKeyboard(session, CB_ADDFOOD_INGREDIENT, CB_ADDFOOD_INGREDIENT_DONE,
                CB_ADDFOOD_INGREDIENT_CLEAR, lang));
        try {
            Message sent = execute(message);
            session.setKeyboardMessageId(sent.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editAddFoodIngredientKeyboard(long chatId, AddFoodSession session, Lang lang) {
        if (session.getKeyboardMessageId() == null) {
            sendAddFoodIngredientKeyboard(chatId, session, lang);
            return;
        }
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(session.getKeyboardMessageId());
        edit.setReplyMarkup(buildIngredientKeyboard(session, CB_ADDFOOD_INGREDIENT, CB_ADDFOOD_INGREDIENT_DONE,
                CB_ADDFOOD_INGREDIENT_CLEAR, lang));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
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
        List<Food> foods = scope.equals(SCOPE_MINE) ? foodRepository.findOwnedBy(chatId) : foodRepository.findGlobal();
        String headerKey = scope.equals(SCOPE_MINE) ? "foods.header.mine" : "foods.header.global";

        if (foods.isEmpty()) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "foods.none"));
            return;
        }

        int totalPages = (foods.size() + FOODS_PER_PAGE - 1) / FOODS_PER_PAGE;
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));
        int start = clampedPage * FOODS_PER_PAGE;
        int end = Math.min(start + FOODS_PER_PAGE, foods.size());

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Food food : foods.subList(start, end)) {
            String label = FoodNameTranslations.translate(food.getName(), lang) + " (" + food.getPrepTimeMinutes()
                    + " " + Messages.get(lang, "min_unit") + ")";
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(CB_FOOD_VIEW + food.getId());
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
            EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
            edit.setChatId(String.valueOf(chatId));
            edit.setMessageId(editMessageId);
            edit.setReplyMarkup(markup);
            try {
                execute(edit);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
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
        return FoodNameTranslations.translate(food.getName(), lang) + "\n"
                + Messages.get(lang, "food.detail_category", categoryLabel(food.getCategory(), lang)) + "\n"
                + Messages.get(lang, "food.detail_time", timeText) + "\n"
                + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n"
                + Messages.get(lang, "food.detail_recipe", recipeText);
    }

    private void handleFoodViewCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String foodId = data.substring(CB_FOOD_VIEW.length());
        Optional<Food> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Food food = foodOpt.get();
        answerCallback(callbackQuery.getId(), null, false);
        String text = formatFoodDetail(food, lang);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        if (canModify(food, chatId)) {
            InlineKeyboardButton settings = new InlineKeyboardButton(Messages.get(lang, "btn.settings"));
            settings.setCallbackData(CB_FOOD_SETTINGS + foodId);
            message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(settings))));
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleFoodSettingsCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String foodId = data.substring(CB_FOOD_SETTINGS.length());
        Optional<Food> foodOpt = foodRepository.findById(foodId);
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        InlineKeyboardButton edit = new InlineKeyboardButton(Messages.get(lang, "btn.edit"));
        edit.setCallbackData(CB_FOOD_EDIT_START + foodId);
        InlineKeyboardButton delete = new InlineKeyboardButton(Messages.get(lang, "btn.delete"));
        delete.setCallbackData(CB_FOOD_DELETE_START + foodId);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "settings.choose_action"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(edit, delete))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup mainMenuKeyboard(Lang lang, long chatId) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(Messages.get(lang, "btn.add_food"));
        if (isSuperAdmin(chatId)) {
            row1.add(Messages.get(lang, "btn.all_foods"));
        }
        KeyboardRow row2 = new KeyboardRow();
        row2.add(Messages.get(lang, "btn.what_can_cook"));
        row2.add(Messages.get(lang, "btn.change_lang"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(Messages.get(lang, "btn.help"));
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3));
        markup.setResizeKeyboard(true);
        return markup;
    }

    private void sendWithMainMenu(long chatId, Lang lang, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(mainMenuKeyboard(lang, chatId));
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
