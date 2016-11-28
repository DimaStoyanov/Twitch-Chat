package ru.ifmo.android_2016.irc.utils;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.graphics.ColorUtils;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.ifmo.android_2016.irc.api.twitch.TwitchBadges;
import ru.ifmo.android_2016.irc.client.Badge;
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

        SpannableStringBuilder time = new SpannableStringBuilder()
                .append(new SimpleDateFormat("hh:mm:ss").format(new Date(msg.getTime())))
                .append(' ');
        time.setSpan(new RelativeSizeSpan(0.65f), 0, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        nickNBadges.append(buildBadges(msg));
        nickNBadges.append(msg.getNickname());
        nickNBadges.append(msg.getAction() ? " " : ": ");

        nickNBadges.setSpan(new StyleSpan(Typeface.BOLD), 0, nickNBadges.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int color = msg.getColor();
        if (color != 0) {
            float[] hsl = new float[3];
            ColorUtils.colorToHSL(color, hsl);
            hsl[2] = (float) (180. / 256.); //TODO: это подходит только для черной темы
            color = ColorUtils.HSLToColor(hsl);

            nickNBadges.setSpan(new ForegroundColorSpan(color), 0, nickNBadges.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return time.append(nickNBadges.append(buildMessageTextWithEmotes(msg, color)));
    }

    private static SpannableStringBuilder buildBadges(TwitchMessage msg) {
        SpannableStringBuilder badges = new SpannableStringBuilder();
        if (msg.getBadges() != null) {
            for (Badge badge : msg.getBadges()) {
                SpannableStringBuilder b = new SpannableStringBuilder().append("  ");
                b.setSpan(
                        new DraweeSpan.Builder(badge.getUrl()).setLayout(50, 50).build(),
                        0,
                        1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                badges.append(b);
            }
        }
        return badges;
    }

    @NonNull
    private static SpannableStringBuilder buildMessageTextWithEmotes(TwitchMessage msg, int color) {
        SpannableStringBuilder messageText = new SpannableStringBuilder();
        messageText.append(msg.getTrailing());
        if (msg.getAction() && color != 0) {
            messageText.setSpan(new ForegroundColorSpan(color), 0, messageText.length(),
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
                .append(buildMessageTextWithEmotes(msg, 0));

    }
}
