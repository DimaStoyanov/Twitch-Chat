package ru.ifmo.android_2016.irc.api;

import android.net.Uri;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.BASE_URI;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.CLIENT_ID;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.EMOTE_MEDIUM;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.EMOTICON_URI;

/**
 * Created by Dima Stoyanov on 24.10.2016.
 * Project Android-IRC
 * Start time : 22:40
 */

public class TwitchApi {

    private static Uri.Builder getBaseUriBuilder() {
        return BASE_URI.buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID);
    }

    /**
     * @param token OAuth token пользователя
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения информации о пользователе.
     * @throws IOException
     */
    public static HttpURLConnection getUserTwitchRequest(String token) throws IOException {
        Uri uri = BASE_URI.buildUpon()
                .appendQueryParameter("oauth_token", token)
                .build();

        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }


    /**
     * Возвращает URL ссылку на изобренрие emoticon с заданным id заданного размера.
     *
     * @param id   Номер картинки
     * @param size Размер картинки. Один из {1.0, 2.0, 3.0}
     * @return URL ссылка на картинку
     */
    public static String getEmoteUrl(String id, String size) {
        return EMOTICON_URI + id + "/" + size;
    }

    public static String getEmoteUrl(String id) {
        return getEmoteUrl(id, EMOTE_MEDIUM);
    }


    public static HttpURLConnection getEmoticonImages(Set<Integer> set) throws IOException {
        String sets = Stream.of(set)
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Uri uri = getBaseUriBuilder()
                .appendPath("chat")
                .appendPath("emoticon_images")
                .appendQueryParameter("emotesets", sets)
                .build();

        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    public static HttpURLConnection getChannel(String channel) throws IOException {
        Uri uri = getBaseUriBuilder()
                .appendPath("channels")
                .appendPath(channel)
                .build();

        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }
}
