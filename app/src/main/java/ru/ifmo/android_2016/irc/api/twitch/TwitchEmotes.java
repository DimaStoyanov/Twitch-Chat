package ru.ifmo.android_2016.irc.api.twitch;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ghost on 11/17/2016.
 */

public class TwitchEmotes {
    private final static Map<String, String> emotes = new HashMap<>();
    private final static Map<String, Integer> emoteSet = new HashMap<>();
    private final static Set<Integer> loadedSets = new HashSet<>();

    private static List<String> lazyGlobalEmotesList;

    private TwitchEmotes() {
    }

    static void addEmotes(int emoteSet, Map<String, String> emotes) {
        TwitchEmotes.emotes.putAll(emotes);
        TwitchEmotes.loadedSets.add(emoteSet);
        TwitchEmotes.emoteSet.putAll(Stream.of(emotes.keySet())
                .collect(Collectors.toMap(v -> v, v -> emoteSet)));
    }

    static Set<Integer> getLoadedSets() {
        return loadedSets;
    }

    public static boolean isEmote(String word, Set<Integer> availableEmotes) {
        return emotes.containsKey(word) && availableEmotes.contains(emoteSet.get(word));
    }

    public static String getEmoteByCode(String code) {
        return emotes.get(code);
    }

    public static List<String> getGlobalEmotesList() {
        //TODO: slow? not thread safe?
        return lazyGlobalEmotesList != null ? lazyGlobalEmotesList :
                (lazyGlobalEmotesList = Stream.of(emoteSet)
                        .filter(e -> e.getValue() == 0)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()));
    }
}
