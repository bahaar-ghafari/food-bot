package com.foodbot.bot;

import com.foodbot.food.Food;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        String text = Messages.get(lang, "friends.menu", chatId,
                Messages.get(lang, visible ? "friends.visible" : "friends.hidden"), followRepository.followersOf(chatId).size());
        InlineKeyboardButton digest = new InlineKeyboardButton(Messages.get(lang,
                followRepository.isDigest(chatId) ? "friends.btn_digest_on" : "friends.btn_digest_off"));
        digest.setCallbackData(CB_FRIENDS_DIGEST);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                List.of(List.of(follow, list), List.of(toggle), List.of(digest)));
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

    void handleFollowId(long chatId, String text, Lang lang, User from) {
        long target;
        try {
            target = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            send(chatId, Messages.get(lang, "friends.bad_id"));
            return;
        }
        awaitingFollowId.remove(chatId);
        if (target != chatId && languageRepository.get(target) == null) {
            send(chatId, Messages.get(lang, "friends.unknown_user"));
            return;
        }
        switch (followRepository.follow(chatId, target)) {
            case SELF -> send(chatId, Messages.get(lang, "friends.self"));
            case ALREADY_FOLLOWING -> send(chatId, Messages.get(lang, "friends.already", displayName(target)));
            case ALREADY_REQUESTED -> send(chatId, Messages.get(lang, "friends.already_requested"));
            case FOLLOWING -> {
                send(chatId, Messages.get(lang, "friends.followed", displayName(target)));
                Lang targetLang = lang(target);
                send(target, Messages.get(targetLang, "friends.new_follower", authorDisplayNameOrId(from, chatId)));
            }
            case REQUESTED -> {
                send(chatId, Messages.get(lang, "friends.request_sent"));
                sendFollowRequest(target, chatId, authorDisplayNameOrId(from, chatId));
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
            awaitingSearch.remove(chatId);
            awaitingFollowId.add(chatId);
            send(chatId, Messages.get(lang, "friends.ask_id"));
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
        } else if (data.equals(CB_FRIENDS_LIST)) {
            sendFollowingList(chatId, lang, messageId);
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
        String text = Messages.get(lang, following.isEmpty() ? "friends.list_empty" : "friends.list_header");
        editMessageTextAndMarkup(chatId, editMessageId, text, new InlineKeyboardMarkup(rows));
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
