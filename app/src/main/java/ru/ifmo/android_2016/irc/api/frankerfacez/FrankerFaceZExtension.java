package ru.ifmo.android_2016.irc.api.frankerfacez;

import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.frankerfacez.emotes.FfzEmotes;
import ru.ifmo.android_2016.irc.client.Badge;
import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.MessageExtension;
import ru.ifmo.android_2016.irc.client.TwitchMessage;

/**
 * Created by ghost on 12/18/2016.
 */

public final class FrankerFaceZExtension implements MessageExtension {
    private Set<Integer> emoteSets;

    public FrankerFaceZExtension(Set<Integer> channelFfzEmotes) {
        this.emoteSets = channelFfzEmotes;
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
        return Stream.of(message.getSplitText())
                .filter(r -> FfzEmotes.isEmote(r.word, emoteSets))
                .map(r -> Emote.newEmote(
                        FfzEmotes.getEmoteUrl(r.word),
                        r.word,
                        r.begin,
                        r.end,
                        FfzEmotes.getWidth(r.word),
                        FfzEmotes.getHeight(r.word)))
                .collect(Collectors.toList());
    }
}