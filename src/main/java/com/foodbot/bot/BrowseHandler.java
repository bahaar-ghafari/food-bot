package com.foodbot.bot;

import com.foodbot.food.Food;
import com.foodbot.food.FoodNameTranslations;
import com.foodbot.food.Paginator;
import com.foodbot.food.SearchResultContext;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import com.foodbot.lang.TimeFormat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.foodbot.bot.BotConstants.*;

class BrowseHandler extends FlowHandler {
    BrowseHandler(BotContext ctx) {
        super(ctx);
    }

    void sendFoodDetailMessage(long chatId, Lang lang, String scope, int page, Food food) {
        String text = formatFoodDetail(food, lang);
        List<InlineKeyboardButton> row = buildDetailRow(chatId, lang, scope, page, food);
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(row)));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void sendViewFoodsPrompt(long chatId, Lang lang) {
        InlineKeyboardButton mine = new InlineKeyboardButton(Messages.get(lang, "scope.mine"));
        mine.setCallbackData(CB_VIEW_FOODS_MINE);
        InlineKeyboardButton global = new InlineKeyboardButton(Messages.get(lang, "scope.global"));
        global.setCallbackData(CB_VIEW_FOODS_GLOBAL);
        InlineKeyboardButton all = new InlineKeyboardButton(Messages.get(lang, "scope.all"));
        all.setCallbackData(CB_VIEW_FOODS_ALL);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "foods.ask_scope"));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(mine, global), List.of(all))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void handleViewFoodsCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        answerCallback(callbackQuery.getId(), null, false);
        String scope = data.equals(CB_VIEW_FOODS_MINE) ? SCOPE_MINE
                : data.equals(CB_VIEW_FOODS_GLOBAL) ? SCOPE_GLOBAL
                : data.equals(CB_VIEW_FOODS_CREATED) ? SCOPE_CREATED
                : data.equals(CB_VIEW_FOODS_FAVORITES) ? SCOPE_FAVORITES : SCOPE_ALL;
        sendFoodListPage(chatId, lang, scope, 0, null);
    }

    void handleViewFoodsPageCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        String remainder = data.substring(CB_VIEW_FOODS_PAGE.length());
        int separatorIndex = remainder.lastIndexOf(':');
        String scope = remainder.substring(0, separatorIndex);
        int page = Integer.parseInt(remainder.substring(separatorIndex + 1));
        answerCallback(callbackQuery.getId(), null, false);
        sendFoodListPage(chatId, lang, scope, page, callbackQuery.getMessage().getMessageId());
    }

    void sendFoodListPage(long chatId, Lang lang, String scope, int page, Integer editMessageId) {
        List<Food> foods;
        String headerText;
        if (scope.equals(SCOPE_SEARCH)) {
            SearchResultContext ctx = lastSearchResults.get(chatId);
            foods = ctx != null ? ctx.getFoods() : List.of();
            headerText = Messages.get(lang, "search.results_header", ctx != null ? ctx.getQuery() : "");
        } else if (scope.equals(SCOPE_FAVORITES)) {
            foods = favoriteFoods(chatId, lang);
            headerText = Messages.get(lang, "foods.header.favorites");
        } else if (scope.startsWith(SCOPE_USER)) {
            long userId = Long.parseLong(scope.substring(SCOPE_USER.length()));
            // a follow can outlive its own approval (e.g. unfollowed in another message)
            foods = followRepository.isFollowing(chatId, userId) ? foodRepository.findOwnedBy(userId, lang) : List.of();
            headerText = Messages.get(lang, "friends.recipes_header", displayName(userId));
        } else if (scope.equals(SCOPE_ALL)) {
            foods = new ArrayList<>(foodRepository.findOwnedBy(chatId, lang));
            foods.addAll(foodRepository.findGlobal(lang));
            headerText = Messages.get(lang, "foods.header.all");
        } else if (scope.equals(SCOPE_CREATED)) {
            foods = foodRepository.findCreatedBy(chatId, lang);
            headerText = Messages.get(lang, "foods.header.created");
        } else {
            foods = scope.equals(SCOPE_MINE) ? foodRepository.findOwnedBy(chatId, lang) : foodRepository.findGlobal(lang);
            headerText = Messages.get(lang, scope.equals(SCOPE_MINE) ? "foods.header.mine" : "foods.header.global");
        }

        if (foods.isEmpty()) {
            sendWithMainMenu(chatId, lang,
                    Messages.get(lang, scope.equals(SCOPE_FAVORITES) ? "favorites.none" : "foods.none"));
            return;
        }

        int totalPages = Paginator.totalPages(foods.size(), FOODS_PER_PAGE);
        int clampedPage = Paginator.clampPage(page, totalPages);
        List<Food> pageItems = Paginator.pageSlice(foods, clampedPage, FOODS_PER_PAGE);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Food food : pageItems) {
            String label = FoodNameTranslations.translate(food.getName(), lang) + " ("
                    + TimeFormat.format(food.getPrepTimeMinutes(), lang) + ")";
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

        if (isSuperAdmin(chatId) && !scope.equals(SCOPE_SEARCH)) {
            InlineKeyboardButton reload = new InlineKeyboardButton(Messages.get(lang, "btn.admin_reload"));
            reload.setCallbackData(CB_ADMIN_RELOAD + scope + ":" + clampedPage);
            rows.add(List.of(reload));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);

        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, headerText, markup);
            return;
        }

        SendMessage message = new SendMessage(String.valueOf(chatId), headerText);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starred foods that this chat can still see (global, their own, or a followed user's) and
     * that are in {@code lang}, oldest star first. Deleted foods and lost access drop out quietly.
     */
    List<Food> favoriteFoods(long chatId, Lang lang) {
        List<Food> foods = new ArrayList<>();
        for (String id : favoriteRepository.idsOf(chatId)) {
            foodRepository.findById(id)
                    .filter(food -> food.getLanguage() == lang)
                    .filter(food -> canView(food, chatId))
                    .ifPresent(foods::add);
        }
        return foods;
    }

    private boolean canView(Food food, long chatId) {
        Long owner = food.getOwnerChatId();
        return owner == null || owner == chatId || followRepository.isFollowing(chatId, owner);
    }

    void handleFavoriteToggleCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        PagedRef ref = parsePagedRef(data.substring(CB_FOOD_FAVORITE.length()));
        Optional<Food> foodOpt = foodRepository.findById(ref.id());
        if (foodOpt.isEmpty() || !canView(foodOpt.get(), chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        boolean nowFavorite = favoriteRepository.toggle(chatId, ref.id());
        answerCallback(callbackQuery.getId(),
                Messages.get(lang, nowFavorite ? "favorites.added" : "favorites.removed"), false);
        List<InlineKeyboardButton> row = buildDetailRow(chatId, lang, ref.key(), ref.page(), foodOpt.get());
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(callbackQuery.getMessage().getMessageId());
        edit.setReplyMarkup(new InlineKeyboardMarkup(List.of(row)));
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void handleAdminReloadCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        if (!isSuperAdmin(chatId)) {
            answerCallback(callbackQuery.getId(), Messages.get(lang, "selection_expired"), false);
            return;
        }
        String remainder = data.substring(CB_ADMIN_RELOAD.length());
        int separatorIndex = remainder.lastIndexOf(':');
        String scope = remainder.substring(0, separatorIndex);
        int page = Integer.parseInt(remainder.substring(separatorIndex + 1));
        foodRepository.reload();
        answerCallback(callbackQuery.getId(), Messages.get(lang, "admin.reloaded"), false);
        sendFoodListPage(chatId, lang, scope, page, callbackQuery.getMessage().getMessageId());
    }

    String formatFoodDetail(Food food, Lang lang) {
        String ingredients = food.getIngredients().stream()
                .map(i -> formatIngredientWithAmount(i, food.getIngredientAmounts(), lang))
                .collect(Collectors.joining(", "));
        String timeText = TimeFormat.format(food.getPrepTimeMinutes(), lang);
        String recipeText = (food.getRecipe() == null || food.getRecipe().isBlank())
                ? Messages.get(lang, "food.no_recipe")
                : food.getRecipe();
        return "🍽️ " + FoodNameTranslations.translate(food.getName(), lang) + "\n"
                + "🏷️ " + Messages.get(lang, "food.detail_category", categoryLabel(food.getCategory(), lang)) + "\n"
                + "⏱️ " + Messages.get(lang, "food.detail_time", timeText) + "\n"
                + "🧾 " + Messages.get(lang, "food.detail_ingredients", ingredients) + "\n\n"
                + "📝 " + Messages.get(lang, "food.detail_recipe", recipeText) + "\n\n"
                + provenanceLine(food, lang) + "\n";
    }

    String provenanceLine(Food food, Lang lang) {
        String author = food.getAuthorName();
        if (food.isAddedViaBot()) {
            return author != null ? "🤖 " + Messages.get(lang, "food.source_bot_by", author)
                    : "🤖 " + Messages.get(lang, "food.source_bot");
        }
        return author != null ? "✍️ " + Messages.get(lang, "food.source_manual_by", author)
                : "✍️ " + Messages.get(lang, "food.source_manual");
    }

    void handleFoodViewCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
        List<InlineKeyboardButton> row = buildDetailRow(chatId, lang, ref.key(), ref.page(), food);
        editMessageTextAndMarkup(chatId, callbackQuery.getMessage().getMessageId(), text,
                new InlineKeyboardMarkup(List.of(row)));
    }

    List<InlineKeyboardButton> buildDetailRow(long chatId, Lang lang, String scope, int page, Food food) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_VIEW_FOODS_PAGE + scope + ":" + page);
        row.add(back);
        boolean favorite = favoriteRepository.isFavorite(chatId, food.getId());
        InlineKeyboardButton star = new InlineKeyboardButton(favorite ? "⭐" : "☆");
        star.setCallbackData(CB_FOOD_FAVORITE + scope + ":" + page + ":" + food.getId());
        row.add(star);
        if (canModify(food, chatId)) {
            InlineKeyboardButton settings = new InlineKeyboardButton(Messages.get(lang, "btn.settings"));
            settings.setCallbackData(CB_FOOD_SETTINGS + scope + ":" + page + ":" + food.getId());
            row.add(settings);
        }
        return row;
    }

    void handleFoodSettingsCallback(CallbackQuery callbackQuery, long chatId, String data) {
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
}
