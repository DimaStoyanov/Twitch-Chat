package ru.ifmo.android_2016.irc.client;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.annimon.stream.function.Function;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.IRCApplication;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotesLoaderTask;
import ru.ifmo.android_2016.irc.utils.FileUtils;
import ru.ifmo.android_2016.irc.utils.Log;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/12/2016.
 */
public final class Channel {
    private static final String TAG = Channel.class.getSimpleName();

    private Client client;
    @NonNull
    private final String name;
    @NonNull
    private final List<MessageText> messages;
    @Nullable
    private final Function<Message, CharSequence> postExecute;
    @Nullable
    private Callback ui;
    @Nullable
    private List<String> lastEmotes = null;

    Channel(@NonNull Client client, @NonNull String name) {
        this(client, name, TextUtils::buildDefaultText);
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull Client client,
            @NonNull String name,
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
        if (func != null) {
            add(new MessageText.Builder()
                    .setFunction(func)
                    .setMessage((TwitchMessage) msg)
                    .setMentionList(client.getNickname())
                    .build());
        }
    }

    void add(CharSequence msg) {
        add(new MessageText(msg));
    }

    void add(MessageText msg) {
        synchronized (messages) {
            messages.add(msg);
        }
        if (messages.size() > 1000) {
            IRCApplication.runOnUiThread(() -> {
                synchronized (messages) {
                    messages.subList(0, 99).clear();
                }
                if (ui != null) ui.onMessagesRemoved(0, 100);
            });
        }
        notifyUi();
    }

    private void notifyUi() {
        if (ui != null) IRCApplication.runOnUiThread(ui::onMessageReceived);
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void attachUi(Callback fragment) {
        Log.d(TAG, "onAttach");
        if (ui == null) {
            ui = fragment;
        } else {
            throw null; //TODO: Already attached
        }
    }

    public void detachUi() {

        Log.d(TAG, "onDetach");
        ui = null;
    }

    @NonNull
    public final List<MessageText> getMessages() {
        return messages;
    }

    @UiThread
    public void send(String message) {
        Message msg = new TwitchMessage()
                .setPrivmsg(getName(), message);

        Log.d(TAG, "requesting " + message + "/" + msg.toString());
        client.request(new Client.Request(Client.Request.Type.SEND, msg));
    }

    public void add(String msg, int color) {
        add(TextUtils.buildColoredText(msg, color));
    }

    public interface Callback {
        @UiThread
        void onMessageReceived();

        @UiThread
        void onMessagesRemoved(int start, int count);
    }


    /**
     * Methods to store last emotes order
     */

    // WARNING! This method shouldn't call in UI Thread, write loader in your own class
    @WorkerThread
    public void addLastEmote(String id, Context context) {
        if (lastEmotes == null) {
            lastEmotes = getLastEmotes(context);
            lastEmotes = lastEmotes == null ? new ArrayList<>() : lastEmotes;
        }
        if (lastEmotes.contains(id))
            lastEmotes.remove(id);
        lastEmotes.add(0, id);
    }

    // WARNING! This method shouldn't call in UI Thread, write loader in your own class
    @WorkerThread
    @Nullable
    public List<String> getLastEmotes(Context context) {
        if (lastEmotes == null) {
            File file = new File(context.getFilesDir(), name + ".obj");
            if (!file.isFile())
                return new ArrayList<>();
            return lastEmotes = FileUtils.readObjectFromFile(file.getAbsolutePath());
        }
        return lastEmotes;
    }


    // WARNING! This method shouldn't call in UI Thread, write loader in your own class
    @WorkerThread
    public void writeEmotesToStorage(Context context) {
        try {
            File file = new File(context.getFilesDir(), name + ".obj");
            if (file.isFile() && !file.delete()) {
                Log.e(TAG, "Can't rewrite file");
                return;
            }
            if (!file.createNewFile()) {
                Log.e(TAG, "Can't create new file");
                return;
            }
            FileUtils.writeObjectToFile(file.getAbsolutePath(), lastEmotes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}