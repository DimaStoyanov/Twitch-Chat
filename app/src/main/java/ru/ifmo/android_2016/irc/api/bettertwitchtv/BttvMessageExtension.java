package ru.ifmo.android_2016.irc.api.bettertwitchtv;

import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes.BttvEmotes;
import ru.ifmo.android_2016.irc.client.Badge;
import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.MessageExtension;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost on 12/3/2016.
 */

public final class BttvMessageExtension implements MessageExtension {
    private final Map<String, String> channelEmotes;

    public BttvMessageExtension(Map<String, String> channelEmotes) {
        this.channelEmotes = channelEmotes;
    }

    @Nullable
    @Override
    public List<Badge> setBadges(TwitchMessage message) {
        return null;
    }

    @Nullable
    @Override
    public List<Badge> addBadges(TwitchMessage message) {
        return null;
    }

    @Nullable
    @Override
    public List<Emote> setEmotes(TwitchMessage message) {
        return null;
    }

    @Nullable
    @Override
    public List<Emote> addEmotes(TwitchMessage message) {
        List<Emote> newEmotes = null;
        if (channelEmotes != null) {
            if (newEmotes == null) {
                newEmotes = new ArrayList<>();
            }
            for (Splitter.Result result : message.getSplitText()) {
                if (channelEmotes.containsKey(result.word)) {
                    String id = channelEmotes.get(result.word);
                    String url = BttvEmotes.getEmoteUrl(id);
                    newEmotes.add(Emote.newEmote(
                            url,
                            id,
                            result.begin,
                            result.end,
                            25,
                            25));
                }
            }
        }
        if (!BttvEmotes.getGlobalEmotes().isEmpty()) {
            if (newEmotes == null) {
                newEmotes = new ArrayList<>();
            }
            Map<String, String> globalEmotes = BttvEmotes.getGlobalEmotes();

            for (Splitter.Result result : message.getSplitText()) {
                if (globalEmotes.containsKey(result.word)) {
                    String id = globalEmotes.get(result.word);
                    String url = BttvEmotes.getEmoteUrl(id);
                    newEmotes.add(Emote.newEmote(
                            url,
                            id,
                            result.begin,
                            result.end,
                            25,
                            25));
                }
            }
        }
        return newEmotes;
    }
}
