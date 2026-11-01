package com.foodbot.bot;

import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.objects.User;

import static com.foodbot.bot.BotConstants.*;

class FeedbackHandler extends FlowHandler {
    FeedbackHandler(BotContext ctx) {
        super(ctx);
    }

    void handleFeedbackText(long chatId, String text, Lang lang, User from) {
        awaitingFeedback.remove(chatId);
        if (feedbackChatId != null) {
            String who = (from != null && from.getUserName() != null) ? "@" + from.getUserName()
                    : (from != null && from.getFirstName() != null) ? from.getFirstName() : String.valueOf(chatId);
            Lang feedbackLang = lang(feedbackChatId);
            send(feedbackChatId, Messages.get(feedbackLang, "feedback.notify_admin", who, text));
        }
        sendWithMainMenu(chatId, lang, Messages.get(lang, "feedback.sent"));
    }
}
