package ru.ifmo.android_2016.irc.api.twitch;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.utils.FunctionUtils.Reference;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;

/**
 * Created by ghost on 11/15/2016.
 */

public class TwitchBadges {
    private static final URL UNDOCUMENTED_GLOBAL_BADGES_URL;
    private static final String UNDOCUMENTED_CHANNEL_BADGES_URL_TEMPLATE =
            "https://badges.twitch.tv/v1/badges/channels/{{room-id}}/display";
    private static final String KRAKEN_BADGES_URL_TEMPLATE =
            "https://api.twitch.tv/kraken/chat/{{channel}}/badges";

    private static final Map<String, String> badges = new HashMap<>();

    static {
        URL url = null;
        try {
            url = new URL("https://badges.twitch.tv/v1/badges/global/display");
        } catch (MalformedURLException e) {
            //impossible
        }
        UNDOCUMENTED_GLOBAL_BADGES_URL = url;
    }

    private TwitchBadges() {
    }

    //TODO:
    public static class BadgesLoaderTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final Reference<Map<String, String>> map = new Reference<>(Collections.emptyMap());

            tryWith(() -> (HttpURLConnection) UNDOCUMENTED_GLOBAL_BADGES_URL.openConnection()).doOp(connection -> {
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    map.ref = parseJson(connection.getInputStream());
                } else {
                    map.ref = Collections.emptyMap();
                }
            }).catchWith(IOException.class, e -> {
                e.printStackTrace();
            }).runUnchecked();

            badges.putAll(map.ref);
            return null;
        }

        private static Map<String, String> parseJson(InputStream inputStream)
                throws IOException {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Map<String, String> result = Collections.emptyMap();

            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("badge_sets")) {
                     result = undocumentedParseBadgeSets(reader);
                } else {
                    throw null; //TODO: use kraken api instead
                }
            }
            reader.endObject();

            return result;
        }

        private static Map<String, String> undocumentedParseBadgeSets(JsonReader reader)
                throws IOException {
            Map<String, String> map = new HashMap<>();

            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();

                reader.beginObject();
                if (reader.nextName().equals("versions")) {

                    reader.beginObject();
                    while (reader.hasNext()) {
                        String version = reader.nextName();
                        map.put(name + "/" + version, undocumentedParseBadge(reader));
                    }
                    reader.endObject();
                } else {
                    throw null; //TODO:
                }
                reader.endObject();
            }
            reader.endObject();

            return map;
        }

        private static String undocumentedParseBadge(JsonReader reader) throws IOException {
            String url = null;

            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "image_url_1x":
                        reader.skipValue();
                        break;

                    case "image_url_2x":
                        url = reader.nextString();
                        break;

                    case "image_url_4x":
                        reader.skipValue();
                        break;

                    case "description":
                        reader.skipValue();
                        break;

                    case "title":
                        reader.skipValue();
                        break;

                    case "click_action":
                        reader.skipValue();
                        break;

                    case "click_url":
                        reader.skipValue();
                        break;

                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
            return url;
        }
    }

    @Nullable
    public static String getBadgeUrl(String badge) {
        if (badges.get(badge) == null) {
            //TODO:
            Log.d("lel", badge);
            return "https://static-cdn.jtvnw.net/badges/v1/de8b26b6-fd28-4e6c-bc89-3d597343800d/2";
        }
        return badges.get(badge);
    }
}
