package com.foodbot.food;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Orders recipe lists so the user's own, favorited and followed-people's recipes come first and
 * plain public foods are pushed to the end. The order within each group is left untouched, so
 * any earlier ranking (best cook match, search relevance) still applies inside the group.
 */
public final class RecipePriority {

    public static boolean isPriority(Food food, long chatId, Set<String> favoriteIds, Set<Long> followedIds) {
        if (food.getOwnerChatId() != null && food.getOwnerChatId() == chatId) {
            return true;
        }
        Long creator = food.getCreatedByChatId();
        if (creator != null && (creator == chatId || followedIds.contains(creator))) {
            return true;
        }
        return favoriteIds.contains(food.getId());
    }

    public static List<Food> publicLast(List<Food> foods, long chatId, Set<String> favoriteIds, Set<Long> followedIds) {
        List<Food> first = new ArrayList<>();
        List<Food> last = new ArrayList<>();
        for (Food food : foods) {
            (isPriority(food, chatId, favoriteIds, followedIds) ? first : last).add(food);
        }
        first.addAll(last);
        return first;
    }

    private RecipePriority() {
    }
}
