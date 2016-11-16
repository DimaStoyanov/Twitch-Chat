package ru.ifmo.android_2016.irc.api.bettertwitchtv;

import android.os.AsyncTask;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.api.BetterTwitchTvApi;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.CallableWithException;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.Reference;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;

/**
 * Created by ghost on 11/16/2016.
 */

public class BttvEmotesLoaderTask extends AsyncTask<String, Void, Void> {
//    private static final String TAG = BetterTwitchTvApi.BttvEmotesLoaderTask.class.getSimpleName();

    @Override
    protected Void doInBackground(String... params) {
        if (BttvEmotes.getGlobalEmotes() == null) {
            BttvEmotes.setGlobalEmotes(load(BetterTwitchTvApi::getBttvGlobalEmoticons));
        }
        for (String channel : params) {
            Map<String, String> channelEmotes = BttvEmotes.getChannelEmotes(channel);
            if (channelEmotes == null) {
                BttvEmotes.setChannelEmotes(channel,
                        load(() -> BetterTwitchTvApi.getBttvChannelEmoticons(channel)));
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
                    BttvEmotes.setEmoteUrlTemplate(reader.nextString());
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

    private static void readEmotes(Map<String, String> result, JsonReader reader)
            throws IOException {
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

    @SuppressWarnings({"Convert2MethodRef", "CodeBlock2Expr"})
    private Map<String, String> load(CallableWithException<IOException, HttpURLConnection> callable) {
        final Reference<Map<String, String>> result = new Reference<>();

        tryWith(callable).doOp((httpURLConnection) -> {
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result.ref = readJson(httpURLConnection.getInputStream());
            } else {
                result.ref = null;
            }
        }).catchWith(IOException.class, (e) -> {
            e.printStackTrace();
        }).runUnchecked();
        return result.ref;
    }
}
