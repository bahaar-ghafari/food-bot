package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.AmountEditorState;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.CookSession;
import com.foodbot.food.EditFoodSession;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodCategories;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.IngredientIcons;
import com.foodbot.food.IngredientPickerState;
import com.foodbot.food.IngredientSearch;
import com.foodbot.food.IngredientTranslations;
import com.foodbot.food.Paginator;
import com.foodbot.food.RecipeStepEditorState;
import com.foodbot.food.RecipeSteps;
import com.foodbot.food.SearchResultContext;
import com.foodbot.lang.Lang;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.lang.Messages;
import com.foodbot.lang.TimeFormat;
import com.foodbot.social.FollowRepository;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import java.io.Serializable;

import static com.foodbot.bot.BotConstants.*;

/** Shared Telegram/UI plumbing for the per-flow handlers. */
class FlowHandler {
    protected final BotContext ctx;
    protected final FoodRepository foodRepository;
    protected final LanguageRepository languageRepository;
    protected final FollowRepository followRepository;
    protected final FavoriteRepository favoriteRepository;
    protected final ShoppingListRepository shoppingListRepository;
    protected final Map<Long, AddFoodSession> addFoodSessions;
    protected final Map<Long, CookSession> cookSessions;
    protected final Map<Long, EditFoodSession> editSessions;
    protected final Map<Long, CookResultContext> lastCookResults;
    protected final Map<Long, SearchResultContext> lastSearchResults;
    protected final Map<Long, Food> pendingSearchSuggestion;
    protected final Map<Long, String> pendingSearchQuery;
    protected final Set<Long> awaitingFeedback;
    protected final Set<Long> awaitingSearch;
    protected final Set<Long> awaitingFollowId;
    protected final AiSuggestionService aiSuggestionService;
    protected final MealDbSuggestionService mealDbSuggestionService;
    protected final Long superAdminChatId;
    protected final Long feedbackChatId;

    FlowHandler(BotContext ctx) {
        this.ctx = ctx;
        this.foodRepository = ctx.foodRepository;
        this.languageRepository = ctx.languageRepository;
        this.followRepository = ctx.followRepository;
        this.favoriteRepository = ctx.favoriteRepository;
        this.shoppingListRepository = ctx.shoppingListRepository;
        this.addFoodSessions = ctx.addFoodSessions;
        this.cookSessions = ctx.cookSessions;
        this.editSessions = ctx.editSessions;
        this.lastCookResults = ctx.lastCookResults;
        this.lastSearchResults = ctx.lastSearchResults;
        this.pendingSearchSuggestion = ctx.pendingSearchSuggestion;
        this.pendingSearchQuery = ctx.pendingSearchQuery;
        this.awaitingFeedback = ctx.awaitingFeedback;
        this.awaitingSearch = ctx.awaitingSearch;
        this.awaitingFollowId = ctx.awaitingFollowId;
        this.aiSuggestionService = ctx.aiSuggestionService;
        this.mealDbSuggestionService = ctx.mealDbSuggestionService;
        this.superAdminChatId = ctx.superAdminChatId;
        this.feedbackChatId = ctx.feedbackChatId;
    }

