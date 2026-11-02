package com.foodbot.bot;

/** Callback-data prefixes and limits shared by the bot's flow handlers. */
final class BotConstants {
    private BotConstants() {
    }

    static final int INGREDIENTS_PER_PAGE = 12;
    static final String INGREDIENT_PAGE_INFIX = "page:";
    static final int FOODS_PER_PAGE = 10;
    static final String SCOPE_MINE = "mine";
    static final String SCOPE_GLOBAL = "global";
    static final String SCOPE_ALL = "all";
    static final String SCOPE_SEARCH = "search";
    static final String SCOPE_CREATED = "created";
    static final String SCOPE_FAVORITES = "fav";
    static final String CB_SEARCH_FUZZY_YES = "srfy";
    static final String CB_SEARCH_FUZZY_NO = "srfn";
    static final String CB_SEARCH_OTHERLANG_YES = "sroy";
    static final String CB_SEARCH_OTHERLANG_NO = "sron";

    static final String CB_LANG = "lang:";
    static final String CB_ADDFOOD_CATEGORY = "afc:";
    static final String CB_ADDFOOD_INGREDIENT = "afi:";
    static final String CB_ADDFOOD_INGREDIENT_DONE = "afi:done";
    static final String CB_ADDFOOD_INGREDIENT_CLEAR = "afi:clear";

    static final String CB_COOK_INGREDIENT = "cki:";
    static final String CB_COOK_INGREDIENT_DONE = "cki:done";
    static final String CB_COOK_INGREDIENT_CLEAR = "cki:clear";
    static final String CB_COOK_SHOP_YES = "cks:yes";
    static final String CB_COOK_SHOP_NO = "cks:no";
    static final String CB_COOK_CATEGORY = "ckc:";
    static final String CB_COOK_TIME_PRESET = "ckt:";
    static final String CB_COOK_VIEW = "cv:";
    static final String CB_COOK_RESULT_PAGE = "ckrp:";
    /** Followed by the food id: adds that dish's missing ingredients to the shopping list. */
    static final String CB_SHOP_ADD = "shadd:";
    static final String CB_SHOP_OPEN = "shl:open";
    /** Followed by the item's index in the list. */
    static final String CB_SHOP_TOGGLE = "shl:t:";
    static final String CB_SHOP_CLEAR = "shl:clear";
    static final String CB_SHOP_ADD_ITEM = "shl:add";
    static final String GROUP_READY = "ready";
    static final String GROUP_SHOP = "shop";
    static final String[] COOK_TIME_PRESET_KEYS =
            {"cook.time.fast", "cook.time.2h", "cook.time.5h", "cook.time.1day", "cook.time.any"};
    static final int[] COOK_TIME_PRESET_MINUTES = {30, 120, 300, 1440, Integer.MAX_VALUE};
    static final String CATEGORY_ANY = "ANY";
    static final String CB_VIEW_FOODS_MINE = "vf:mine";
    static final String CB_VIEW_FOODS_GLOBAL = "vf:global";
    static final String CB_VIEW_FOODS_ALL = "vf:all";
    static final String CB_VIEW_FOODS_CREATED = "vf:created";
    static final String CB_VIEW_FOODS_FAVORITES = "vf:fav";
    static final String CB_VIEW_FOODS_PAGE = "vfp:";
    static final String CB_ADMIN_RELOAD = "adm:reload:";

