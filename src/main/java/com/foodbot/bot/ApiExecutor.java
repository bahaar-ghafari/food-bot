package com.foodbot.bot;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;

/** The one Telegram operation the flow handlers need; lets tests record calls instead of hitting the network. */
@FunctionalInterface
interface ApiExecutor {
    <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) throws TelegramApiException;
}
