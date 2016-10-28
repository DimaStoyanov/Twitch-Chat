package irc.android_2016.ifmo.ru.irc.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Dima Stoyanov on 24.10.2016.
 * Project Android-IRC
 * Start time : 22:40
 */

public class TwitchApi {
    private static final String CLIENT_ID = "7v810qqrz2gguvfb0pp3qiuoni06yoc";
    private static final Uri BASE_URI = Uri.parse("https://api.twitch.tv/kraken/chat");

    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения общий эмоций.
     * @throws IOException
     */
    public static HttpURLConnection getTwitchGlobalEmotiсons() throws IOException {
        Uri uri = BASE_URI.buildUpon().appendPath("emotes").appendPath("emoticons").appendQueryParameter("client_id", CLIENT_ID).build();
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
        Uri uri = BASE_URI.buildUpon().appendPath(channel).appendPath("emoticons").appendQueryParameter("client_id", CLIENT_ID).build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }


    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения эмоций канала.
     * @throws IOException
     */
    public static HttpURLConnection getAllTwitchEmoticons() throws IOException {
        Uri uri = BASE_URI.buildUpon().appendPath("emoticons").build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }


}
