package ru.ifmo.android_2016.irc.utils;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.text.SimpleDateFormat;
import java.util.Date;

import ru.ifmo.android_2016.irc.client.Badge;
import ru.ifmo.android_2016.irc.client.Bits;
import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.Message;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.ui.span.ChangeableForegroundColorSpan;

/**
 * Created by ghost on 11/8/2016.
 */

public final class TextUtils {
    private final static String TAG = TextUtils.class.getSimpleName();

    private TextUtils() {
    }

    public interface TextFunction {
        Spanned apply(TwitchMessage msg);
    }

    @WorkerThread
    public static SpannableStringBuilder buildMessage(@NonNull TwitchMessage msg) {
        SpannableStringBuilder time = buildTime(msg);
        SpannableStringBuilder badges = buildBadges(msg);
        SpannableStringBuilder nickname = buildNickname(msg);
        SpannableStringBuilder message = buildMessageTextWithSomeShit(msg);

        return new SpannableStringBuilder()
                .append(time).append(' ')
                .append(badges)
                .append(nickname).append(' ')
                .append(message);
    }

    private static SpannableStringBuilder buildNickname(@NonNull TwitchMessage msg) {
        SpannableStringBuilder nickname = new SpannableStringBuilder();
        nickname.append(msg.getNickname())
                .append(msg.isAction() ? "" : ":");

        nickname.setSpan(new StyleSpan(Typeface.BOLD), 0, nickname.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        nickname.setSpan(new ChangeableForegroundColorSpan(msg.getColor()), 0, nickname.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return nickname;
    }

    private static SpannableStringBuilder buildTime(@NonNull TwitchMessage msg) {
        SpannableStringBuilder time = new SpannableStringBuilder()
                .append(new SimpleDateFormat("hh:mm:ss").format(new Date(msg.getTime())));
        time.setSpan(new RelativeSizeSpan(0.65f), 0, time.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return time;
    }

    @WorkerThread
    private static SpannableStringBuilder buildBadges(@NonNull TwitchMessage msg) {
        SpannableStringBuilder badges = new SpannableStringBuilder();
        if (msg.getBadges() != null) {
            for (Badge badge : msg.getBadges()) {
                SpannableStringBuilder b = new SpannableStringBuilder().append("  ");
                b.setSpan(
                        new DraweeSpan.Builder(badge.getUrl())
                                .setLayout(50, 50)
                                .build(),
                        0,
                        1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                badges.append(b);
            }
        }
        return badges;
    }

    @NonNull
    private static SpannableStringBuilder buildMessageTextWithSomeShit(@NonNull TwitchMessage msg) {
        SpannableStringBuilder messageText = new SpannableStringBuilder();
        messageText.append(msg.getTrailing());
        if (msg.isAction()) {
            messageText.setSpan(new ChangeableForegroundColorSpan(msg.getColor()), 0, messageText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (msg.getEmotes() != null) {
            for (Emote emote : msg.getEmotes()) {
                messageText.setSpan(
                        new DraweeSpan.Builder(emote.getEmoteUrl())
                                .setLayout(50, 50)
                                .setShowAnimaImmediately(true)    //LAAAAAGGGGSSSS!!! NotLikeThis
                                .build(),
                        emote.getBegin(),
                        emote.getEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (msg.getBits() != null) {
            for (Bits bits : msg.getBits()) {
                messageText.setSpan(
                        new DraweeSpan.Builder(bits.getBitsUrl())
                                .setLayout(50, 50)
                                .setShowAnimaImmediately(true)
                                .build(),
                        bits.getImageBegin(),
                        bits.getImageEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                messageText.setSpan(
                        new ChangeableForegroundColorSpan(bits.getColor()),
                        bits.getNumberBegin(),
                        bits.getNumberEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
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
    public static Spanned buildBanText(TwitchMessage msg) {
        return new SpannableStringBuilder().append(msg.getBan().toString(msg.getTrailing()));
    }

    @WorkerThread
    public static Spanned buildNotice(TwitchMessage msg) {
        return new SpannableStringBuilder().append(msg.getTrailing());
    }

    @WorkerThread
    public static Spanned buildColoredText(String msg, int color) {
        SpannableStringBuilder result = new SpannableStringBuilder(msg);
        result.setSpan(new ChangeableForegroundColorSpan(color), 0, msg.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return result;
    }

    @WorkerThread
    public static Spanned buildWhisper(TwitchMessage msg) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder()
                .append(msg.getNickname())
                .append(" -> ")
                .append(msg.getPrivmsgTarget())
                .append(": ");

        spannableStringBuilder.setSpan(new ChangeableForegroundColorSpan(msg.getColor()), 0,
                spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableStringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0,
                spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableStringBuilder.append(buildMessageTextWithSomeShit(msg));
    }

    public static String removePunct(String string) {
        return string.replaceFirst("^\\p{Punct}", "");
    }
}
