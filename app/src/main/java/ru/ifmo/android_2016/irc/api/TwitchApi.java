package ru.ifmo.android_2016.irc.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.BASE_CHAT_URI;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.BASE_URI;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.CLIENT_ID;

/**
 * Created by Dima Stoyanov on 24.10.2016.
 * Project Android-IRC
 * Start time : 22:40
 */

public class TwitchApi {


    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения общий эмоций.
     * @throws IOException
     */
    public static HttpURLConnection getTwitchGlobalEmotiсons() throws IOException {
        Uri uri = BASE_CHAT_URI.buildUpon().appendPath("emotes").appendPath("emoticons").appendQueryParameter("client_id", CLIENT_ID).build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();

    }

    /**
     * @param channel Канал twich
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения эмоций канала.
     * @throws IOException
     */
    public static HttpURLConnection getTwitchChannelEmotiсons(@NonNull String channel) throws IOException {
        if("global".equals(channel))
            return getTwitchGlobalEmotiсons();
        Uri uri = BASE_CHAT_URI.buildUpon().appendPath(channel).appendPath("emoticons").appendQueryParameter("client_id", CLIENT_ID).build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }


    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения эмоций канала.
     * @throws IOException
     */
    public static HttpURLConnection getAllTwitchEmoticons() throws IOException {
        Uri uri = BASE_CHAT_URI.buildUpon().appendPath("emoticons").build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    /**
     * @param token OAuth token пользователя
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения информации о пользователе.
     * @throws IOException
     */
    public static HttpURLConnection getUserTwitchRequest(String token) throws IOException {
        Uri uri = BASE_URI.buildUpon().appendQueryParameter("oauth_token", token).build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

}