    protected <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) throws TelegramApiException {
        return ctx.sender.execute(method);
    }

    Lang lang(long chatId) {
        Lang lang = languageRepository.get(chatId);
        return lang != null ? lang : Lang.FA;
    }

    boolean isSuperAdmin(long chatId) {
        return superAdminChatId != null && superAdminChatId == chatId;
    }

    boolean canModify(Food food, long chatId) {
        return isSuperAdmin(chatId) || (food.getOwnerChatId() != null && food.getOwnerChatId() == chatId);
    }

    boolean matchesLanguage(String text, Lang lang) {
        boolean hasPersian = text.codePoints().anyMatch(cp -> cp >= 0x0600 && cp <= 0x06FF);
        boolean hasLatin = text.chars().anyMatch(c -> Character.isLetter(c) && c < 128);
        if (lang == Lang.FA) {
            return hasPersian && !hasLatin;
        }
        return hasLatin && !hasPersian;
    }

    Integer parseMinutes(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    void handleIngredientTyped(IngredientPickerState state, String text) {
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

    boolean handleIngredientPageNav(IngredientPickerState state, String data, String prefix) {
        String pagePrefix = prefix + INGREDIENT_PAGE_INFIX;
        if (!data.startsWith(pagePrefix)) {
            return false;
        }
        state.setIngredientPage(Integer.parseInt(data.substring(pagePrefix.length())));
        return true;
    }

    void toggleAndAnswer(Set<String> selected, String ingredient, String callbackId, Lang lang) {
        if (selected.remove(ingredient)) {
            answerCallback(callbackId, Messages.get(lang, "removed", ingredient), false);
        } else {
            selected.add(ingredient);
            answerCallback(callbackId, Messages.get(lang, "added", ingredient), false);
        }
    }

    record PagedRef(String key, int page, String id) {
    }

    PagedRef parsePagedRef(String remainder) {
        int firstColon = remainder.indexOf(':');
        int secondColon = remainder.indexOf(':', firstColon + 1);
        String key = remainder.substring(0, firstColon);
        int page = Integer.parseInt(remainder.substring(firstColon + 1, secondColon));
        String id = remainder.substring(secondColon + 1);
        return new PagedRef(key, page, id);
    }

    void editMessageTextAndMarkup(long chatId, Integer messageId, String text, InlineKeyboardMarkup markup) {
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

    void clearInlineKeyboard(long chatId, Integer messageId) {
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

    InlineKeyboardMarkup buildRecipeStepKeyboard(List<String> steps, String actionPrefix,
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

    String recipeStepManagerText(List<String> steps, Integer editingIndex, Lang lang) {
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

    void applyRecipeStepAction(RecipeStepEditorState state, String verb, int index) {
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

    void applyAmountInput(AmountEditorState state, String text) {
        String ingredient = state.getAmountEditingIngredient();
        String trimmed = text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("skip") || trimmed.equals("-")) {
            state.getIngredientAmounts().remove(ingredient);
        } else {
            state.getIngredientAmounts().put(ingredient, trimmed);
        }
        state.setAmountEditingIngredient(null);
    }

    InlineKeyboardMarkup buildAmountsKeyboard(List<String> ingredients, AmountEditorState state,
                                                       String actionPrefix, String doneCallback,
                                                       InlineKeyboardButton extraButton, Lang lang) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (int i = 0; i < ingredients.size(); i++) {
            String ingredient = ingredients.get(i);
            String label = iconPrefix(ingredient) + IngredientTranslations.translate(ingredient, lang);
            if (ingredient.equals(state.getAmountEditingIngredient())) {
                label = "✏️ " + label + " …";
            } else {
                String amount = state.getIngredientAmounts().get(ingredient);
                if (amount != null && !amount.isBlank()) {
                    label = label + " — " + amount;
                }
            }
            InlineKeyboardButton button = new InlineKeyboardButton(label);
            button.setCallbackData(actionPrefix + i);
            rows.add(List.of(button));
        }
        InlineKeyboardButton done = new InlineKeyboardButton(Messages.get(lang, "btn.done_recipe"));
        done.setCallbackData(doneCallback);
        rows.add(extraButton != null ? List.of(done, extraButton) : List.of(done));
        return new InlineKeyboardMarkup(rows);
    }

    String amountsManagerText(AmountEditorState state, Lang lang) {
        if (state.getAmountEditingIngredient() != null) {
            String name = IngredientTranslations.translate(state.getAmountEditingIngredient(), lang);
            return Messages.get(lang, "amounts.ask_value", name);
        }
        return Messages.get(lang, "amounts.manager.prompt");
    }

    Map<String, String> filterAmounts(Map<String, String> amounts, Set<String> keep) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : amounts.entrySet()) {
            if (keep.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    static String authorDisplayName(User from) {
        if (from == null) {
            return null;
        }
        String full = ((from.getFirstName() == null ? "" : from.getFirstName()) + " "
                + (from.getLastName() == null ? "" : from.getLastName())).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return from.getUserName() != null ? "@" + from.getUserName() : null;
    }

    List<String> cookCategoryKeys() {
        List<String> keys = new ArrayList<>();
        keys.add(CATEGORY_ANY);
        keys.addAll(FoodCategories.ALL);
        return keys;
    }

    String iconPrefix(String ingredient) {
        String icon = IngredientIcons.iconFor(ingredient);
        return icon.isEmpty() ? "" : icon + " ";
    }

    String formatIngredientWithAmount(String ingredient, Map<String, String> amounts, Lang lang) {
        String label = iconPrefix(ingredient) + IngredientTranslations.translate(ingredient, lang);
        String amount = amounts.get(ingredient);
        return amount != null && !amount.isBlank() ? label + " (" + amount + ")" : label;
    }

    String categoryLabel(String key, Lang lang) {
        if (key == null || key.equals(CATEGORY_ANY)) {
            return Messages.get(lang, "category.any");
        }
        return Messages.get(lang, "category." + key);
    }

    String formatFood(Food food, Lang lang) {
        String ingredients = food.getIngredients().stream()
                .map(i -> formatIngredientWithAmount(i, food.getIngredientAmounts(), lang))
                .collect(Collectors.joining(", "));
        return FoodNameTranslations.translate(food.getName(), lang) + " [" + categoryLabel(food.getCategory(), lang)
                + "] (" + TimeFormat.format(food.getPrepTimeMinutes(), lang) + ") - " + ingredients;
    }

    void sendCategoryKeyboard(long chatId, String callbackPrefix, List<String> categoryKeys, Lang lang,
                                       String prompt) {
        sendCategoryKeyboard(chatId, callbackPrefix, categoryKeys, lang, prompt, null, null);
    }

    void sendCategoryKeyboard(long chatId, String callbackPrefix, List<String> categoryKeys, Lang lang,
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

    InlineKeyboardMarkup buildIngredientKeyboard(IngredientPickerState state, String prefix,
                                                          String doneCallback, String clearCallback, Lang lang) {
        return buildIngredientKeyboard(state, prefix, doneCallback, clearCallback, lang, null);
    }

    InlineKeyboardMarkup buildIngredientKeyboard(IngredientPickerState state, String prefix,
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
            String label = (isSelected ? "✅ " : "⬜ ") + iconPrefix(name) + IngredientTranslations.translate(name, lang);
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

    String displayName(long userId) {
        try {
            Chat chat = execute(new GetChat(String.valueOf(userId)));
            String full = ((chat.getFirstName() == null ? "" : chat.getFirstName()) + " "
                    + (chat.getLastName() == null ? "" : chat.getLastName())).trim();
            if (!full.isEmpty()) {
                return full + " (" + userId + ")";
            }
            if (chat.getUserName() != null) {
                return "@" + chat.getUserName() + " (" + userId + ")";
            }
        } catch (TelegramApiException ignored) {
            // fall through to the bare id
        }
        return String.valueOf(userId);
    }

    static String authorDisplayNameOrId(User from, long chatId) {
        String name = authorDisplayName(from);
        return (name != null ? name + " (" + chatId + ")" : String.valueOf(chatId));
    }

    ReplyKeyboardMarkup mainMenuKeyboard(Lang lang, long chatId) {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(Messages.get(lang, "btn.add_food"));
        if (isSuperAdmin(chatId)) {
            row1.add(Messages.get(lang, "btn.all_foods"));
        }
        KeyboardRow row2 = new KeyboardRow();
        row2.add(Messages.get(lang, "btn.what_can_cook"));
        row2.add(Messages.get(lang, "btn.search"));
        KeyboardRow row3 = new KeyboardRow();
        row3.add(Messages.get(lang, "btn.change_lang"));
        row3.add(Messages.get(lang, "btn.help"));
        KeyboardRow row4 = new KeyboardRow();
        row4.add(Messages.get(lang, "btn.friends"));
        row4.add(Messages.get(lang, "btn.feedback"));
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(List.of(row1, row2, row3, row4));
        markup.setResizeKeyboard(true);
        return markup;
    }

    void sendWithMainMenu(long chatId, Lang lang, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(mainMenuKeyboard(lang, chatId));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** Sends the recipe's photo, if it has one; a stale or rejected file_id must not block the text that follows. */
    void sendFoodPhoto(long chatId, Food food) {
        if (food.getPhotoFileId() == null) {
            return;
        }
        SendPhoto photo = new SendPhoto(String.valueOf(chatId), new InputFile(food.getPhotoFileId()));
        try {
            ctx.sender.sendPhoto(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void send(long chatId, String text) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void answerCallback(String callbackId, String text, boolean alert) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        answer.setText(text);
        answer.setShowAlert(alert);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
