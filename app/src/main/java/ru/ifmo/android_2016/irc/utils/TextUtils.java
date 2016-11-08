package ru.ifmo.android_2016.irc.utils;

import android.graphics.Typeface;
import android.support.annotation.WorkerThread;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import java.util.List;

import ru.ifmo.android_2016.irc.api.TwitchApi;
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
        String msg_content = msg.getTrailing();
        int offset = 0;
        TwitchMessage.Emote cur_emote;
        for (int i = 0; i < emotes.size(); i++) {
            cur_emote = emotes.get(i);
            if (cur_emote.getLength() == 0) continue;
            if (offset < cur_emote.getBegin()) {
                builder.append(msg_content.substring(offset, cur_emote.getBegin()));
                Log.d(TAG, "inserting text" + offset + " " + cur_emote.getBegin());
                offset = cur_emote.getBegin();

            }
            if (offset == cur_emote.getBegin()) {
                int start = builder.length();
                builder.append(" [img]");
                builder.setSpan(new DraweeSpan.Builder(TwitchApi.getEmoticonUrl(cur_emote.getEmoteName()))
                                .setLayout(50, 50).build(),
                        start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(" ");
                offset += cur_emote.getLength();
            } else {
                throw new RuntimeException("Error while parsing message" + msg.text
                        + " with badges" + emotes.toString());
            }
        }
        if (offset < msg_content.length() - 1) {
            builder.append(msg_content.substring(offset));
        }
        if (msg.getAction() && msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
