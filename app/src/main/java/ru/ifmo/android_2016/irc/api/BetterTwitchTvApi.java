package ru.ifmo.android_2016.irc.api;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;
import android.util.Log;

import com.annimon.stream.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ghost on 11/11/2016.
 */

public class BetterTwitchTvApi {
    @NonNull
    private static final Uri API_URI = Uri.parse("https://api.betterttv.net/2");
    @Nullable
    private static String EMOTICON_URL_TEMPLATE = null;
    @Nullable
    public static Map<String, String> globalEmotes = null;    //code -> id

    //channel -> (code -> id)
    @NonNull
    private final static Map<String, Map<String, String>> channelEmotes = new HashMap<>();

    /**
     * Возвращает URL ссылку на изобренрие emoticon с заданным id заданного размера.
     *
     * @param id   Номер картинки
     * @param size Размер картинки. Один из {1.0, 2.0, 3.0}
     * @return URL ссылка на картинку
     */
    public static String getEmoteUrl(String id, String size) {
        if (EMOTICON_URL_TEMPLATE != null) {
            return "https:" + EMOTICON_URL_TEMPLATE
                    .replace("{{id}}", id).replace("{{image}}", size);
        }
        throw null; //TODO: Я не знаю, нужно ли здесь кидать эксепшн или просто вернуть нулл
    }

    public static String getEmoteUrl(String id) {
        return getEmoteUrl(id, "2x");
    }

    /**
     * @return Возвращает {@link HttpURLConnection} для выполнения запроса для получения общий
     * эмоций.
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
                .appendPath(channelName.replace("#", ""))
                .build();
        return (HttpURLConnection) new URL(uri.toString()).openConnection();
    }

    @Nullable
    public static Map<String, String> getChannelEmotes(String channel) {
        return channelEmotes.get(channel);
    }

    public static class BttvEmotesLoaderTask extends AsyncTask<String, Void, Void> {
        private static final String TAG = BttvEmotesLoaderTask.class.getSimpleName();

        @Override
        protected Void doInBackground(String... params) {
            if (globalEmotes == null) {
                HttpURLConnection httpURLConnection = null;
                try {
                    httpURLConnection = getBttvGlobalEmoticons();
                    httpURLConnection.connect();
                    if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        globalEmotes = readJson(httpURLConnection.getInputStream());

                        for (Map.Entry<String, String> entry : globalEmotes.entrySet()) {
                            Log.d("bttv", entry.getKey() + "/" + entry.getValue());
                        }
                    } else {
                        globalEmotes = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
            for (String channel : params) {
                if (!channelEmotes.containsKey(channel)) {
                    Log.d(TAG, "gachiGASM");
                    Map<String, String> emotes = null;
                    HttpURLConnection httpURLConnection = null;
                    try {
                        httpURLConnection = getBttvChannelEmoticons(channel);
                        httpURLConnection.connect();
                        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            emotes = readJson(httpURLConnection.getInputStream());

                            Stream.of(emotes.entrySet()).forEach((entry) ->
                                    Log.d("bttv", entry.getKey() + "/" + entry.getValue()));
                        } else {
                            Log.d(TAG, "bad happened " + httpURLConnection.getResponseCode() + " " + channel);
                            emotes = null;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                    }
                    if (emotes != null) {
                        channelEmotes.put(channel, emotes);
                    }
                }
            }
            return null;
        }

        private static Map<String, String> readJson(InputStream inputStream) throws IOException {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
            Map<String, String> result = new HashMap<>();

            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "status":
                        reader.skipValue();
                        break;

                    case "urlTemplate":
                        EMOTICON_URL_TEMPLATE = reader.nextString();
                        break;

                    case "emotes":
                        readEmotes(result, reader);
                        break;

                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();

            return result;
        }

        private static void readEmotes(Map<String, String> result, JsonReader reader) throws IOException {
            reader.beginArray();
            while (reader.hasNext()) {
                String id = null, code = null;
                reader.beginObject();
                while (reader.hasNext()) {
                    switch (reader.nextName()) {
                        case "id":
                            id = reader.nextString();
                            break;

                        case "code":
                            code = reader.nextString();
                            break;

                        default:
                            reader.skipValue();
                    }
                }
                reader.endObject();
                result.put(code, id);
            }
            reader.endArray();
        }
    }
}
