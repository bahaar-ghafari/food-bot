package com.foodbot.bot;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

/** The Telegram operations the flow handlers need; lets tests record calls instead of hitting the network. */
interface ApiExecutor {
    <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) throws TelegramApiException;

    /** Photos aren't a {@link BotApiMethod}, so they get their own entry point; a no-op unless overridden. */
    default Message sendPhoto(SendPhoto photo) throws TelegramApiException {
        return null;
    }
}
