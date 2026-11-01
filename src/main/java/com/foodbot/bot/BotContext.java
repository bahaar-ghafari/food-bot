package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.CookSession;
import com.foodbot.food.EditFoodSession;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.SearchResultContext;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.social.FollowRepository;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.foodbot.bot.BotConstants.*;

/** State and collaborators shared by every flow handler. */
class BotContext {
    final ApiExecutor sender;
    final Long superAdminChatId;
    final Long feedbackChatId;
    final FoodRepository foodRepository;
    final LanguageRepository languageRepository;
    final FollowRepository followRepository;
    final Map<Long, AddFoodSession> addFoodSessions = new ConcurrentHashMap<>();
    final Map<Long, CookSession> cookSessions = new ConcurrentHashMap<>();
    final Map<Long, EditFoodSession> editSessions = new ConcurrentHashMap<>();
    final Map<Long, CookResultContext> lastCookResults = new ConcurrentHashMap<>();
    final Map<Long, SearchResultContext> lastSearchResults = new ConcurrentHashMap<>();
    final Map<Long, Food> pendingSearchSuggestion = new ConcurrentHashMap<>();
    final Map<Long, String> pendingSearchQuery = new ConcurrentHashMap<>();
    final Set<Long> awaitingFeedback = ConcurrentHashMap.newKeySet();
    final Set<Long> awaitingSearch = ConcurrentHashMap.newKeySet();
    final Set<Long> awaitingFollowId = ConcurrentHashMap.newKeySet();
    final AiSuggestionService aiSuggestionService;
    final MealDbSuggestionService mealDbSuggestionService;

    final FlowHandler ui;
    final BrowseHandler browse;
    final FollowHandler follow;
    final FeedbackHandler feedback;
    final SearchHandler search;
    final AddFoodHandler addFood;
    final EditFoodHandler editFood;
    final CookHandler cook;

    BotContext(ApiExecutor sender, Long superAdminChatId, Long feedbackChatId, FoodRepository foodRepository,
               LanguageRepository languageRepository, FollowRepository followRepository,
               AiSuggestionService aiSuggestionService, MealDbSuggestionService mealDbSuggestionService) {
        this.sender = sender;
        this.superAdminChatId = superAdminChatId;
        this.feedbackChatId = feedbackChatId;
        this.foodRepository = foodRepository;
        this.languageRepository = languageRepository;
        this.followRepository = followRepository;
        this.aiSuggestionService = aiSuggestionService;
        this.mealDbSuggestionService = mealDbSuggestionService;
        this.ui = new FlowHandler(this);
        this.browse = new BrowseHandler(this);
        this.follow = new FollowHandler(this);
        this.feedback = new FeedbackHandler(this);
        this.search = new SearchHandler(this);
        this.addFood = new AddFoodHandler(this);
        this.editFood = new EditFoodHandler(this);
        this.cook = new CookHandler(this);
    }
}
