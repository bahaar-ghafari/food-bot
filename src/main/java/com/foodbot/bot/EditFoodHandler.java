package com.foodbot.bot;

import com.foodbot.food.EditFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodCategories;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.RecipeSteps;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import com.foodbot.lang.TimeFormat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.foodbot.bot.BotConstants.*;

/**
 * Edits happen on a draft held in the session: the review screen shows it, each field button detours through
 * the matching prompt and returns here, and nothing is written until Save.
 */
class EditFoodHandler extends FlowHandler {
    EditFoodHandler(BotContext ctx) {
        super(ctx);
    }

    /** Re-checks the session and the caller's right to edit; answers the callback and returns null if either fails. */
    private EditFoodSession activeSession(CallbackQuery callbackQuery, long chatId, Lang lang,
                                          EditFoodSession.Step... allowed) {
        EditFoodSession session = editSessions.get(chatId);
        if (session == null || (allowed.length > 0 && !List.of(allowed).contains(session.getStep()))) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return null;
        }
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            answerCallback(callbackQuery.getId(), Messages.get(lang, "permission.denied"), true);
            return null;
        }
        return session;
    }

    private void backToReview(long chatId, EditFoodSession session, Lang lang) {
        session.setStep(EditFoodSession.Step.REVIEW);
        session.setAmountEditingIngredient(null);
        session.setEditingRecipeStepIndex(null);
        renderEditReview(chatId, session, lang);
    }

    void handleEditText(long chatId, String text, Lang lang) {
        EditFoodSession session = editSessions.get(chatId);
        Optional<Food> foodOpt = foodRepository.findById(session.getFoodId());
        if (foodOpt.isEmpty() || !canModify(foodOpt.get(), chatId)) {
            editSessions.remove(chatId);
            send(chatId, Messages.get(lang, "permission.denied"));
            return;
        }
        switch (session.getStep()) {
            case EDITING_NAME:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                session.setName(text);
                backToReview(chatId, session, lang);
                break;

            case EDITING_TIME:
                Integer minutes = parseMinutes(text);
                if (minutes == null) {
                    send(chatId, Messages.get(lang, "addfood.invalid_time"));
                    return;
                }
                session.setPrepTimeMinutes(minutes);
                backToReview(chatId, session, lang);
                break;

            case EDITING_INGREDIENTS:
                if (!matchesLanguage(text, lang)) {
                    send(chatId, Messages.get(lang, "lang.wrong_script"));
                    return;
                }
                handleIngredientTyped(session, text);
                renderEditIngredientKeyboard(chatId, session, lang);
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

            case EDITING_PHOTO:
                send(chatId, Messages.get(lang, "addfood.photo_send_or_skip"));
                break;

            case REVIEW:
            case EDITING_CATEGORY:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;
        }
    }

    void handleEditPhoto(long chatId, String fileId, Lang lang) {
        EditFoodSession session = editSessions.get(chatId);
        if (session == null) {
            return;
        }
        if (session.getStep() != EditFoodSession.Step.EDITING_PHOTO) {
            send(chatId, Messages.get(lang, "edit.photo_not_now"));
            return;
        }
        session.setPhotoFileId(fileId);
        backToReview(chatId, session, lang);
    }

    /** Skip keeps whatever photo is attached; Remove drops it. */
    void handleEditPhotoButtonCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_PHOTO);
        if (session == null) {
            return;
        }
        if (data.equals(CB_FOOD_EDIT_PHOTO_REMOVE)) {
            session.setPhotoFileId(null);
        }
        answerCallback(callbackQuery.getId(), null, false);
        backToReview(chatId, session, lang);
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
        EditFoodSession session = loadDraft(foodOpt.get(), chatId, lang);
        session.setExitCallback(CB_FOOD_SETTINGS + ref.key() + ":" + ref.page() + ":" + ref.id());
        session.setKeyboardMessageId(callbackQuery.getMessage().getMessageId());
        editSessions.put(chatId, session);
        answerCallback(callbackQuery.getId(), null, false);
        renderEditReview(chatId, session, lang);
    }

    EditFoodSession loadDraft(Food food, long chatId, Lang lang) {
        EditFoodSession session = new EditFoodSession(food.getId());
        session.setName(food.getName());
        session.setPrepTimeMinutes(food.getPrepTimeMinutes());
        session.setCategory(food.getCategory());
        session.setPhotoFileId(food.getPhotoFileId());
        session.getRecipeSteps().addAll(RecipeSteps.parse(food.getRecipe()));
        session.getCandidateIngredients().addAll(foodRepository.findAllIngredients(chatId, lang));
        for (String ingredient : food.getIngredients()) {
            boolean exists = session.getCandidateIngredients().stream().anyMatch(i -> i.equalsIgnoreCase(ingredient));
            if (!exists) {
                session.getCandidateIngredients().add(ingredient);
            }
            session.getSelectedIngredients().add(ingredient);
        }
        session.getIngredientAmounts().putAll(food.getIngredientAmounts());
        return session;
    }

    String formatEditReview(EditFoodSession session, Lang lang) {
        String ingredients = session.getSelectedIngredients().stream()
                .map(i -> formatIngredientWithAmount(i, session.getIngredientAmounts(), lang))
                .collect(Collectors.joining(", "));
        String recipeText = session.getRecipeSteps().isEmpty()
                ? Messages.get(lang, "food.no_recipe")
                : RecipeSteps.join(session.getRecipeSteps());
        return Messages.get(lang, "edit.review_header") + "\n\n"
                + "🍽️ " + session.getName() + "\n"
                + "🏷️ " + categoryLabel(session.getCategory(), lang) + "\n"
                + "⏱️ " + Messages.get(lang, "food.detail_time", TimeFormat.format(session.getPrepTimeMinutes(), lang))
                + "\n"
                + "🧾 " + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n\n"
                + "📝 " + Messages.get(lang, "food.detail_recipe", recipeText) + "\n"
                + "📷 " + Messages.get(lang, session.getPhotoFileId() != null ? "addfood.photo_attached"
                        : "addfood.photo_none") + "\n";
    }

    void renderEditReview(long chatId, EditFoodSession session, Lang lang) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(
                List.of(fieldButton(lang, "name"), fieldButton(lang, "time")),
                List.of(fieldButton(lang, "category"), fieldButton(lang, "ingredients")),
                List.of(fieldButton(lang, "recipe"), fieldButton(lang, "photo")),
                List.of(button(Messages.get(lang, "edit.save"), CB_FOOD_EDIT_SAVE)),
                List.of(button(Messages.get(lang, "btn.back"), CB_FOOD_EDIT_EXIT))));
        sendOrEditEditFood(chatId, session, formatEditReview(session, lang), markup);
    }

    private InlineKeyboardButton fieldButton(Lang lang, String field) {
        return button(Messages.get(lang, "edit.field." + field), CB_FOOD_EDIT_FIELD + field);
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(label);
        button.setCallbackData(callbackData);
        return button;
    }

    /** Leaves the editor without saving and goes back to the recipe's Settings screen. */
    void handleEditExitCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.REVIEW);
        if (session == null) {
            return;
        }
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        PagedRef ref = parsePagedRef(session.getExitCallback().substring(CB_FOOD_SETTINGS.length()));
        ctx.browse.renderSettings(chatId, lang, ref, session.getKeyboardMessageId());
    }

    void handleEditSaveCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.REVIEW);
        if (session == null) {
            return;
        }
        Food food = foodRepository.findById(session.getFoodId()).orElseThrow();
        Map<String, String> amounts = filterAmounts(session.getIngredientAmounts(), session.getSelectedIngredients());
        Food updated = new Food(food.getId(), session.getName(), session.getPrepTimeMinutes(), session.getCategory(),
                new ArrayList<>(session.getSelectedIngredients()), food.getOwnerChatId(), food.getCreatedByChatId(),
                RecipeSteps.join(session.getRecipeSteps()), food.getLanguage(), amounts, food.getSource(),
                food.getAuthorName(), session.getPhotoFileId());
        foodRepository.update(updated, true);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        editSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), Messages.get(lang, "addfood.saved_toast"), false);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "edit.saved", formatFood(updated, lang)));
    }

    void handleEditFieldCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang);
        if (session == null) {
            return;
        }
        String field = data.substring(CB_FOOD_EDIT_FIELD.length());
        answerCallback(callbackQuery.getId(), null, false);
        if (field.equals("back")) {
            backToReview(chatId, session, lang);
            return;
        }
        if (session.getStep() != EditFoodSession.Step.REVIEW) {
            return;
        }
        InlineKeyboardButton back = button(Messages.get(lang, "btn.back"), CB_FOOD_EDIT_FIELD + "back");
        switch (field) {
            case "name":
                session.setStep(EditFoodSession.Step.EDITING_NAME);
                sendOrEditEditFood(chatId, session, Messages.get(lang, "edit.ask_name"),
                        new InlineKeyboardMarkup(List.of(List.of(back))));
                break;
            case "time":
                session.setStep(EditFoodSession.Step.EDITING_TIME);
                sendOrEditEditFood(chatId, session, Messages.get(lang, "edit.ask_time"),
                        new InlineKeyboardMarkup(List.of(List.of(back))));
                break;
            case "category":
                session.setStep(EditFoodSession.Step.EDITING_CATEGORY);
                sendCategoryKeyboard(chatId, CB_FOOD_EDIT_CATEGORY, FoodCategories.ALL, lang,
                        Messages.get(lang, "edit.ask_category"), session.getKeyboardMessageId(), back);
                break;
            case "ingredients":
                session.setStep(EditFoodSession.Step.EDITING_INGREDIENTS);
                session.setIngredientFilter("");
                session.setIngredientPage(0);
                renderEditIngredientKeyboard(chatId, session, lang);
                break;
            case "recipe":
                session.setStep(EditFoodSession.Step.EDITING_RECIPE);
                session.setEditingRecipeStepIndex(null);
                renderEditRecipeManager(chatId, session, lang);
                break;
            case "photo":
                session.setStep(EditFoodSession.Step.EDITING_PHOTO);
                renderEditPhotoPrompt(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    void renderEditPhotoPrompt(long chatId, EditFoodSession session, Lang lang) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(button(Messages.get(lang, "addfood.photo_skip"), CB_FOOD_EDIT_PHOTO_SKIP)));
        if (session.getPhotoFileId() != null) {
            rows.add(List.of(button(Messages.get(lang, "addfood.photo_remove"), CB_FOOD_EDIT_PHOTO_REMOVE)));
        }
        rows.add(List.of(button(Messages.get(lang, "btn.back"), CB_FOOD_EDIT_FIELD + "back")));
        sendOrEditEditFood(chatId, session, Messages.get(lang, "addfood.ask_photo"), new InlineKeyboardMarkup(rows));
    }

    void renderEditRecipeManager(long chatId, EditFoodSession session, Lang lang) {
        InlineKeyboardButton clear = button(Messages.get(lang, "btn.clear_recipe"), CB_FOOD_EDIT_RECIPE_CLEAR);
        InlineKeyboardMarkup markup = buildRecipeStepKeyboard(session.getRecipeSteps(), CB_FOOD_EDIT_RECIPE_STEP,
                CB_FOOD_EDIT_RECIPE_DONE, clear, lang);
        String text = recipeStepManagerText(session.getRecipeSteps(), session.getEditingRecipeStepIndex(), lang);
        sendOrEditEditFood(chatId, session, text, markup);
    }

    void handleEditRecipeStepCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_RECIPE);
        if (session == null) {
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
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_RECIPE);
        if (session == null) {
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        backToReview(chatId, session, lang);
    }

    void handleEditRecipeClearCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_RECIPE);
        if (session == null) {
            return;
        }
        session.getRecipeSteps().clear();
        answerCallback(callbackQuery.getId(), null, false);
        backToReview(chatId, session, lang);
    }

    void handleEditCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_CATEGORY);
        if (session == null) {
            return;
        }
        int index = Integer.parseInt(data.substring(CB_FOOD_EDIT_CATEGORY.length()));
        session.setCategory(FoodCategories.ALL.get(index));
        answerCallback(callbackQuery.getId(), categoryLabel(session.getCategory(), lang), false);
        backToReview(chatId, session, lang);
    }

    void handleEditIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_INGREDIENTS);
        if (session == null) {
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
            renderEditIngredientKeyboard(chatId, session, lang);
            return;
        }
        if (handleIngredientPageNav(session, data, CB_FOOD_EDIT_INGREDIENT)) {
            answerCallback(callbackQuery.getId(), null, false);
            renderEditIngredientKeyboard(chatId, session, lang);
            return;
        }
        int index = Integer.parseInt(data.substring(CB_FOOD_EDIT_INGREDIENT.length()));
        String ingredient = session.getCandidateIngredients().get(index);
        toggleAndAnswer(session.getSelectedIngredients(), ingredient, callbackQuery.getId(), lang);
        renderEditIngredientKeyboard(chatId, session, lang);
    }

    void renderEditAmountsManager(long chatId, EditFoodSession session, Lang lang) {
        List<String> ordered = new ArrayList<>(session.getSelectedIngredients());
        InlineKeyboardMarkup markup = buildAmountsKeyboard(ordered, session, CB_FOOD_EDIT_AMOUNT,
                CB_FOOD_EDIT_AMOUNT_DONE, null, lang);
        sendOrEditEditFood(chatId, session, amountsManagerText(session, lang), markup);
    }

    void handleEditAmountCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        EditFoodSession session = activeSession(callbackQuery, chatId, lang, EditFoodSession.Step.EDITING_AMOUNTS);
        if (session == null) {
            return;
        }
        if (data.equals(CB_FOOD_EDIT_AMOUNT_DONE)) {
            answerCallback(callbackQuery.getId(), null, false);
            backToReview(chatId, session, lang);
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
        ctx.browse.renderSettings(chatId, lang, ref, callbackQuery.getMessage().getMessageId());
    }

    void renderEditIngredientKeyboard(long chatId, EditFoodSession session, Lang lang) {
        sendOrEditEditFood(chatId, session, Messages.get(lang, "edit.ingredient_prompt"),
                buildIngredientKeyboard(session, CB_FOOD_EDIT_INGREDIENT, CB_FOOD_EDIT_INGREDIENT_DONE,
                        CB_FOOD_EDIT_INGREDIENT_CLEAR, lang));
    }
}
