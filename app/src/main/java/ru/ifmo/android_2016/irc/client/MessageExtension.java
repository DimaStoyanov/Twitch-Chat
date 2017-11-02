package ru.ifmo.android_2016.irc.client;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by ghost on 12/3/2016.
 */
//TODO: default methods on API 15?
public interface MessageExtension {
    @Nullable
    List<Badge> setBadges(TwitchMessage message);

    @Nullable
    List<Badge> addBadges(TwitchMessage message);

    @Nullable
    List<Emote> setEmotes(TwitchMessage message);

    @Nullable
    List<Emote> addEmotes(TwitchMessage message);
}
