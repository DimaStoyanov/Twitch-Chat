package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.api.BetterTwitchTvApi;
import ru.ifmo.android_2016.irc.api.TwitchApi;
import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost on 11/12/2016.
 */

public class Emote implements Comparable<Emote> {
    private static final String TAG = Emote.class.getSimpleName();

    private static Pattern pattern = Pattern.compile("([\\w\\\\()-]+):(?:\\d+-\\d+)(?:,\\d+-\\d+)*");
    private static Pattern range = Pattern.compile("(\\d+)-(\\d+)");
    private final String emoteUrl;
    private final int begin;
    private final int end;

    private Emote(String emoteId, int begin, int end) {
        this.emoteUrl = emoteId;
        this.begin = begin;
        this.end = end;
    }

    static Emote getTwitchEmote(String emoteName, int begin, int end) {
        return new Emote(TwitchApi.getEmoteUrl(emoteName), begin, end);
    }

    static Emote getBttvEmote(String emoteName, int begin, int end) {
        return new Emote(BetterTwitchTvApi.getEmoteUrl(emoteName), begin, end);
    }

    @Override
    public int compareTo(@NonNull Emote o) {
        return this.begin - o.begin;
    }

    public String getEmoteUrl() {
        return emoteUrl;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public int getLength() {
        return end - begin + 1;
    }

    @Override
    public String toString() {
        return "[" + getBegin() + "-" + getEnd() + "]:" + getEmoteUrl();
    }

    @Nullable
    static List<Emote> parse(String emotes, List<Splitter.Result> splitResult, String channel) {
        List<Emote> result = new ArrayList<>(4);
        if (emotes != null) {
            String[] emote = emotes.split("/");

            for (String e : emote) {
                Matcher matcher = Emote.pattern.matcher(e);
                if (matcher.matches()) {
                    String[] p = e.split(":");
                    String eId = p[0];
                    for (String range : p[1].split(",")) {
                        Matcher matcher1 = Emote.range.matcher(range);
                        if (matcher1.matches()) {
                            result.add(Emote.getTwitchEmote(
                                    eId,
                                    Integer.parseInt(matcher1.group(1)),
                                    Integer.parseInt(matcher1.group(2))));
                        }
                    }
                } else {
                    throw null; //TODO: Something bad happened
                }
            }
        }

        /* BetterTTV */
        if (splitResult != null) {
            parseBttvEmotes(result, splitResult, BetterTwitchTvApi.globalEmotes);
            parseBttvEmotes(result, splitResult, BetterTwitchTvApi.getChannelEmotes(channel));
        }

        if (result.size() > 0) {
            return result;
        } else {
            return null;
        }
    }

    private static void parseBttvEmotes(final List<Emote> result, List<Splitter.Result> splitResult,
                                        Map<String, String> bttvEmotes) {
        if (bttvEmotes != null) {
            Stream.of(splitResult)
                    .filter((r) -> bttvEmotes.containsKey(r.word))
                    .forEach((r) ->
                            result.add(Emote.getBttvEmote(bttvEmotes.get(r.word), r.begin, r.end)));
        }
    }
}