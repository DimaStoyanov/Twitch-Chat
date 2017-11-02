package ru.ifmo.android_2016.irc.api.twitch.badges;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

import ru.ifmo.android_2016.irc.client.Badge;
import ru.ifmo.android_2016.irc.client.Emote;
import ru.ifmo.android_2016.irc.client.MessageExtension;
import ru.ifmo.android_2016.irc.client.TwitchMessage;

/**
 * Created by ghost on 12/3/2016.
 */

public final class TwitchBadgesExtension implements MessageExtension {
    @NonNull
    private final Map<String, String> badges;

    public TwitchBadgesExtension(@NonNull Map<String, String> badges) {
        this.badges = badges;
    }

    @Nullable
    @Override
    public List<Badge> setBadges(TwitchMessage message) {
        if (message.getBadges() != null) {
            for (Badge badge : message.getBadges()) {
                String url = badges.get(badge.toString());
                if (url != null) {
                    badge.setUrl(url);
                }
            }
        }
        return message.getBadges();
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
        return null;
    }
}
