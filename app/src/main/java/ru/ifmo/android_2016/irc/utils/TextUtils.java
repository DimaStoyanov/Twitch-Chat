package ru.ifmo.android_2016.irc.utils;

import android.graphics.Typeface;
import android.support.annotation.WorkerThread;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.List;

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
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(msg.getNickname());
        builder.append(msg.getAction() ? " " : ": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        List<TwitchMessage.Emote> emotes = msg.getEmotes();
        if (emotes == null) {
            return builder.append(msg.getTrailing());
        }
        SpannableStringBuilder builder1 = new SpannableStringBuilder();
        builder1.append(msg.getTrailing());
        TwitchMessage.Emote cur_emote;
        for (int i = 0; i < emotes.size(); i++) {
            cur_emote = emotes.get(i);
            builder1.setSpan(new DraweeSpan.Builder(cur_emote.getEmoteUrl())
                            .setLayout(50, 50).setShowAnimaImmediately(true).build(),
                    cur_emote.getBegin(), cur_emote.getEnd() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        builder.append(builder1);
        if (msg.getAction() && msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
