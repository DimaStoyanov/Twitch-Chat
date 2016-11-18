package ru.ifmo.android_2016.irc.api.twitch;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by ghost on 11/17/2016.
 */

public class TwitchEmotes {
    private final static Map<String, String> emotes = new HashMap<>();
    private final static Set<Integer> loadedSets = new HashSet<>();

    private TwitchEmotes() {
    }

    static void setEmotesByEmoteSet(int emoteSet, Map<String, String> emotes) {
        TwitchEmotes.emotes.putAll(emotes);
        TwitchEmotes.loadedSets.add(emoteSet);
    }

    static Set<Integer> getLoadedSets() {
        return loadedSets;
    }

    public static boolean isEmote(String word, Set<Integer> availableEmotes) {
        return emotes.containsKey(word);
    }

    public static String getEmoteByCode(String code) {
        return emotes.get(code);
    }
}
