package ru.ifmo.android_2016.irc.api.twitch;

import android.os.AsyncTask;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ghost on 11/15/2016.
 */

public class TwitchBadges {
    private static final String UNDOCUMENTED_GLOBAL_BADGES_URL =
            "https://badges.twitch.tv/v1/badges/global/display";
    private static final String UNDOCUMENTED_CHANNEL_BADGES_URL_TEMPLATE =
            "https://badges.twitch.tv/v1/badges/channels/{{room-id}}/display";
    private static final String KRAKEN_BADGES_URL_TEMPLATE =
            "https://api.twitch.tv/kraken/chat/{{channel}}/badges";

    private static final Map<String, String> map = new HashMap<>();

    private TwitchBadges() {
    }

    //TODO:
    static class BadgesLoaderTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            return null;
        }

        private static void undocumentedParseJson(InputStream inputStream) throws IOException {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));

            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals("badge_sets")) {
                    undocumentedParseBadgeSets(reader);
                } else {
                    throw null; //TODO: use kraken api instead
                }
            }
            reader.endObject();
        }

        private static void undocumentedParseBadgeSets(JsonReader reader) throws IOException {
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
        }

        private static String undocumentedParseBadge(JsonReader reader) throws IOException {
            String url = null;

            reader.beginObject();
            switch (reader.nextName()) {    //TODO:
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
            reader.endObject();
            return url;
        }
    }
}
