package ru.ifmo.android_2016.irc.utils;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.Message;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;

/**
 * Created by ghost on 11/8/2016.
 */

public final class TextUtils {
    private final static String TAG = TextUtils.class.getSimpleName();

    private TextUtils() {
    }

    @WorkerThread
    public static SpannableStringBuilder buildTextDraweeView(TwitchMessage msg) {
        SpannableStringBuilder nickNBadges = new SpannableStringBuilder();

        nickNBadges.append(msg.getNickname());
        nickNBadges.append(msg.getAction() ? " " : ": ");

        nickNBadges.setSpan(new StyleSpan(Typeface.BOLD), 0, nickNBadges.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (msg.getColor() != 0) {
            nickNBadges.setSpan(new ForegroundColorSpan(msg.getColor()), 0, nickNBadges.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return nickNBadges.append(buildMessageTextWithEmotes(msg));
    }

    @NonNull
    private static SpannableStringBuilder buildMessageTextWithEmotes(TwitchMessage msg) {
        SpannableStringBuilder messageText = new SpannableStringBuilder();
        messageText.append(msg.getTrailing());
        if (msg.getAction() && msg.getColor() != 0) {
            messageText.setSpan(new ForegroundColorSpan(msg.getColor()), 0, messageText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (msg.getEmotes() != null) {
            for (Emote emote : msg.getEmotes()) {
                messageText.setSpan(
                        new DraweeSpan.Builder(emote.getEmoteUrl())
                                .setLayout(50, 50)
                                //.setShowAnimaImmediately(true)    //LAAAAAGGGGSSSS!!! NotLikeThis
                                .build(),
                        emote.getBegin(),
                        emote.getEnd() + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return messageText;
    }

    @WorkerThread
    public static SpannableStringBuilder buildDefaultText(Message msg) {
        return new SpannableStringBuilder().append("<").append(msg.getNickname()).append("> ")
                .append(msg.getTrailing());
    }

    @WorkerThread
    public static CharSequence buildBanText(TwitchMessage msg) {
        return new SpannableStringBuilder().append(msg.getBan().toString(msg.getTrailing()));
    }

    @WorkerThread
    public static CharSequence buildNotice(TwitchMessage msg) {
        return new SpannableStringBuilder().append(msg.getTrailing());
    }

    @WorkerThread
    public static Spanned buildColoredText(String msg, int color) {
        SpannableStringBuilder result = new SpannableStringBuilder(msg);
        result.setSpan(new ForegroundColorSpan(color), 0, msg.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return result;
    }

    @WorkerThread
    public static CharSequence buildWhisper(TwitchMessage msg) {
        return new SpannableStringBuilder()
                .append(msg.getNickname())
                .append(" \u25B6 ")
                .append(msg.getPrivmsgTarget())
                .append(": ")
                .append(buildMessageTextWithEmotes(msg));

    }
}