    static final String CB_FOOD_EDIT_START = "fe:";
    static final String CB_FOOD_EDIT_FIELD = "fef:";
    static final String CB_FOOD_EDIT_CATEGORY = "fec:";
    static final String CB_FOOD_EDIT_INGREDIENT = "fei:";
    static final String CB_FOOD_EDIT_INGREDIENT_DONE = "fei:done";
    static final String CB_FOOD_EDIT_INGREDIENT_CLEAR = "fei:clear";
    static final String CB_FOOD_EDIT_RECIPE_CLEAR = "ferclear";
    static final String CB_FOOD_EDIT_RECIPE_DONE = "ferdone";
    static final String CB_FOOD_EDIT_RECIPE_STEP = "fers:";
    static final String CB_FOOD_EDIT_SAVE = "fesave";
    static final String CB_FOOD_EDIT_EXIT = "feexit";
    static final String CB_FOOD_EDIT_PHOTO_SKIP = "fephskip";
    static final String CB_FOOD_EDIT_PHOTO_REMOVE = "fephrm";
    static final String CB_FOOD_EDIT_AMOUNT = "feam:";
    static final String CB_FOOD_EDIT_AMOUNT_DONE = "feam:done";
    static final String CB_FOOD_DELETE_START = "fd:";
    static final String CB_FOOD_DELETE_CONFIRM_YES = "fdy:";
    static final String CB_FOOD_DELETE_CONFIRM_NO = "fdn:";
    static final String CB_ADDFOOD_RECIPE_DONE = "afrdone";
    static final String CB_ADDFOOD_RECIPE_STEP = "afrs:";
    static final String CB_ADDFOOD_AMOUNT = "afam:";
    static final String CB_ADDFOOD_AMOUNT_DONE = "afam:done";
    static final String CB_ADDFOOD_PHOTO_SKIP = "afpskip";
    static final String CB_ADDFOOD_PHOTO_REMOVE = "afpdel";
    static final String CB_ADDFOOD_BACK = "afback";
    static final String CB_ADDFOOD_CANCEL = "afcancel";
    static final String CB_ADDFOOD_REVIEW_EDIT = "afrv:";
    static final String CB_ADDFOOD_CONFIRM = "afconfirm";
    static final String CB_FRIENDS_FOLLOW = "frf";
    static final String CB_FRIENDS_LIST = "frl";
    static final String CB_FRIENDS_VISIBILITY = "frv";
    static final String CB_FRIENDS_USER = "fru:";
    static final String CB_FRIENDS_UNFOLLOW = "frun:";
    static final String CB_FRIENDS_APPROVE = "fra:";
    static final String CB_FRIENDS_APPROVE_VISIBLE = "frav:";
    static final String CB_FRIENDS_DECLINE = "frd:";
    static final String CB_FRIENDS_DIGEST = "frdg";
    static final String CB_FRIENDS_MUTE = "frm:";
    static final String CB_FRIENDS_FOLLOWERS = "frfs";
    static final String CB_FRIENDS_MENU = "frb";
    /** Followed by a chat id: shows that user's profile card. */
    static final String CB_PROFILE_VIEW = "frp:";
    static final String CB_PROFILE_FOLLOW = "frpf:";
    static final String CB_PROFILE_UNFOLLOW = "frpu:";
    /** Scope prefix for browsing a followed user's recipes; the rest is their chat id. */
    static final String SCOPE_USER = "usr";
    /** Prefix of the {@code /start} payload that opens a shared recipe: {@code recipe_<id>}. */
    static final String START_RECIPE_PREFIX = "recipe_";
    /** Prefix of the {@code /start} payload that opens a shared profile: {@code user_<chat id>}. */
    static final String START_PROFILE_PREFIX = "user_";
    static final String CB_FOOD_VIEW = "fv:";
    static final String CB_FOOD_SETTINGS = "fset:";
    static final String CB_FOOD_FAVORITE = "ffav:";
    /** Followed by {@code <factor>:<scope>:<page>:<id>}; factor is one of {@link #SCALE_FACTORS}. */
    static final String CB_FOOD_SCALE = "fsc:";
    static final double[] SCALE_FACTORS = {0.5, 1, 2, 3};
    static final int RECIPE_STEP_CHAR_LIMIT = 150;
    static final int AMOUNT_CHAR_LIMIT = 30;
    static final String MAINTENANCE_MESSAGE =
            "⚠️ Something went wrong on our end. Please try again in a couple of minutes — we might be doing maintenance.\n\n"
                    + "⚠️ مشکلی پیش آمد. لطفاً چند دقیقه دیگر دوباره امتحان کنید — ممکن است در حال به‌روزرسانی باشیم.";
}
