package com.foodbot.bot;

import com.foodbot.food.EditFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodCategories;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.RecipeSteps;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.foodbot.bot.BotConstants.*;

class EditFoodHandler extends FlowHandler {
    EditFoodHandler(BotContext ctx) {
        super(ctx);
    }

    void handleEditText(long chatId, String text, Lang lang) {
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
                        food.getLanguage(), food.getIngredientAmounts());
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
                        food.getLanguage(), food.getIngredientAmounts());
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

            case EDITING_AMOUNTS:
                if (session.getAmountEditingIngredient() == null) {
                    send(chatId, Messages.get(lang, "tap_button_above"));
                    return;
                }
                if (text.trim().length() > AMOUNT_CHAR_LIMIT) {
                    send(chatId, Messages.get(lang, "amounts.too_long"));
                    return;
                }
                applyAmountInput(session, text);
                renderEditAmountsManager(chatId, session, lang);
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

    void sendOrEditEditFood(long chatId, EditFoodSession session, String text, InlineKeyboardMarkup markup) {
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

    void handleEditStartCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void sendEditFieldChoiceKeyboard(long chatId, Lang lang, PagedRef ref, Integer editMessageId) {
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

    void handleEditFieldCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
                session.getIngredientAmounts().clear();
                session.getIngredientAmounts().putAll(food.getIngredientAmounts());
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

    void renderEditRecipeManager(long chatId, EditFoodSession session, Lang lang) {
        InlineKeyboardButton clear = new InlineKeyboardButton(Messages.get(lang, "btn.clear_recipe"));
        clear.setCallbackData(CB_FOOD_EDIT_RECIPE_CLEAR);
        InlineKeyboardMarkup markup = buildRecipeStepKeyboard(session.getRecipeSteps(), CB_FOOD_EDIT_RECIPE_STEP,
                CB_FOOD_EDIT_RECIPE_DONE, clear, lang);
        String text = recipeStepManagerText(session.getRecipeSteps(), session.getEditingRecipeStepIndex(), lang);
        sendOrEditEditFood(chatId, session, text, markup);
    }

    void handleEditRecipeStepCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleEditRecipeDoneCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
                RecipeSteps.join(session.getRecipeSteps()), food.getLanguage(), food.getIngredientAmounts());
        foodRepository.update(updated);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    void handleEditRecipeClearCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
                food.getIngredients(), food.getOwnerChatId(), food.getCreatedByChatId(), null, food.getLanguage(),
                food.getIngredientAmounts());
        foodRepository.update(updated);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    void handleEditCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
                food.getLanguage(), food.getIngredientAmounts());
        foodRepository.update(updated);
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), categoryLabel(category, lang), false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    void handleEditIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
            answerCallback(callbackQuery.getId(), null, false);
            session.setAmountEditingIngredient(null);
            session.setStep(EditFoodSession.Step.EDITING_AMOUNTS);
            renderEditAmountsManager(chatId, session, lang);
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

    void renderEditAmountsManager(long chatId, EditFoodSession session, Lang lang) {
        List<String> ordered = new ArrayList<>(session.getSelectedIngredients());
        InlineKeyboardMarkup markup = buildAmountsKeyboard(ordered, session, CB_FOOD_EDIT_AMOUNT,
                CB_FOOD_EDIT_AMOUNT_DONE, null, lang);
        sendOrEditEditFood(chatId, session, amountsManagerText(session, lang), markup);
    }

    void handleEditAmountCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || session.getStep() != EditFoodSession.Step.EDITING_AMOUNTS) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return;
        }
        if (data.equals(CB_FOOD_EDIT_AMOUNT_DONE)) {
            Food food = foodOpt.get();
            Map<String, String> amounts = filterAmounts(session.getIngredientAmounts(), session.getSelectedIngredients());
            Food updated = new Food(food.getId(), food.getName(), food.getPrepTimeMinutes(), food.getCategory(),
                    new ArrayList<>(session.getSelectedIngredients()), food.getOwnerChatId(), food.getCreatedByChatId(),
                    food.getRecipe(), food.getLanguage(), amounts);
            foodRepository.update(updated);
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "addfood.saved_toast"), false);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
            return;
        }
        List<String> ordered = new ArrayList<>(session.getSelectedIngredients());
        int index = Integer.parseInt(data.substring(CB_FOOD_EDIT_AMOUNT.length()));
        if (index < 0 || index >= ordered.size()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        session.setAmountEditingIngredient(ordered.get(index));
        answerCallback(callbackQuery.getId(), null, false);
        renderEditAmountsManager(chatId, session, lang);
    }

    void handleDeleteStartCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleDeleteConfirmYesCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleDeleteConfirmNoCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void sendEditIngredientKeyboard(long chatId, EditFoodSession session, Lang lang) {
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

    void editEditIngredientKeyboard(long chatId, EditFoodSession session, Lang lang) {
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
}
