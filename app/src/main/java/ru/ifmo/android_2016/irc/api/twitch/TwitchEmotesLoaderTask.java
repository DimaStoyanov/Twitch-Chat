package ru.ifmo.android_2016.irc.api.twitch;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.JsonReader;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.TwitchApi;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.Reference;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;

/**
 * Created by ghost on 11/17/2016.
 */

public class TwitchEmotesLoaderTask extends AsyncTask<Integer, Void, Void> {
    @Override
    protected Void doInBackground(Integer... params) {
        Set<Integer> needToLoad = new HashSet<>(Arrays.asList(params));
        needToLoad.removeAll(TwitchEmotes.getLoadedSets());

        if (needToLoad.size() > 0) {
            String sets = Stream.of(needToLoad)
                    .map(i -> Integer.toString(i))
                    .collect(Collectors.joining(","));

            final Reference<Map<Integer, Map<String, String>>> result =
                    new Reference<>(Collections.emptyMap());

            tryWith(() -> TwitchApi.getEmoticonImages(sets)).doOp(httpURLConnection -> {
                httpURLConnection.connect();

                if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    result.ref = parseResult(httpURLConnection.getInputStream());
                }
            }).catchWith(IOException.class, Throwable::printStackTrace).runUnchecked();

            for (Entry<Integer, Map<String, String>> emotes : result.ref.entrySet()) {
                TwitchEmotes.addEmotes(emotes.getKey(), emotes.getValue());
            }
        }
        return null;
    }

    private Map<Integer, Map<String, String>> parseResult(InputStream inputStream)
            throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream));

        @SuppressLint("UseSparseArrays")
        Map<Integer, Map<String, String>> result = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("emoticon_sets")) {
                reader.beginObject();
                while (reader.hasNext()) {
                    readEmoteSet(reader, result);
                }
                reader.endObject();
            } else {
                throw null; //TODO:
            }
        }
        reader.endObject();

        return result;
    }

    private void readEmoteSet(JsonReader reader, Map<Integer, Map<String, String>> result)
            throws IOException {
        int set = Integer.valueOf(reader.nextName());
        Map<String, String> emoteSet = new HashMap<>();

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
                        throw null; //TODO:
                }
            }
            reader.endObject();

            if (id != null && code != null) {
                emoteSet.put(code, id);
            } else {
                throw null; //TODO:
            }
        }
        reader.endArray();

        result.put(set, emoteSet);
    }
}
