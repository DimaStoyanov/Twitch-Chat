package ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.JsonReader;

import com.annimon.stream.function.Consumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.api.BetterTwitchTvApi;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes.BttvEmotes;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.CallableWithException;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.Reference;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;

/**
 * Created by ghost on 11/16/2016.
 */

public class BttvEmotesLoader extends AsyncTask<Void, Void, Void> {
    //    private static final String TAG = BetterTwitchTvApi.BttvEmotesLoader.class.getSimpleName();
    private final boolean forceGlobalReload;
    @Nullable
    private final String channel;
    @Nullable
    private final Consumer<Map<String, String>> onLoad;

    public BttvEmotesLoader() {
        this(false, null, null);
    }

    public BttvEmotesLoader(boolean forceGlobalReload) {
        this(forceGlobalReload, null, null);
    }

    public BttvEmotesLoader(@Nullable String channel,
                            @Nullable Consumer<Map<String, String>> onLoad) {
        this(false, channel, onLoad);
    }

    public BttvEmotesLoader(boolean forceGlobalReload,
                            @Nullable String channel,
                            @Nullable Consumer<Map<String, String>> onLoad) {
        this.forceGlobalReload = forceGlobalReload;
        this.channel = channel;
        this.onLoad = onLoad;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (BttvEmotes.getGlobalEmotes().isEmpty() || forceGlobalReload) {
            BttvEmotes.setGlobalEmotes(load(BetterTwitchTvApi::getBttvGlobalEmoticons));
        }
        if (channel != null && onLoad != null) {
            Map<String, String> map = load(() -> BetterTwitchTvApi.getBttvChannelEmoticons(channel));
            onLoad.accept(map);
            BttvEmotes.setChannelEmotes(channel, map);
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
    @NonNull
    private Map<String, String> load(CallableWithException<IOException, HttpURLConnection> callable) {
        final Reference<Map<String, String>> result = new Reference<>(Collections.emptyMap());

        tryWith(callable).doOp((httpURLConnection) -> {
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result.ref = readJson(httpURLConnection.getInputStream());
            } else {
                result.ref = Collections.emptyMap();
            }
        }).catchWith(IOException.class, (e) -> {
            e.printStackTrace();
        }).runUnchecked();

        return result.ref;
    }
}
