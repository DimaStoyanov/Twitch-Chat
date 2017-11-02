package ru.ifmo.android_2016.irc.api.frankerfacez.emotes;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.Collections;
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
    final static Set<Integer> defaultSets = Collections.synchronizedSet(new HashSet<>());
    //code -> id
    private final static Map<String, String> emotes = Collections.synchronizedMap(new HashMap<>());
    //code -> setId
    private final static Map<String, Set<Integer>> codeToSet = new HashMap<>();
    //roomName -> roomId?
    private final static Map<String, Integer> roomIds = new HashMap<>();

    private final static Map<Integer, Set<String>> lul = new HashMap<>();

    public static String getEmoteUrl(String code) {
        return EMOTES_BASE_URI.replace("{id}", emotes.get(code));
    }

    public static void addEmotes(List<FrankerFaceZParser.Set> sets) {
        for (FrankerFaceZParser.Set set : sets) {
            Map<String, String> emotes = set.getEmotes();
            Set<String> strings = emotes.keySet();

            FfzEmotes.emotes.putAll(emotes);
            FfzEmotes.lul.put(set.getId(), strings);
            for (String code : strings) {
                if (!codeToSet.containsKey(code)) {
                    codeToSet.put(code, new HashSet<>());
                }
                codeToSet.get(code).add(set.getId());
            }
        }
    }

    public static boolean isEmote(String word, Set<Integer> availableEmotes) {
        if (emotes.containsKey(word)) {
            boolean channel = !Collections.disjoint(availableEmotes, codeToSet.get(word));
            boolean global = !Collections.disjoint(defaultSets, codeToSet.get(word));
            return channel || global;
        }
        return false;
    }

    public static List<String> getEmotes(String channel) {
        Set<Integer> integers = new HashSet<>();
        integers.addAll(defaultSets);
        integers.add(roomIds.get(channel));

        return Stream.of(lul)
                .filter(e -> integers.contains(e.getKey()))
                .flatMap(e -> Stream.of(e.getValue()))
                .collect(Collectors.toList());
    }

    public static void addRoomId(String room, int id) {
        roomIds.put(room, id);
    }

    public static int getWidth(String word) {
        return 25;  //TODO:
    }

    public static int getHeight(String word) {
        return 25;  //TODO:
    }
}