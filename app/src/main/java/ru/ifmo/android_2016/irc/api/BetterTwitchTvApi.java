package ru.ifmo.android_2016.irc.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static ru.ifmo.android_2016.irc.utils.TextUtils.removePunct;

/**
 * Created by ghost on 11/11/2016.
 */

public class BetterTwitchTvApi {
    @NonNull
    private static final Uri API_URI = Uri.parse("https://api.betterttv.net/2");

    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения общий
     * эмоций BetterTwitchTV.
     * @throws IOException
     */
    public static HttpURLConnection getBttvGlobalEmoticons() throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("emotes")
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    public static HttpURLConnection getBttvChannelEmoticons(String channelName) throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("channels")
                .appendPath(removePunct(channelName))
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }
}
