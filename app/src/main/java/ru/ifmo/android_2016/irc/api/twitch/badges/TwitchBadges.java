package ru.ifmo.android_2016.irc.api.twitch.badges;

import android.support.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.utils.Log;

/**
 * Created by ghost on 11/15/2016.
 */

public class TwitchBadges {
    static final URL NEW_API_GLOBAL_BADGES_URL;
    static final String NEW_API_CHANNEL_BADGES_URL_TEMPLATE =
            "https://badges.twitch.tv/v1/badges/channels/{{room-id}}/display";
    static final String KRAKEN_BADGES_URL_TEMPLATE =
            "https://api.twitch.tv/kraken/chat/{{channel}}/badges";

    static final Map<String, String> badges = new HashMap<>();

    static {
        URL url = null;
        try {
            url = new URL("https://badges.twitch.tv/v1/badges/global/display");
        } catch (MalformedURLException e) {
            //impossible
        }
        NEW_API_GLOBAL_BADGES_URL = url;
    }

    private TwitchBadges() {
    }

    @Nullable
    public static String getBadgeUrl(String badge) {
        if (badges.get(badge) == null) {
            //TODO:
            Log.d("TwitchBadges", badge);
            return "";
        }
        return badges.get(badge);
    }
}
