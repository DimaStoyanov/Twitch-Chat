package ru.ifmo.android_2016.irc.api.twitch.badges;

import android.os.AsyncTask;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.util.JsonReader;

import com.annimon.stream.function.Consumer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.api.TwitchApi;
import ru.ifmo.android_2016.irc.utils.FunctionUtils;
import ru.ifmo.android_2016.irc.utils.IOUtils;

import static ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadges.NEW_API_CHANNEL_BADGES_URL_TEMPLATE;
import static ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadges.NEW_API_GLOBAL_BADGES_URL;
import static ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadges.badges;
import static ru.ifmo.android_2016.irc.utils.FunctionUtils.throwChecked;
import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;
import static ru.ifmo.android_2016.irc.utils.TextUtils.removePunct;

/**
 * Created by ghost on 12/2/2016.
 */

public class TwitchBadgesLoader extends AsyncTask<Void, Void, Map<String, String>> {
    private final boolean forceGlobalReload;
    @Nullable
    private final String channel;
    @Nullable
    private final Consumer<Map<String, String>> callback;

    public TwitchBadgesLoader() {
        this(false, null, null);
    }

    public TwitchBadgesLoader(@Nullable String channel,
                              @Nullable Consumer<Map<String, String>> callback) {
        this(false, channel, callback);
    }

    public TwitchBadgesLoader(boolean forceGlobalReload,
                              @Nullable String channel,
                              @Nullable Consumer<Map<String, String>> callback) {
        this.forceGlobalReload = forceGlobalReload;
        this.channel = channel;
        this.callback = callback;
    }

    @Override
    protected Map<String, String> doInBackground(Void... rooms) {
        if (badges.isEmpty() || forceGlobalReload) {
            badges.putAll(load(() ->
                    (HttpURLConnection) NEW_API_GLOBAL_BADGES_URL.openConnection()));
        }
        if (channel != null && channel.length() > 1 && callback != null) {
            int roomId = getChannelId(channel);
            if (roomId != -1) {
                callback.accept(load(() ->
                        (HttpURLConnection) new URL(NEW_API_CHANNEL_BADGES_URL_TEMPLATE
                                .replace("{{room-id}}", String.valueOf(roomId)))
                                .openConnection()));
            }
        }
        return null;
    }

    private int getChannelId(String channel) {
        final FunctionUtils.Reference<Integer> ref = new FunctionUtils.Reference<>(-1);
        tryWith(() -> TwitchApi.getChannel(removePunct(channel))).doOp(connection -> {
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    ref.ref = new JSONObject(IOUtils.readToString(connection.getInputStream(), "UTF-8"))
                            .getInt("_id");
                } catch (JSONException e) {
                    throwChecked(e);
                }
            }
        }).catchWith(IOException.class, Exception::printStackTrace).runUnchecked();

        return ref.ref;
    }

    private Map<String, String> load(FunctionUtils.CallableWithException<IOException, HttpURLConnection> fun) {
        final FunctionUtils.Reference<Map<String, String>> map = new FunctionUtils.Reference<>(Collections.emptyMap());

        tryWith(fun).doOp(connection -> {
            connection.connect();

            if (connection.getResponseCode() == 200) {
                map.ref = parseJson(connection.getInputStream());
            } else {
                map.ref = Collections.emptyMap();
            }
        }).catchWith(IOException.class, e -> {
            //
            e.printStackTrace();
        }).runUnchecked();

        return map.ref;
    }

    private static Map<String, String> parseJson(InputStream inputStream)
            throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
        Map<String, String> result = Collections.emptyMap();

        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("badge_sets")) {
                result = newParseBadgeSets(reader);
            } else {
                throw null; //TODO: use kraken api instead
            }
        }
        reader.endObject();

        return result;
    }

    private static Map<String, String> newParseBadgeSets(JsonReader reader)
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
                    map.put(name + "/" + version, newParseBadge(reader));
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

    private static String newParseBadge(JsonReader reader) throws IOException {
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
