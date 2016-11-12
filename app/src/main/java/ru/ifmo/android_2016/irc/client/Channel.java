package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.ChatFragment;
import ru.ifmo.android_2016.irc.api.BetterTwitchTvApi;
import ru.ifmo.android_2016.irc.utils.Function;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/12/2016.
 * <p>
 * Инферфейс между Ui и сетевой частью
 */
public class Channel {
    @NonNull private final String channel;
    @NonNull private final List<CharSequence> messages;
    @Nullable private final Function<Message, CharSequence> postExecute;
    @Nullable private Callback ui;

    Channel(@NonNull String channel) {
        this(channel, new TwitchPostExecute());
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull String channel, @Nullable Function<Message, CharSequence> postExecute) {
        this.channel = channel;
        this.messages = new ArrayList<>(16);
        this.postExecute = postExecute;
        new BetterTwitchTvApi.BttvEmotesLoaderTask().execute(channel);
    }

    void add(Message msg) {
        add(msg, postExecute);
    }

    void add(Message msg, Function<Message, CharSequence> func) {
        if (func != null) {
            messages.add(postExecute.apply(msg));
        }
        notifyUi();
    }

    private void notifyUi() {
        if (ui != null) {
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.onMessageReceived();
                }
            });
        }
    }

    void add(String msg) {
        messages.add(msg);
        notifyUi();
    }

    @NonNull
    public String getChannel() {
        return channel;
    }

    public void attachUi(ChatFragment fragment) {
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
    public List<CharSequence> getMessages() {
        return messages;
    }

    public interface Callback {
        void runOnUiThread(Runnable run);

        @UiThread
        void onMessageReceived();
    }
}