package ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ghost on 11/16/2016.
 */

public class BttvEmotes {
    @NonNull
    private final static Map<String, String> globalEmotes = new HashMap<>();
    @NonNull
    private final static Map<String, Map<String, String>> channelEmotes = new HashMap<>();
    @Nullable
    private static String EMOTE_URL_TEMPLATE;

    @SuppressWarnings("unchecked")
    private final static List<Map<String, String>> urlCache = Arrays.asList(new HashMap[3]);

    static {
        for (int i : new int[]{0, 1, 2}) urlCache.set(i, new HashMap<>());
    }

    private BttvEmotes() {
    }

    @NonNull
    public static Map<String, String> getGlobalEmotes() {
        return globalEmotes;
    }

    public static List<String> getEmotes(String channel) {
        List<String> wut = new ArrayList<>();

        wut.addAll(getChannelEmotes(channel).keySet());
        wut.addAll(getGlobalEmotes().keySet());

        return wut;
    }

    public static String getEmoteUrlByCode(String code, String channel) {
        return getEmoteUrl(getEmoteByCode(code, channel), 3);
    }

    static void setGlobalEmotes(@NonNull Map<String, String> globalEmotes) {
        BttvEmotes.globalEmotes.clear();
        BttvEmotes.globalEmotes.putAll(globalEmotes);
    }

    @NonNull
    static Map<String, String> getChannelEmotes(String channel) {
        Map<String, String> result = channelEmotes.get(channel);
        if (result == null) return Collections.emptyMap();
        return result;
    }

    static void setChannelEmotes(String channel, Map<String, String> channelEmotes) {
        BttvEmotes.channelEmotes.put(channel, channelEmotes);
    }

    static void setEmoteUrlTemplate(String emoteUrlTemplate) {
        BttvEmotes.EMOTE_URL_TEMPLATE = emoteUrlTemplate;
    }

    /**
     * Возвращает URL ссылку на изобренрие emoticon с заданным id заданного размера.
     *
     * @param id   Номер картинки
     * @param size Размер картинки. Один из {1, 2, 3}
     * @return URL ссылка на картинку
     */
    @SuppressWarnings("WeakerAccess")
    public static String getEmoteUrl(String id, @IntRange(from = 1, to = 3) int size) {
        if (urlCache.get(size - 1).containsKey(id)) return urlCache.get(size - 1).get(id);
        if (id == null) {
            throw new RuntimeException("Incorrect emote id");
        }
        if (EMOTE_URL_TEMPLATE != null) {
            String url = "https:" + EMOTE_URL_TEMPLATE
                    .replace("{{id}}", id)
                    .replace("{{image}}", size + "x");
            urlCache.get(size - 1).put(id, url);
            return url;
        }
        throw null; //TODO: Я не знаю, нужно ли здесь кидать эксепшн или просто вернуть нулл
    }

    public static String getEmoteUrl(String id) {
        return getEmoteUrl(id, 2);
    }

    public static boolean isEmote(String code, String channel) {
        if (code == null) return false;

        Map<String, String> channelEmotes = BttvEmotes.channelEmotes.get(channel);

        return (!globalEmotes.isEmpty() && globalEmotes.containsKey(code)) ||
                (channelEmotes != null && channelEmotes.containsKey(code));
    }

    public static String getEmoteByCode(String code, String channel) {
        if (code == null) return null;

        if (!globalEmotes.isEmpty()) {
            if (globalEmotes.containsKey(code)) return globalEmotes.get(code);
        }

        Map<String, String> channelEmotes = BttvEmotes.channelEmotes.get(channel);
        if (channelEmotes != null) {
            if (channelEmotes.containsKey(code)) return channelEmotes.get(code);
        }

        return null;
    }
}
