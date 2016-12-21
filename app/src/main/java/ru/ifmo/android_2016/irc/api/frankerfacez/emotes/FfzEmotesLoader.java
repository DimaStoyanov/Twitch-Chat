package ru.ifmo.android_2016.irc.api.frankerfacez.emotes;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.FrankerFaceZApi;
import ru.ifmo.android_2016.irc.api.frankerfacez.FrankerFaceZParser;
import ru.ifmo.android_2016.irc.utils.FunctionUtils;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.CallableWithException;
import ru.ifmo.android_2016.irc.utils.FunctionUtils.Reference;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.fuckCheckedExceptions;
import static ru.ifmo.android_2016.irc.utils.FunctionUtils.getInputStreamIfOk;
import static ru.ifmo.android_2016.irc.utils.FunctionUtils.tryWith;

/**
 * Created by ghost on 12/18/2016.
 */

public final class FfzEmotesLoader extends AsyncTask<Void, Void, Void> {
    private final boolean forceGlobalReload;
    private final String channel;
    private final Consumer<Set<Integer>> onLoad;

    public FfzEmotesLoader() {
        this(false, null, null);
    }

    public FfzEmotesLoader(boolean forceGlobalReload) {
        this(forceGlobalReload, null, null);
    }

    public FfzEmotesLoader(@Nullable String channel,
                           @Nullable Consumer<Set<Integer>> onLoad) {
        this(false, channel, onLoad);
    }

    public FfzEmotesLoader(boolean forceGlobalReload,
                           @Nullable String channel,
                           @Nullable Consumer<Set<Integer>> onLoad) {
        this.forceGlobalReload = forceGlobalReload;
        this.channel = channel;
        this.onLoad = onLoad;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (!FfzEmotes.globalLoaded || forceGlobalReload) {
            FrankerFaceZParser.Response response = load(FrankerFaceZApi::getGlobalEmotes);
            FunctionUtils.lol(() -> addEmotes(response));
            FfzEmotes.globalLoaded = true;
            response.getDefaultSets().executeIfPresent(FfzEmotes.defaultSets::addAll);
        }
        if (channel != null && onLoad != null) {
            FrankerFaceZParser.Response response = load(() -> FrankerFaceZApi.getRoomInfo(channel));
            onLoad.accept(Stream.of(response.getSets())
                    .map(FrankerFaceZParser.Set::getId)
                    .collect(Collectors.toSet()));
        }
        return null;
    }

    @NonNull
    private FrankerFaceZParser.Response
    load(CallableWithException<IOException, HttpURLConnection> callable) {
        Reference<FrankerFaceZParser.Response> ref = new Reference<>(new FrankerFaceZParser.Response());

        tryWith(callable).doOp(connection -> {
            getInputStreamIfOk(connection).executeIfPresent(inputStream -> {
                ref.ref = fuckCheckedExceptions(() -> FrankerFaceZParser.parse(inputStream), null);
            });
        }).catchWith(IOException.class, (e) -> {
            e.printStackTrace();
        }).runUnchecked();

        return ref.ref;
    }

    private void addEmotes(FrankerFaceZParser.Response response) throws IOException {
        FfzEmotes.addEmotes(response.getSets());

        response.getRoom().executeIfPresent(r -> {
            String room = r.getName();
            int id = r.getId();
            FfzEmotes.addRoomId(room, id);
        });
    }
}
