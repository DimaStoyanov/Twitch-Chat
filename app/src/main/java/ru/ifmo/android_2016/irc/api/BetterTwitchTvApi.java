package ru.ifmo.android_2016.irc.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ghost on 11/11/2016.
 */

public class BetterTwitchTvApi {
    @NonNull
    private static final Uri API_URI = Uri.parse("https://api.betterttv.net/2");

    public static HttpURLConnection getBttvGlobalEmoticons() throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("emotes")
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    public static HttpURLConnection getBttvChannelEmoticons(@NonNull String channelName)
            throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("channels")
                .appendPath(channelName.substring(1))
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }
}
