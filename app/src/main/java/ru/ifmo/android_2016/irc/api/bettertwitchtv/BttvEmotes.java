package ru.ifmo.android_2016.irc.api.bettertwitchtv;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ghost on 11/16/2016.
 */

public class BttvEmotes {
    private static final String DEFAULT_SIZE = "2x";
    private static Map<String, String> globalEmotes = null;
    private final static Map<String, Map<String, String>> channelEmotes = new HashMap<>();
    private static String EMOTE_URL_TEMPLATE;

    private BttvEmotes() {
    }

    static Map<String, String> getGlobalEmotes() {
        return globalEmotes;
    }

    static Object[] getGlobalEmotesKey() {
        return globalEmotes.keySet().toArray();
    }

    public static Object[] getChannelEmotesKey(String channel) {
        if (getChannelEmotes(channel) == null)
            return getGlobalEmotesKey();
        Object[] chkeys = getChannelEmotes(channel).keySet().toArray();
        Object[] glkeys = getGlobalEmotesKey();
        Object[] keys = new Object[chkeys.length + glkeys.length];
        System.arraycopy(chkeys, 0, keys, 0, chkeys.length);
        System.arraycopy(glkeys, 0, keys, chkeys.length, glkeys.length);
        return keys;
    }

    public static String getEmoteUrlByCode(String code, String channel) {
        return getEmoteUrl(getEmoteByCode(code, channel), "3x");
    }

    static void setGlobalEmotes(Map<String, String> globalEmotes) {
        BttvEmotes.globalEmotes = globalEmotes;
    }

    static Map<String, String> getChannelEmotes(String channel) {
        return channelEmotes.get(channel);
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
     * @param size Размер картинки. Один из {1x, 2x, 3x}
     * @return URL ссылка на картинку
     */
    public static String getEmoteUrl(String id, String size) {
        if (EMOTE_URL_TEMPLATE != null) {
            return "https:" + EMOTE_URL_TEMPLATE
                    .replace("{{id}}", id)
                    .replace("{{image}}", size);
        }
        throw null; //TODO: Я не знаю, нужно ли здесь кидать эксепшн или просто вернуть нулл
    }

    public static String getEmoteUrl(String id) {
        return getEmoteUrl(id, DEFAULT_SIZE);
    }

    public static boolean isEmote(String code, String channel) {
        if (code == null) return false;

        Map<String, String> channelEmotes = BttvEmotes.channelEmotes.get(channel);

        return (globalEmotes != null && globalEmotes.containsKey(code)) ||
                (channelEmotes != null && channelEmotes.containsKey(code));
    }

    public static String getEmoteByCode(String code, String channel) {
        if (code == null) return null;

        if (globalEmotes != null) {
            if (globalEmotes.containsKey(code)) return globalEmotes.get(code);
        }

        Map<String, String> channelEmotes = BttvEmotes.channelEmotes.get(channel);
        if (channelEmotes != null) {
            if (channelEmotes.containsKey(code)) return channelEmotes.get(code);
        }

        return null;
    }
}
