package ru.ifmo.android_2016.irc.api.frankerfacez.emotes;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.frankerfacez.FrankerFaceZParser;

/**
 * Created by ghost on 12/18/2016.
 */

public final class FfzEmotes {
    private FfzEmotes() {
    }

    private static final String EMOTES_BASE_URI = "https://cdn.frankerfacez.com/emoticon/{id}/2";

    static boolean globalLoaded = false;
    final static Set<Integer> defaultSets = new HashSet<>();
    final static Map<String, String> emotes = new HashMap<>();
    final static Map<String, Integer> emoteSet = new HashMap<>();
    final static Set<Integer> loadedSets = new HashSet<>();

    public static String getEmoteUrl(String id) {
        return EMOTES_BASE_URI.replace("{id}", emotes.get(id));
    }

    public static void addEmotes(List<FrankerFaceZParser.Set> sets) {
        for (FrankerFaceZParser.Set set : sets) {
            Map<String, String> emotes = set.getEmotes();

            FfzEmotes.emotes.putAll(emotes);
            FfzEmotes.loadedSets.add(set.getId());
            FfzEmotes.emoteSet.putAll(Stream.of(emotes.keySet())
                    .collect(Collectors.toMap(v -> v, v -> set.getId())));
        }
    }

    public static boolean isEmote(String word, Set<Integer> availableEmotes) {
        return emotes.containsKey(word) && availableEmotes.contains(emoteSet.get(word));
    }
}
