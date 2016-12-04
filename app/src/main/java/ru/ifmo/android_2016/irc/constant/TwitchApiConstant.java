package ru.ifmo.android_2016.irc.constant;

import android.net.Uri;

/**
 * Created by Dima Stoyanov on 30.10.2016.
 * Project Android-IRC
 * Start time : 19:49
 */

public final class TwitchApiConstant {
    public static final Uri BASE_URI = Uri.parse("https://api.twitch.tv/kraken/");
    public static final Uri BASE_CHAT_URI = Uri.parse("https://api.twitch.tv/kraken/chat");
    public static final String EMOTICON_URI = "http://static-cdn.jtvnw.net/emoticons/v1/";
    public static final String EMOTE_SMALL = "1.0";
    public static final String EMOTE_MEDIUM = "2.0";
    public static final String EMOTE_LARGE = "3.0";
    public static final String CLIENT_ID = "7v810qqrz2gguvfb0pp3qiuoni06yoc";
    public static final String REDIRECT_URL = "http://localhost";
    public static final String OAUTH_URL = Uri.parse("https://api.twitch.tv/kraken/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("scope", "user_read chat_login").build().toString();
}
