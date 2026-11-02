package com.foodbot.bot;

import com.foodbot.food.Food;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import com.foodbot.lang.TimeFormat;
import com.foodbot.social.UsernameRepository;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.foodbot.bot.BotConstants.*;

class FollowHandler extends FlowHandler {
    FollowHandler(BotContext ctx) {
        super(ctx);
    }

    void sendFriendsMenu(long chatId, Lang lang, Integer editMessageId) {
        boolean visible = followRepository.isVisible(chatId);
        InlineKeyboardButton follow = new InlineKeyboardButton(Messages.get(lang, "friends.btn_follow"));
        follow.setCallbackData(CB_FRIENDS_FOLLOW);
        InlineKeyboardButton list = new InlineKeyboardButton(
                Messages.get(lang, "friends.btn_list", followRepository.followingOf(chatId).size()));
        list.setCallbackData(CB_FRIENDS_LIST);
        InlineKeyboardButton toggle = new InlineKeyboardButton(
                Messages.get(lang, visible ? "friends.btn_hide" : "friends.btn_show"));
        toggle.setCallbackData(CB_FRIENDS_VISIBILITY);
        String text = Messages.get(lang, "friends.menu",
                Messages.get(lang, visible ? "friends.visible" : "friends.hidden"), followRepository.followersOf(chatId).size());
        InlineKeyboardButton digest = new InlineKeyboardButton(Messages.get(lang,
                followRepository.isDigest(chatId) ? "friends.btn_digest_on" : "friends.btn_digest_off"));
        digest.setCallbackData(CB_FRIENDS_DIGEST);
        InlineKeyboardButton followers = new InlineKeyboardButton(
                Messages.get(lang, "friends.btn_followers", followRepository.followersOf(chatId).size()));
        followers.setCallbackData(CB_FRIENDS_FOLLOWERS);
        InlineKeyboardButton card = new InlineKeyboardButton(Messages.get(lang, "friends.btn_my_profile"));
        card.setCallbackData(CB_PROFILE_VIEW + chatId);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>(
                List.of(List.of(card), List.of(follow), List.of(list, followers), List.of(toggle), List.of(digest)));
        if (ctx.botUsername != null) {
            InlineKeyboardButton share = new InlineKeyboardButton(Messages.get(lang, "friends.btn_share_profile"));
            share.setUrl(profileShareUrl(chatId, lang));
            rows.add(List.of(share));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, text, markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** Asks who to follow, offering Telegram's contact picker alongside typing an @username or ID. */
    void sendFollowPrompt(long chatId, Lang lang) {
        KeyboardButton pick = new KeyboardButton(Messages.get(lang, "friends.btn_pick_contact"));
        pick.setRequestUser(new KeyboardButtonRequestUser("1", false, null));
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(List.of(new KeyboardRow(List.of(pick))));
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        SendMessage message = new SendMessage(String.valueOf(chatId), Messages.get(lang, "friends.ask_id"));
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /** The user picked someone with the contact picker. */
    void handleUserShared(long chatId, long target, Lang lang, User from) {
        awaitingFollowId.remove(chatId);
        if (target != chatId && languageRepository.get(target) == null) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.unknown_user"));
            return;
        }
        followAndNotify(chatId, target, lang, from);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.menu_hint"));
    }

    /** Accepts a numeric Telegram ID, or an @username / t.me link of someone who has used the bot. */
    void handleFollowId(long chatId, String text, Lang lang, User from) {
        long target;
        String trimmed = text.trim();
        if (trimmed.matches("\\d+")) {
            target = Long.parseLong(trimmed);
        } else {
            String username = UsernameRepository.normalize(trimmed);
            if (username != null && username.matches("[a-z][a-z0-9_]{3,31}")
                    && ctx.usernameRepository.findChatId(username) != null) {
                target = ctx.usernameRepository.findChatId(username);
            } else if (trimmed.startsWith("@") || trimmed.toLowerCase().contains("t.me/")) {
                if (username == null || !username.matches("[a-z][a-z0-9_]{3,31}")) {
                    send(chatId, Messages.get(lang, "friends.bad_id"));
                    return;
                }
                awaitingFollowId.remove(chatId);
                sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.unknown_username", username));
                return;
            } else {
                List<Long> matches = ctx.usernameRepository.findByName(trimmed);
                if (matches.isEmpty()) {
                    awaitingFollowId.remove(chatId);
                    sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.unknown_name", trimmed));
                    return;
                }
                if (matches.size() > 1) {
                    send(chatId, Messages.get(lang, "friends.ambiguous_name", matches.size()));
                    return;
                }
                target = matches.get(0);
            }
        }
        awaitingFollowId.remove(chatId);
        if (target != chatId && languageRepository.get(target) == null) {
            sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.unknown_user"));
            return;
        }
        followAndNotify(chatId, target, lang, from);
        sendWithMainMenu(chatId, lang, Messages.get(lang, "friends.menu_hint"));
    }

    /** Follows (or requests to follow) {@code target}, replying to the follower and notifying the target. */
    private void followAndNotify(long chatId, long target, Lang lang, User from) {
        switch (followRepository.follow(chatId, target)) {
            case SELF -> send(chatId, Messages.get(lang, "friends.self"));
            case ALREADY_FOLLOWING -> send(chatId, Messages.get(lang, "friends.already", displayName(target)));
            case ALREADY_REQUESTED -> send(chatId, Messages.get(lang, "friends.already_requested"));
            case FOLLOWING -> {
                send(chatId, Messages.get(lang, "friends.followed", displayName(target)));
                Lang targetLang = lang(target);
                send(target, Messages.get(targetLang, "friends.new_follower", authorDisplayName(from, chatId)));
            }
            case REQUESTED -> {
                send(chatId, Messages.get(lang, "friends.request_sent"));
                sendFollowRequest(target, chatId, authorDisplayName(from, chatId));
            }
        }
    }

    void sendFollowRequest(long targetChatId, long requesterChatId, String requesterName) {
        Lang lang = lang(targetChatId);
        InlineKeyboardButton allow = new InlineKeyboardButton(Messages.get(lang, "friends.btn_allow"));
        allow.setCallbackData(CB_FRIENDS_APPROVE + requesterChatId);
        InlineKeyboardButton allowVisible = new InlineKeyboardButton(Messages.get(lang, "friends.btn_allow_visible"));
        allowVisible.setCallbackData(CB_FRIENDS_APPROVE_VISIBLE + requesterChatId);
        InlineKeyboardButton decline = new InlineKeyboardButton(Messages.get(lang, "friends.btn_decline"));
        decline.setCallbackData(CB_FRIENDS_DECLINE + requesterChatId);
        SendMessage message = new SendMessage(String.valueOf(targetChatId),
                Messages.get(lang, "friends.request", requesterName));
        message.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(allow, decline), List.of(allowVisible))));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void handleFriendsCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        Integer messageId = callbackQuery.getMessage().getMessageId();
        answerCallback(callbackQuery.getId(), null, false);
        if (data.equals(CB_FRIENDS_FOLLOW)) {
            addFoodSessions.remove(chatId);
            cookSessions.remove(chatId);
            editSessions.remove(chatId);
            awaitingFeedback.remove(chatId);
            awaitingShoppingItem.remove(chatId);
            awaitingSearch.remove(chatId);
            awaitingFollowId.add(chatId);
            sendFollowPrompt(chatId, lang);
        } else if (data.equals(CB_FRIENDS_VISIBILITY)) {
            followRepository.setVisible(chatId, !followRepository.isVisible(chatId));
            sendFriendsMenu(chatId, lang, messageId);
        } else if (data.equals(CB_FRIENDS_DIGEST)) {
            followRepository.setDigest(chatId, !followRepository.isDigest(chatId));
            sendFriendsMenu(chatId, lang, messageId);
        } else if (data.startsWith(CB_FRIENDS_MUTE)) {
            long userId = Long.parseLong(data.substring(CB_FRIENDS_MUTE.length()));
            followRepository.setMuted(chatId, userId, !followRepository.isMuted(chatId, userId));
            sendFollowingList(chatId, lang, messageId);
        } else if (data.equals(CB_FRIENDS_MENU)) {
            sendFriendsMenu(chatId, lang, messageId);
        } else if (data.equals(CB_FRIENDS_LIST)) {
            sendFollowingList(chatId, lang, messageId);
        } else if (data.equals(CB_FRIENDS_FOLLOWERS)) {
            sendFollowersList(chatId, lang, messageId);
        } else if (data.startsWith(CB_PROFILE_VIEW)) {
            sendProfile(chatId, lang, Long.parseLong(data.substring(CB_PROFILE_VIEW.length())), messageId);
        } else if (data.startsWith(CB_PROFILE_FOLLOW)) {
            long userId = Long.parseLong(data.substring(CB_PROFILE_FOLLOW.length()));
            if (userId != chatId && languageRepository.get(userId) == null) {
                send(chatId, Messages.get(lang, "friends.unknown_user"));
                return;
            }
            followAndNotify(chatId, userId, lang, callbackQuery.getFrom());
            sendProfile(chatId, lang, userId, messageId);
        } else if (data.startsWith(CB_PROFILE_UNFOLLOW)) {
            long userId = Long.parseLong(data.substring(CB_PROFILE_UNFOLLOW.length()));
            followRepository.unfollow(chatId, userId);
            sendProfile(chatId, lang, userId, messageId);
        } else if (data.startsWith(CB_FRIENDS_USER)) {
            long userId = Long.parseLong(data.substring(CB_FRIENDS_USER.length()));
            if (!followRepository.isFollowing(chatId, userId)) {
                sendFollowingList(chatId, lang, messageId);
                return;
            }
            ctx.browse.sendFoodListPage(chatId, lang, SCOPE_USER + userId, 0, null);
        } else if (data.startsWith(CB_FRIENDS_UNFOLLOW)) {
            followRepository.unfollow(chatId, Long.parseLong(data.substring(CB_FRIENDS_UNFOLLOW.length())));
            sendFollowingList(chatId, lang, messageId);
        } else {
            handleFollowRequestAnswer(chatId, lang, messageId, data);
        }
    }

    private InlineKeyboardButton backToFriendsButton(Lang lang) {
        InlineKeyboardButton back = new InlineKeyboardButton(Messages.get(lang, "btn.back"));
        back.setCallbackData(CB_FRIENDS_MENU);
        return back;
    }

    void sendFollowingList(long chatId, Lang lang, Integer editMessageId) {
        List<Long> following = followRepository.followingOf(chatId);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (long userId : following) {
            InlineKeyboardButton view = new InlineKeyboardButton("🍽️ " + displayName(userId));
            view.setCallbackData(CB_FRIENDS_USER + userId);
            InlineKeyboardButton unfollow = new InlineKeyboardButton(Messages.get(lang, "friends.btn_unfollow"));
            unfollow.setCallbackData(CB_FRIENDS_UNFOLLOW + userId);
            InlineKeyboardButton mute = new InlineKeyboardButton(Messages.get(lang,
                    followRepository.isMuted(chatId, userId) ? "friends.btn_unmute" : "friends.btn_mute"));
            mute.setCallbackData(CB_FRIENDS_MUTE + userId);
            rows.add(List.of(view, mute, unfollow));
        }
        rows.add(List.of(backToFriendsButton(lang)));
        String text = Messages.get(lang, following.isEmpty() ? "friends.list_empty" : "friends.list_header");
        editMessageTextAndMarkup(chatId, editMessageId, text, new InlineKeyboardMarkup(rows));
    }

    /** Upper bound so the list always fits Telegram's per-keyboard button limit (and its name lookups stay cheap). */
    private static final int MAX_LISTED_FOLLOWERS = 50;

    void sendFollowersList(long chatId, Lang lang, Integer editMessageId) {
        List<Long> followers = followRepository.followersOf(chatId);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (long userId : followers.subList(0, Math.min(followers.size(), MAX_LISTED_FOLLOWERS))) {
            InlineKeyboardButton profile = new InlineKeyboardButton("👤 " + displayName(userId));
            profile.setCallbackData(CB_PROFILE_VIEW + userId);
            rows.add(List.of(profile));
        }
        rows.add(List.of(backToFriendsButton(lang)));
        String text = followers.isEmpty() ? Messages.get(lang, "friends.followers_empty")
                : Messages.get(lang, "friends.followers_header")
                + (followers.size() > MAX_LISTED_FOLLOWERS
                ? "\n" + Messages.get(lang, "friends.followers_more", MAX_LISTED_FOLLOWERS, followers.size()) : "");
        editMessageTextAndMarkup(chatId, editMessageId, text, new InlineKeyboardMarkup(rows));
    }

    String profileDeepLink(long userId) {
        return "https://t.me/" + ctx.botUsername + "?start=" + START_PROFILE_PREFIX + userId;
    }

    /** Telegram's chat picker, prefilled with a one-line profile summary and the profile's deep link. */
    String profileShareUrl(long userId, Lang lang) {
        String name = plainName(userId);
        String text = Messages.get(lang, "profile.share_text", name != null ? name : Messages.get(lang, "profile.unknown_name"),
                foodRepository.countCreatedBy(userId), followRepository.followersOf(userId).size());
        return "https://t.me/share/url?url=" + URLEncoder.encode(profileDeepLink(userId), StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /**
     * Profile card for {@code userId}: counts, the latest recipe names (only for the owner and their followers,
     * like browsing) and a Follow / Unfollow button. Sent as a new message, or drawn over {@code editMessageId}.
     */
    void sendProfile(long chatId, Lang lang, long userId, Integer editMessageId) {
        if (userId != chatId && languageRepository.get(userId) == null) {
            send(chatId, Messages.get(lang, "profile.unavailable"));
            return;
        }
        boolean self = userId == chatId;
        boolean following = followRepository.isFollowing(chatId, userId);
        StringBuilder text = new StringBuilder(Messages.get(lang, "profile.card", displayName(userId),
                foodRepository.countCreatedBy(userId), followRepository.followersOf(userId).size(),
                followRepository.followingOf(userId).size()));
        if (ctx.botUsername != null) {
            text.append("\n🔗 ").append(profileDeepLink(userId));
        }
        if (self || following) {
            List<Food> owned = foodRepository.findCreatedBy(userId, lang);
            text.append("\n\n");
            if (owned.isEmpty()) {
                text.append(Messages.get(lang, "profile.empty"));
            } else {
                text.append(Messages.get(lang, "profile.latest"));
                List<Food> latest = owned.subList(Math.max(0, owned.size() - 3), owned.size());
                for (int i = latest.size() - 1; i >= 0; i--) {
                    Food food = latest.get(i);
                    text.append("\n").append(Messages.get(lang, "profile.item", food.getName(),
                            TimeFormat.format(food.getPrepTimeMinutes(), lang)));
                }
            }
        }
        if (!following) {
            text.append("\n\n").append(Messages.get(lang, "profile.features"));
        }
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (self) {
            text.append("\n\n").append(Messages.get(lang, "profile.you"));
        } else if (following) {
            InlineKeyboardButton recipes = new InlineKeyboardButton(Messages.get(lang, "profile.btn_recipes"));
            recipes.setCallbackData(CB_FRIENDS_USER + userId);
            InlineKeyboardButton unfollow = new InlineKeyboardButton(Messages.get(lang, "friends.btn_unfollow"));
            unfollow.setCallbackData(CB_PROFILE_UNFOLLOW + userId);
            rows.add(List.of(recipes, unfollow));
        } else {
            InlineKeyboardButton follow = new InlineKeyboardButton(Messages.get(lang, "profile.btn_follow"));
            follow.setCallbackData(CB_PROFILE_FOLLOW + userId);
            rows.add(List.of(follow));
        }
        if (ctx.botUsername != null) {
            InlineKeyboardButton share = new InlineKeyboardButton(Messages.get(lang, "friends.btn_share_profile"));
            share.setUrl(profileShareUrl(userId, lang));
            rows.add(List.of(share));
        }
        rows.add(List.of(backToFriendsButton(lang)));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, text.toString(), markup);
            return;
        }
        SendMessage message = new SendMessage(String.valueOf(chatId), text.toString());
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    void handleFollowRequestAnswer(long chatId, Lang lang, Integer messageId, String data) {
        boolean approve = data.startsWith(CB_FRIENDS_APPROVE);
        boolean makeVisible = data.startsWith(CB_FRIENDS_APPROVE_VISIBLE);
        String prefix = makeVisible ? CB_FRIENDS_APPROVE_VISIBLE : approve ? CB_FRIENDS_APPROVE : CB_FRIENDS_DECLINE;
        long requester = Long.parseLong(data.substring(prefix.length()));
        boolean handled = (approve || makeVisible)
                ? followRepository.approve(chatId, requester) : followRepository.decline(chatId, requester);
        if (!handled) {
            editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "friends.request_gone"),
                    new InlineKeyboardMarkup(List.of()));
            return;
        }
        if (makeVisible) {
            followRepository.setVisible(chatId, true);
        }
        String resultKey = (approve || makeVisible) ? "friends.request_allowed" : "friends.request_declined";
        editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, resultKey, displayName(requester)),
                new InlineKeyboardMarkup(List.of()));
        if (approve || makeVisible) {
            send(requester, Messages.get(lang(requester), "friends.request_accepted", displayName(chatId)));
        }
    }

    /** Sends each follower one message with every recipe queued for them since the last digest. */
    void sendDigests() {
        followRepository.drainDigests().forEach((follower, foodIds) -> {
            Lang lang = lang(follower);
            StringBuilder body = new StringBuilder();
            int count = 0;
            for (String foodId : foodIds) {
                Food food = foodRepository.findById(foodId).orElse(null);
                if (food == null) {
                    continue; // deleted since it was queued
                }
                Long owner = food.getOwnerChatId();
                String author = food.getAuthorName() != null ? food.getAuthorName()
                        : owner != null ? displayName(owner) : "";
                body.append("\n• ").append(food.getName());
                if (!author.isEmpty()) {
                    body.append(" — ").append(author);
                }
                count++;
            }
            if (count > 0) {
                send(follower, Messages.get(lang, "friends.digest", count) + "\n" + body);
            }
        });
    }
}
