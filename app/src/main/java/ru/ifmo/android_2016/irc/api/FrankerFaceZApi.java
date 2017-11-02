package ru.ifmo.android_2016.irc.api;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ghost on 12/18/2016.
 */

public final class FrankerFaceZApi {
    @NonNull
    private static final Uri API_URI = Uri.parse("https://api.frankerfacez.com/v1/");

    private FrankerFaceZApi() {
    }

    public static HttpURLConnection getEmoteSet(String channel) throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("set")
                .appendPath(channel)
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    public static HttpURLConnection getRoomInfo(String channel) throws IOException {
        Uri uri = API_URI.buildUpon()
                .appendPath("room")
                .appendPath(channel.substring(1))
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    public static HttpURLConnection getGlobalEmotes() throws IOException {
        return getEmoteSet("global");
    }
}
