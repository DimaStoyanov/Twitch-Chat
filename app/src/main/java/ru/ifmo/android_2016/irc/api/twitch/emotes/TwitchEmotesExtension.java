package ru.ifmo.android_2016.irc.api.twitch.emotes;

import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.Set;

import ru.ifmo.android_2016.irc.client.Badge;
import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.MessageExtension;
import ru.ifmo.android_2016.irc.client.TwitchMessage;

/**
 * Created by ghost on 12/4/2016.
 */

public final class TwitchEmotesExtension implements MessageExtension {
    private final Set<Integer> emoteSets;

    public TwitchEmotesExtension(Set<Integer> emoteSets) {
        this.emoteSets = emoteSets;
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
                .filter(r -> TwitchEmotes.isEmote(r.word, emoteSets))
                .map(r -> Emote.newEmote(
                        TwitchEmotes.getEmoteUrl(r.word),
                        r.word,
                        r.begin,
                        r.end,
                        Emote.Type.TWITCH))
                .collect(Collectors.toList());
    }
}
