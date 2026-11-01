package com.foodbot.bot;

import com.foodbot.food.CookRanking;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.CookSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.IngredientCategories;
import com.foodbot.food.IngredientTranslations;
import com.foodbot.food.Paginator;
import com.foodbot.food.PantryStaples;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import com.foodbot.lang.TimeFormat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.foodbot.bot.BotConstants.*;

class CookHandler extends FlowHandler {
    CookHandler(BotContext ctx) {
        super(ctx);
    }

    void handleCookText(long chatId, String text, Lang lang) {
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

    void handleCookIngredientCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleCookShopCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void handleCookCategoryCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    List<Food> computeCookMatches(long chatId, Lang lang, CookResultContext ctx, boolean needsShopping) {
        List<Food> result = new ArrayList<>();
        for (Food food : foodRepository.findVisibleTo(chatId, lang)) {
            if (food.getPrepTimeMinutes() > ctx.getTimeMinutes()) {
                continue;
            }
            if (ctx.getCategory() != null && !ctx.getCategory().equalsIgnoreCase(food.getCategory())) {
                continue;
            }
            boolean hasEverything = food.getIngredients().stream()
                    .allMatch(ing -> CookRanking.effectivelyHas(ctx.getHaveIngredients(), ing));
            if (needsShopping) {
                // Only suggest shopping for a dish that actually builds on something the user
                // already has - otherwise "I have chicken" would keep surfacing unrelated fish
                // or beef dishes that need every ingredient bought from scratch. If the user
                // hasn't selected anything yet, skip this check so /cook can still browse the
                // full list of shoppable dishes.
                boolean groundedInWhatUserHas = ctx.getHaveIngredients().isEmpty()
                        || CookRanking.buildsOnWhatUserHas(food, ctx.getHaveIngredients());
                if (!hasEverything && ctx.isCanShop() && groundedInWhatUserHas) {
                    result.add(food);
                }
            } else if (hasEverything) {
                result.add(food);
            }
        }
        // Rank so dishes reusing more of what the user already has come first, not just
        // whichever dish happens to need the fewest extra ingredients bought (see CookRanking).
        CookRanking.sortByBestMatch(result, ctx.getHaveIngredients());
        return result;
    }

    boolean effectivelyHas(Set<String> haveIngredients, String ingredient) {
        return CookRanking.effectivelyHas(haveIngredients, ingredient);
    }

    void addNonStapleIngredients(List<String> candidates, List<String> allIngredients) {
        for (String ingredient : allIngredients) {
            if (!PantryStaples.isStaple(ingredient)) {
                candidates.add(ingredient);
            }
        }
    }

    void sendCookResults(long chatId, Lang lang, CookResultContext ctx) {
        List<Food> ready = computeCookMatches(chatId, lang, ctx, false);
        List<Food> shoppingNeeded = computeCookMatches(chatId, lang, ctx, true);

        if (ready.isEmpty() && shoppingNeeded.isEmpty()) {
            sendNoMatchFallback(chatId, lang, ctx);
            return;
        }
        if (!ready.isEmpty()) {
            sendCookResultPage(chatId, lang, GROUP_READY, 0, null);
        }
        if (!shoppingNeeded.isEmpty()) {
            sendCookResultPage(chatId, lang, GROUP_SHOP, 0, null);
        }
    }

    /**
     * Nothing in the food list fits. If shopping is an option, ask the AI for a one-off
     * suggestion; otherwise prefer picking a protein/carb the user already has over
     * suggesting they eat their whole pantry raw, and only fall back to the AI/raw messages
     * once that's not possible either.
     */
    void sendNoMatchFallback(long chatId, Lang lang, CookResultContext ctx) {
        if (ctx.isCanShop()) {
            String message = aiSuggestionMessage(ctx, lang, true)
                    .orElseGet(() -> Messages.get(lang, "cook.nothing_matches"));
            sendWithMainMenu(chatId, lang, message);
            return;
        }
        if (ctx.getHaveIngredients().isEmpty()) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "cook.nothing_matches_no_shop_empty"));
            return;
        }
        // Prefer suggesting a protein + carb combo (e.g. tuna + rice) over just the protein
        // alone, since that's a real meal rather than eating one ingredient plain.
        Optional<IngredientCategories.Combo> combo = IngredientCategories.pickProteinAndCarb(ctx.getHaveIngredients());
        if (combo.isPresent()) {
            String proteinLabel = iconPrefix(combo.get().protein())
                    + IngredientTranslations.translate(combo.get().protein(), lang);
            String carbLabel = iconPrefix(combo.get().carb())
                    + IngredientTranslations.translate(combo.get().carb(), lang);
            sendWithMainMenu(chatId, lang,
                    Messages.get(lang, "cook.nothing_matches_no_shop_suggest_combo", proteinLabel, carbLabel));
            return;
        }
        Optional<String> pick = IngredientCategories.pickProteinOrCarb(ctx.getHaveIngredients());
        if (pick.isPresent()) {
            String label = iconPrefix(pick.get()) + IngredientTranslations.translate(pick.get(), lang);
            sendWithMainMenu(chatId, lang, Messages.get(lang, "cook.nothing_matches_no_shop_suggest", label));
            return;
        }
        String message = aiSuggestionMessage(ctx, lang, false).orElseGet(() -> {
            String haveList = ctx.getHaveIngredients().stream()
                    .map(i -> iconPrefix(i) + IngredientTranslations.translate(i, lang))
                    .collect(Collectors.joining(", "));
            return Messages.get(lang, "cook.nothing_matches_no_shop_raw", haveList);
        });
        sendWithMainMenu(chatId, lang, message);
    }

    /**
     * Tries the Claude-based suggestion first (only does anything if ANTHROPIC_API_KEY is
     * configured), then falls back to a free, no-signup recipe lookup via TheMealDB so this
     * fallback always works even without an API key.
     */
    Optional<String> aiSuggestionMessage(CookResultContext ctx, Lang lang, boolean canShop) {
        List<String> localizedHave = ctx.getHaveIngredients().stream()
                .map(i -> IngredientTranslations.translate(i, lang))
                .collect(Collectors.toList());
        Optional<String> aiText = aiSuggestionService.suggest(localizedHave, ctx.getTimeMinutes(), canShop, lang)
                .map(text -> Messages.get(lang, "cook.ai_suggestion", text));
        if (aiText.isPresent()) {
            return aiText;
        }
        List<String> englishHave = ctx.getHaveIngredients().stream()
                .map(i -> IngredientTranslations.translate(i, Lang.EN))
                .collect(Collectors.toList());
        return mealDbSuggestionService.suggest(englishHave)
                .map(text -> Messages.get(lang, "cook.recipe_search_suggestion", text));
    }

    void sendCookResultPage(long chatId, Lang lang, String group, int page, Integer editMessageId) {
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
                    + TimeFormat.format(food.getPrepTimeMinutes(), lang) + ")";
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

    void handleCookResultPageCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String remainder = data.substring(CB_COOK_RESULT_PAGE.length());
        int separatorIndex = remainder.lastIndexOf(':');
        String group = remainder.substring(0, separatorIndex);
        int page = Integer.parseInt(remainder.substring(separatorIndex + 1));
        answerCallback(callbackQuery.getId(), null, false);
        sendCookResultPage(chatId, lang, group, page, callbackQuery.getMessage().getMessageId());
    }

    void handleCookViewCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    String formatCookFoodDetail(Food food, CookResultContext ctx, Lang lang) {
        List<String> have = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String ingredient : food.getIngredients()) {
            boolean hasIt = effectivelyHas(ctx.getHaveIngredients(), ingredient);
            String formatted = formatIngredientWithAmount(ingredient, food.getIngredientAmounts(), lang);
            if (hasIt) {
                have.add(formatted);
            } else {
                missing.add(formatted);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("🍽️ ").append(FoodNameTranslations.translate(food.getName(), lang)).append("\n");
        builder.append("🏷️ ").append(categoryLabel(food.getCategory(), lang)).append("\n");
        builder.append("⏱️ ").append(TimeFormat.format(food.getPrepTimeMinutes(), lang)).append("\n\n");

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

    void sendCookTimePrompt(long chatId, Lang lang) {
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

    void handleCookTimePresetCallback(CallbackQuery callbackQuery, long chatId, String data) {
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

    void sendShoppingPrompt(long chatId, Lang lang) {
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

    void sendCookIngredientKeyboard(long chatId, CookSession session, Lang lang) {
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

    void editCookIngredientKeyboard(long chatId, CookSession session, Lang lang) {
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
}
