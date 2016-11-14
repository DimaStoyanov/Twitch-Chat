package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import com.annimon.stream.function.Function;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.api.BetterTwitchTvApi;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/12/2016.
 */
public class Channel {
    @NonNull
    private final String name;
    @NonNull
    private final List<CharSequence> messages;
    @Nullable
    private final Function<Message, CharSequence> postExecute;
    @Nullable
    private Callback ui;

    Channel(@NonNull String name) {
        this(name, TextUtils::buildDefaultText);
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull String name, @Nullable Function<Message, CharSequence> postExecute) {
        this.name = name;
        this.messages = new ArrayList<>(16);
        this.postExecute = postExecute;
        new BetterTwitchTvApi.BttvEmotesLoaderTask().execute(name);
    }

    void add(Message msg) {
        add(msg, postExecute);
    }

    void add(Message msg, Function<Message, CharSequence> func) {
        if (func != null) messages.add(func.apply(msg));
        notifyUi();
    }

    private void notifyUi() {
        if (ui != null) ui.runOnUiThread(ui::onMessageReceived);
    }

    void add(String msg) {
        messages.add(msg);
        notifyUi();
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void attachUi(Callback fragment) {
        if (ui == null) {
            ui = fragment;
        } else {
            throw null; //TODO: Already attached
        }
    }

    public void detachUi() {
        ui = null;
    }

    @NonNull
    public final List<CharSequence> getMessages() {
        return messages;
    }

    public interface Callback {
        void runOnUiThread(Runnable run);

        @UiThread
        void onMessageReceived();
    }
}