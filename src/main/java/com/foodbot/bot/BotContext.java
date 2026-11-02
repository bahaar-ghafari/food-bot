package com.foodbot.bot;

import com.foodbot.ai.AiSuggestionService;
import com.foodbot.ai.MealDbSuggestionService;
import com.foodbot.food.AddFoodSession;
import com.foodbot.food.CookResultContext;
import com.foodbot.food.CookSession;
import com.foodbot.food.EditFoodSession;
import com.foodbot.food.FavoriteRepository;
import com.foodbot.food.Food;
import com.foodbot.food.FoodRepository;
import com.foodbot.food.SearchResultContext;
import com.foodbot.food.ShoppingListRepository;
import com.foodbot.lang.LanguageRepository;
import com.foodbot.social.FollowRepository;
import com.foodbot.social.UsernameRepository;

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
    final UsernameRepository usernameRepository;
    final FavoriteRepository favoriteRepository;
    final ShoppingListRepository shoppingListRepository;
    final Map<Long, AddFoodSession> addFoodSessions = new ConcurrentHashMap<>();
    final Map<Long, CookSession> cookSessions = new ConcurrentHashMap<>();
    final Map<Long, EditFoodSession> editSessions = new ConcurrentHashMap<>();
    final Map<Long, CookResultContext> lastCookResults = new ConcurrentHashMap<>();
    final Map<Long, SearchResultContext> lastSearchResults = new ConcurrentHashMap<>();
    final Map<Long, Food> pendingSearchSuggestion = new ConcurrentHashMap<>();
    final Map<Long, String> pendingSearchQuery = new ConcurrentHashMap<>();
    /** Recipe id from a deep link opened before the user had picked a language; shown once they do. */
    final Map<Long, String> pendingSharedRecipe = new ConcurrentHashMap<>();
    /** The bot's @username, used to build share links; share buttons are hidden while it is unset. */
    /** Profile chat id from a deep link opened before the user had picked a language; shown once they do. */
    final Map<Long, Long> pendingSharedProfile = new ConcurrentHashMap<>();
    volatile String botUsername;
    final Set<Long> awaitingFeedback = ConcurrentHashMap.newKeySet();
    final Set<Long> awaitingShoppingItem = ConcurrentHashMap.newKeySet();
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
    final ShoppingHandler shopping;
    final InlineQueryHandler inline;

    BotContext(ApiExecutor sender, Long superAdminChatId, Long feedbackChatId, FoodRepository foodRepository,
               LanguageRepository languageRepository, FollowRepository followRepository,
               UsernameRepository usernameRepository, FavoriteRepository favoriteRepository, ShoppingListRepository shoppingListRepository,
               AiSuggestionService aiSuggestionService, MealDbSuggestionService mealDbSuggestionService) {
        this.sender = sender;
        this.superAdminChatId = superAdminChatId;
        this.feedbackChatId = feedbackChatId;
        this.foodRepository = foodRepository;
        this.languageRepository = languageRepository;
        this.followRepository = followRepository;
        this.usernameRepository = usernameRepository;
        this.favoriteRepository = favoriteRepository;
        this.shoppingListRepository = shoppingListRepository;
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
        this.shopping = new ShoppingHandler(this);
        this.inline = new InlineQueryHandler(this);
    }
}
