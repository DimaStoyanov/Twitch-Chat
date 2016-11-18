package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import com.annimon.stream.function.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotesLoaderTask;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/12/2016.
 */
public class Channel {
    private static final String TAG = Channel.class.getSimpleName();
    private Client client;
    @NonNull
    private final String name;
    @NonNull
    private final List<CharSequence> messages;
    @Nullable
    private final Function<Message, CharSequence> postExecute;
    @Nullable
    private Callback ui;

    Channel(@NonNull Client client, @NonNull String name) {
        this(client, name, TextUtils::buildDefaultText);
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull Client client, @NonNull String name,
            @Nullable Function<Message, CharSequence> postExecute) {
        this.client = client;
        this.name = name;
        this.messages = new ArrayList<>(16);
        this.postExecute = postExecute;
        new BttvEmotesLoaderTask().execute(name);
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

    @UiThread
    public void send(String message) {
        Message msg = new TwitchMessage()
                .setCommand("PRIVMSG")
                .setParams(Collections.singletonList(getName()))
                .setTrailing(message);

        Log.d(TAG, "requesting " + message + "/" + msg.toString());
        client.request(new Client.Request(Client.Request.Type.SEND, msg));
    }

    public interface Callback {
        void runOnUiThread(Runnable run);

        @UiThread
        void onMessageReceived();
    }
}