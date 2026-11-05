package com.foodbot.bot;

import com.foodbot.food.IngredientTranslations;
import com.foodbot.lang.Lang;
import com.foodbot.lang.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.foodbot.bot.BotConstants.*;

/**
 * Shopping-list partners: two people who share one list. Linking is by invite link and needs the
 * invitee's approval; each side can name the other and gets a ping when the other adds items.
 */
class PartnerHandler extends FlowHandler {
    static final int MAX_NAME_LENGTH = 30;

    PartnerHandler(BotContext ctx) {
        super(ctx);
    }

    /** What {@code viewer} calls their partner: the nickname they gave, else the partner's Telegram name. */
    String partnerName(long viewer) {
        String nickname = partnerRepository.nicknameOf(viewer);
        if (nickname != null) {
            return nickname;
        }
        Long partner = partnerRepository.partnerOf(viewer);
        String telegramName = partner == null ? null : plainName(partner);
        return telegramName != null ? telegramName : Messages.get(lang(viewer), "partner.default_name");
    }

    String inviteLink(long chatId) {
        return "https://t.me/" + ctx.botUsername + "?start=" + START_PARTNER_PREFIX + chatId;
    }

    /** Telegram's chat picker, prefilled with the invitation text and link. */
    String inviteShareUrl(long chatId, Lang lang) {
        String name = plainName(chatId);
        String text = Messages.get(lang, "partner.share_text", name != null ? name : Messages.get(lang, "profile.unknown_name"));
        return "https://t.me/share/url?url=" + URLEncoder.encode(inviteLink(chatId), StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    /** The partner screen: how to invite someone, or who the list is shared with and how to rename/stop. */
    void sendPartnerMenu(long chatId, Lang lang, Integer editMessageId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        String text;
        if (partnerRepository.partnerOf(chatId) == null) {
            text = Messages.get(lang, "partner.intro");
            if (ctx.botUsername != null) {
                InlineKeyboardButton invite = new InlineKeyboardButton(Messages.get(lang, "partner.btn_invite"));
                invite.setUrl(inviteShareUrl(chatId, lang));
                rows.add(List.of(invite));
            }
        } else {
            text = Messages.get(lang, "partner.linked", partnerName(chatId));
            rows.add(List.of(button(Messages.get(lang, "partner.btn_rename"), CB_PARTNER_NAME)));
            rows.add(List.of(button(Messages.get(lang, "partner.btn_stop"), CB_PARTNER_STOP)));
        }
        rows.add(List.of(button(Messages.get(lang, "partner.btn_list"), CB_SHOP_OPEN)));
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(rows);
        if (editMessageId != null) {
            editMessageTextAndMarkup(chatId, editMessageId, text, markup);
        } else {
            sendWithMarkup(chatId, text, markup);
        }
    }

    /** Someone opened an invite link: asks the invitee to accept (after checking the invite still makes sense). */
    void handleInvite(long chatId, Lang lang, long inviterId) {
        if (inviterId == chatId) {
            send(chatId, Messages.get(lang, "partner.invite_self"));
            return;
        }
        Long current = partnerRepository.partnerOf(chatId);
        if (current != null) {
            send(chatId, Messages.get(lang, current == inviterId ? "partner.already_with_them" : "partner.already_other",
                    partnerName(chatId)));
            return;
        }
        if (partnerRepository.partnerOf(inviterId) != null || languageRepository.get(inviterId) == null) {
            send(chatId, Messages.get(lang, "partner.invite_stale"));
            return;
        }
        String inviter = plainName(inviterId);
        sendWithMarkup(chatId,
                Messages.get(lang, "partner.invite_text", inviter != null ? inviter : Messages.get(lang, "profile.unknown_name")),
                new InlineKeyboardMarkup(List.of(List.of(
                        button(Messages.get(lang, "partner.btn_accept"), CB_PARTNER_ACCEPT + inviterId),
                        button(Messages.get(lang, "partner.btn_decline"), CB_PARTNER_DECLINE + inviterId)))));
    }

    void handleCallback(CallbackQuery callbackQuery, long chatId, String data) {
        Lang lang = lang(chatId);
        int messageId = callbackQuery.getMessage().getMessageId();
        answerCallback(callbackQuery.getId(), null, false);
        if (data.equals(CB_PARTNER_MENU)) {
            sendPartnerMenu(chatId, lang, messageId);
        } else if (data.equals(CB_PARTNER_NAME)) {
            if (partnerRepository.partnerOf(chatId) == null) {
                sendPartnerMenu(chatId, lang, messageId);
                return;
            }
            clearPendingInput(chatId);
            ctx.awaitingPartnerName.add(chatId);
            sendWithHomeButton(chatId, lang, Messages.get(lang, "partner.name_prompt"));
        } else if (data.equals(CB_PARTNER_STOP)) {
            editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "partner.stop_confirm", partnerName(chatId)),
                    new InlineKeyboardMarkup(List.of(List.of(
                            button(Messages.get(lang, "partner.btn_stop_yes"), CB_PARTNER_STOP_YES),
                            button(Messages.get(lang, "partner.btn_keep"), CB_PARTNER_MENU)))));
        } else if (data.equals(CB_PARTNER_STOP_YES)) {
            stopSharing(chatId, lang, messageId);
        } else if (data.startsWith(CB_PARTNER_ACCEPT)) {
            Long inviter = parseId(data.substring(CB_PARTNER_ACCEPT.length()));
            if (inviter == null) {
                return;
            }
            accept(chatId, lang, messageId, inviter);
        } else if (data.startsWith(CB_PARTNER_DECLINE)) {
            editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "partner.declined"),
                    new InlineKeyboardMarkup(List.of()));
        }
    }

    private void accept(long chatId, Lang lang, int messageId, long inviter) {
        if (inviter == chatId || partnerRepository.partnerOf(chatId) != null
                || partnerRepository.partnerOf(inviter) != null || languageRepository.get(inviter) == null) {
            editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "partner.invite_stale"),
                    new InlineKeyboardMarkup(List.of()));
            return;
        }
        // Both people's existing lists become one list, stored under the lower chat id.
        long owner = Math.min(chatId, inviter);
        shoppingListRepository.mergeInto(Math.max(chatId, inviter), owner);
        partnerRepository.link(chatId, inviter);

        editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "partner.accepted", partnerName(chatId)),
                new InlineKeyboardMarkup(List.of(List.of(button(Messages.get(lang, "partner.btn_list"), CB_SHOP_OPEN)))));
        Lang inviterLang = lang(inviter);
        sendWithMarkup(inviter, Messages.get(inviterLang, "partner.accepted_notice", partnerName(inviter)),
                new InlineKeyboardMarkup(List.of(
                        List.of(button(Messages.get(inviterLang, "partner.btn_rename"), CB_PARTNER_NAME)),
                        List.of(button(Messages.get(inviterLang, "partner.btn_list"), CB_SHOP_OPEN)))));
        // let the invitee name them straight away; ignoring the prompt keeps the Telegram name
        clearPendingInput(chatId);
        ctx.awaitingPartnerName.add(chatId);
        sendWithHomeButton(chatId, lang, Messages.get(lang, "partner.name_prompt"));
    }

    private void stopSharing(long chatId, Lang lang, int messageId) {
        Long former = partnerRepository.partnerOf(chatId);
        if (former == null) {
            sendPartnerMenu(chatId, lang, messageId);
            return;
        }
        long owner = partnerRepository.listOwner(chatId);
        String formerName = partnerName(chatId);
        partnerRepository.unlink(chatId);
        // both keep a copy of the list as it is now
        shoppingListRepository.copyList(owner, owner == chatId ? former : chatId);
        ctx.pendingPartnerNotice.remove(chatId);
        ctx.pendingPartnerNotice.remove(former);
        ctx.awaitingPartnerName.remove(former);
        editMessageTextAndMarkup(chatId, messageId, Messages.get(lang, "partner.stopped", formerName),
                new InlineKeyboardMarkup(List.of(List.of(button(Messages.get(lang, "partner.btn_list"), CB_SHOP_OPEN)))));
        Lang formerLang = lang(former);
        send(former, Messages.get(formerLang, "partner.stopped_notice"));
    }

    /** The user typed the name they call their partner. */
    void handleNameText(long chatId, String text, Lang lang) {
        String name = text.trim();
        if (partnerRepository.partnerOf(chatId) == null) {
            ctx.awaitingPartnerName.remove(chatId);
            sendPartnerMenu(chatId, lang, null);
            return;
        }
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            send(chatId, Messages.get(lang, "partner.name_invalid", MAX_NAME_LENGTH));
            return;
        }
        partnerRepository.setNickname(chatId, name);
        ctx.awaitingPartnerName.remove(chatId);
        sendWithMarkup(chatId, Messages.get(lang, "partner.name_saved", name),
                new InlineKeyboardMarkup(List.of(List.of(button(Messages.get(lang, "partner.btn_list"), CB_SHOP_OPEN)))));
    }

    /** Pings the user's partner (if any) that {@code items} were added to the shared list. */
    void notifyAdded(long chatId, Collection<String> items) {
        Long partner = partnerRepository.partnerOf(chatId);
        if (partner == null || items.isEmpty()) {
            return;
        }
        Lang partnerLang = lang(partner);
        String names = items.stream().map(item -> IngredientTranslations.translate(item, partnerLang))
                .collect(Collectors.joining(", "));
        sendWithMarkup(partner, Messages.get(partnerLang, "partner.notify_added", partnerName(partner), names),
                new InlineKeyboardMarkup(List.of(List.of(button(Messages.get(partnerLang, "partner.btn_list"), CB_SHOP_OPEN)))));
    }

    private static Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(label);
        button.setCallbackData(callbackData);
        return button;
    }

    private void sendWithMarkup(long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage(String.valueOf(chatId), text);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
