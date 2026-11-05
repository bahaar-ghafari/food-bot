package com.foodbot.bot;

import com.foodbot.food.AddFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodCategories;
import com.foodbot.food.RecipeSteps;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import com.foodbot.lang.PersianNames;
import com.foodbot.lang.TimeFormat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.foodbot.bot.BotConstants.*;

class AddFoodHandler extends FlowHandler {
    AddFoodHandler(BotContext ctx) {
        super(ctx);
    }

    void sendAddFoodNamePrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_name"),
                new InlineKeyboardMarkup(List.of(List.of(back))));
    }

    void sendAddFoodTimePrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_time", session.getName()),
                new InlineKeyboardMarkup(List.of(List.of(back))));
    }

    void sendAddFoodCategoryPrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        sendCategoryKeyboard(chatId, CB_ADDFOOD_CATEGORY, FoodCategories.ALL, lang,
                Messages.get(lang, "addfood.ask_category"), session.getKeyboardMessageId(), back);
    }

    void sendAddFoodPhotoPrompt(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton skip = new InlineKeyboardButton(Messages.get(lang, "addfood.photo_skip"));
        skip.setCallbackData(CB_ADDFOOD_PHOTO_SKIP);
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(skip));
        if (session.getPhotoFileId() != null) {
            InlineKeyboardButton remove = new InlineKeyboardButton(Messages.get(lang, "addfood.photo_remove"));
            remove.setCallbackData(CB_ADDFOOD_PHOTO_REMOVE);
            rows.add(List.of(remove));
        }
        rows.add(List.of(back));
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ask_photo"), new InlineKeyboardMarkup(rows));
    }

    void handleAddFoodPhoto(long chatId, String fileId, Lang lang) {
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null) {
            return;
        }
        if (session.getStep() != AddFoodSession.Step.AWAITING_PHOTO) {
            send(chatId, Messages.get(lang, "addfood.photo_not_now"));
            return;
        }
        session.setPhotoFileId(fileId);
        session.setReturnToReview(false);
        session.setStep(AddFoodSession.Step.REVIEW);
        renderAddFoodReview(chatId, session, lang);
    }

    /** Skip keeps whatever photo is already attached (none, on a first pass); Remove drops it. */
    void handleAddFoodPhotoButtonCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_PHOTO) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        if (data.equals(CB_ADDFOOD_PHOTO_REMOVE)) {
            session.setPhotoFileId(null);
        }
        session.setReturnToReview(false);
        session.setStep(AddFoodSession.Step.REVIEW);
        answerCallback(callbackQuery.getId(), null, false);
        renderAddFoodReview(chatId, session, lang);
    }

    void handleAddFoodCancelCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.remove(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        if (session != null) {
            clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        }
        // The main-menu keyboard is already showing: just remove the screen instead of posting "Cancelled".
        deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
    }

    void handleAddFoodBackCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
                handleAddFoodCancelCallback(callbackQuery, chatId, data);
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
            case AWAITING_AMOUNTS:
                session.setAmountEditingIngredient(null);
                session.setStep(AddFoodSession.Step.SELECTING_INGREDIENTS);
                renderAddFoodIngredientKeyboard(chatId, session, lang);
                break;
            case AWAITING_RECIPE:
                session.setStep(AddFoodSession.Step.AWAITING_AMOUNTS);
                session.setEditingRecipeStepIndex(null);
                renderAddFoodAmountsManager(chatId, session, lang);
                break;
            case AWAITING_PHOTO:
                session.setStep(AddFoodSession.Step.AWAITING_RECIPE);
                renderAddFoodRecipeManager(chatId, session, lang);
                break;
            case REVIEW:
                session.setStep(AddFoodSession.Step.AWAITING_PHOTO);
                sendAddFoodPhotoPrompt(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    void handleAddFoodText(long chatId, String text, Lang lang) {
        AddFoodSession session = addFoodSessions.get(chatId);
        switch (session.getStep()) {
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

            case AWAITING_AMOUNTS:
                if (session.getAmountEditingIngredient() == null) {
                    send(chatId, Messages.get(lang, "tap_button_above"));
                    return;
                }
                if (text.trim().length() > AMOUNT_CHAR_LIMIT) {
                    send(chatId, Messages.get(lang, "amounts.too_long"));
                    return;
                }
                applyAmountInput(session, text);
                renderAddFoodAmountsManager(chatId, session, lang);
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

            case AWAITING_PHOTO:
                send(chatId, Messages.get(lang, "addfood.photo_send_or_skip"));
                break;

            case REVIEW:
                send(chatId, Messages.get(lang, "tap_button_above"));
                break;
        }
    }

    void sendOrEditAddFood(long chatId, AddFoodSession session, String text, InlineKeyboardMarkup markup) {
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

    void handleAddFoodIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
            session.setStep(AddFoodSession.Step.AWAITING_AMOUNTS);
            session.setAmountEditingIngredient(null);
            renderAddFoodAmountsManager(chatId, session, lang);
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

    void handleAddFoodCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void renderAddFoodIngredientKeyboard(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = buildIngredientKeyboard(session, CB_ADDFOOD_INGREDIENT, CB_ADDFOOD_INGREDIENT_DONE,
                CB_ADDFOOD_INGREDIENT_CLEAR, lang, back);
        sendOrEditAddFood(chatId, session, Messages.get(lang, "addfood.ingredient_prompt"), markup);
    }

    void renderAddFoodAmountsManager(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        List<String> ordered = new ArrayList<>(session.getSelectedIngredients());
        InlineKeyboardMarkup markup = buildAmountsKeyboard(ordered, session, CB_ADDFOOD_AMOUNT, CB_ADDFOOD_AMOUNT_DONE,
                back, lang);
        sendOrEditAddFood(chatId, session, amountsManagerText(session, lang), markup);
    }

    void handleAddFoodAmountCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_AMOUNTS) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        if (data.equals(CB_ADDFOOD_AMOUNT_DONE)) {
            answerCallback(callbackQuery.getId(), null, false);
            session.setAmountEditingIngredient(null);
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
        List<String> ordered = new ArrayList<>(session.getSelectedIngredients());
        int index = Integer.parseInt(data.substring(CB_ADDFOOD_AMOUNT.length()));
        if (index < 0 || index >= ordered.size()) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        session.setAmountEditingIngredient(ordered.get(index));
        answerCallback(callbackQuery.getId(), null, false);
        renderAddFoodAmountsManager(chatId, session, lang);
    }

    void renderAddFoodRecipeManager(long chatId, AddFoodSession session, Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = buildRecipeStepKeyboard(session.getRecipeSteps(), CB_ADDFOOD_RECIPE_STEP,
                CB_ADDFOOD_RECIPE_DONE, back, lang);
        String text = recipeStepManagerText(session.getRecipeSteps(), session.getEditingRecipeStepIndex(), lang);
        sendOrEditAddFood(chatId, session, text, markup);
    }

    void handleAddFoodRecipeStepCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleAddFoodRecipeDoneCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.AWAITING_RECIPE) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        boolean fromReview = session.isReturnToReview();
        session.setReturnToReview(false);
        session.setEditingRecipeStepIndex(null);
        answerCallback(callbackQuery.getId(), null, false);
        if (fromReview) {
            session.setStep(AddFoodSession.Step.REVIEW);
            renderAddFoodReview(chatId, session, lang);
        } else {
            session.setStep(AddFoodSession.Step.AWAITING_PHOTO);
            sendAddFoodPhotoPrompt(chatId, session, lang);
        }
    }

    String formatAddFoodReview(AddFoodSession session, Lang lang) {
        String ingredients = session.getSelectedIngredients().stream()
                .map(i -> formatIngredientWithAmount(i, session.getIngredientAmounts(), lang))
                .collect(Collectors.joining(", "));
        String timeText = TimeFormat.format(session.getPrepTimeMinutes(), lang);
        String recipeText = session.getRecipeSteps().isEmpty()
                ? Messages.get(lang, "food.no_recipe")
                : RecipeSteps.join(session.getRecipeSteps());
        return Messages.get(lang, "addfood.review_header") + "\n\n"
                + "🍽️ " + session.getName() + "\n"
                + "🏷️ " + categoryLabel(session.getCategory(), lang) + "\n"
                + "⏱️ " + Messages.get(lang, "food.detail_time", timeText) + "\n"
                + "🧾 " + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n\n"
                + "📝 " + Messages.get(lang, "food.detail_recipe", recipeText) + "\n"
                + "📷 " + Messages.get(lang, session.getPhotoFileId() != null ? "addfood.photo_attached"
                        : "addfood.photo_none") + "\n";
    }

    void renderAddFoodReview(long chatId, AddFoodSession session, Lang lang) {
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
        InlineKeyboardButton editPhoto = new InlineKeyboardButton(Messages.get(lang, "edit.field.photo"));
        editPhoto.setCallbackData(CB_ADDFOOD_REVIEW_EDIT + "photo");
        InlineKeyboardButton confirm = new InlineKeyboardButton(Messages.get(lang, "addfood.confirm_save"));
        confirm.setCallbackData(CB_ADDFOOD_CONFIRM);
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_ADDFOOD_BACK);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(
                List.of(editName, editTime),
                List.of(editCategory, editIngredients),
                List.of(editRecipe, editPhoto),
                List.of(confirm),
                List.of(back)));
        sendOrEditAddFood(chatId, session, formatAddFoodReview(session, lang), markup);
    }

    void handleAddFoodReviewEditCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
            case "photo":
                session.setReturnToReview(true);
                session.setStep(AddFoodSession.Step.AWAITING_PHOTO);
                sendAddFoodPhotoPrompt(chatId, session, lang);
                break;
            default:
                break;
        }
    }

    void handleAddFoodConfirmCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        AddFoodSession session = addFoodSessions.get(chatId);
        if (session == null || session.getStep() != AddFoodSession.Step.REVIEW) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        answerCallback(callbackQuery.getId(), null, false);
        finalizeNewFood(chatId, session, lang, callbackQuery.getFrom());
    }

    void finalizeNewFood(long chatId, AddFoodSession session, Lang lang, User from) {
        Map<String, String> amounts = filterAmounts(session.getIngredientAmounts(), session.getSelectedIngredients());
        Food food = new Food(UUID.randomUUID().toString(), session.getName(), session.getPrepTimeMinutes(),
                session.getCategory(), new ArrayList<>(session.getSelectedIngredients()), session.getOwnerChatId(),
                chatId, RecipeSteps.join(session.getRecipeSteps()), lang, amounts, Food.SOURCE_BOT, authorDisplayName(from),
                session.getPhotoFileId());
        foodRepository.add(food);
        clearInlineKeyboard(chatId, session.getKeyboardMessageId());
        addFoodSessions.remove(chatId);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "addfood.saved_message", formatFood(food, lang)));
        notifyFollowers(chatId, food, authorDisplayName(from));
    }

    void notifyFollowers(long authorChatId, Food food, String authorName) {
        String who = authorName != null ? authorName : String.valueOf(authorChatId);
        for (long follower : followRepository.notifiableFollowersOf(authorChatId)) {
            if (followRepository.isDigest(follower)) {
                followRepository.queueForDigest(follower, food.getId());
                continue;
            }
            Lang followerLang = lang(follower);
            sendFoodPhoto(follower, food);
            send(follower, Messages.get(followerLang, "friends.new_recipe", PersianNames.forLang(followerLang, who)) + "\n\n"
                    + ctx.browse.formatFoodDetail(food, followerLang));
        }
    }
}
