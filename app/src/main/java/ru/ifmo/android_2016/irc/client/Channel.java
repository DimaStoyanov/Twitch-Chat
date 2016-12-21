package ru.ifmo.android_2016.irc.client;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.ChatActivity;
import ru.ifmo.android_2016.irc.IRCApplication;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvMessageExtension;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes.BttvEmotesLoader;
import ru.ifmo.android_2016.irc.api.frankerfacez.FrankerFaceZExtension;
import ru.ifmo.android_2016.irc.api.frankerfacez.emotes.FfzEmotesLoader;
import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadgesExtension;
import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadgesLoader;
import ru.ifmo.android_2016.irc.utils.FileUtils;
import ru.ifmo.android_2016.irc.utils.Log;
import ru.ifmo.android_2016.irc.utils.NotificationUtils;
import ru.ifmo.android_2016.irc.utils.TextUtils;
import ru.ifmo.android_2016.irc.utils.TextUtils.TextFunction;

import static ru.ifmo.android_2016.irc.ChatActivity.CHANNEL;
import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;

/**
 * Created by ghost on 11/12/2016.
 */
public final class Channel {
    private static final String TAG = Channel.class.getSimpleName();
    private static final int MAX_MESSAGES = 1000;

    private Client client;
    @NonNull
    private final String name;
    @NonNull
    private final List<MessageText> messages;
    @Nullable
    private final TextFunction textFunction;
    @Nullable
    private Callback ui;
    @Nullable
    private TwitchMessage userState;
    @NonNull
    private Map<String, String> channelBadges = new HashMap<>();
    @NonNull
    private Map<String, String> channelBttvEmotes = new HashMap<>();
    @NonNull
    private Set<Integer> channelFfzEmoteSet = new HashSet<>();
    @Nullable
    private List<String> lastEmotes = null;

    Channel(@NonNull Client client, @NonNull String name) {
        this(client, name, TextUtils::buildDefaultText);
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull Client client,
            @NonNull String name,
            @Nullable TextFunction textFunction) {
        this.client = client;
        this.name = name;
        this.messages = Collections.synchronizedList(new ArrayList<>(16));
        this.textFunction = textFunction;

        loadExtensions(name);

        if (client.getNickname() != null) {
            setNickname(client.getNickname());
        }
    }

    private void loadExtensions(@NonNull String name) {
        new BttvEmotesLoader(name, channelBttvEmotes::putAll).executeOnExecutor(Client.executor);
        new TwitchBadgesLoader(name, channelBadges::putAll).executeOnExecutor(Client.executor);
        new FfzEmotesLoader(name, channelFfzEmoteSet::addAll).executeOnExecutor(Client.executor);
    }

    void add(IRCMessage msg) {
        add(msg, textFunction);
    }

    void add(IRCMessage msg, TextFunction func) {
        if (func != null) {
            MessageText mt = new MessageText.Builder(msg)
                    .setFunction(func)
                    .setNotificationListener(text -> {
                        Notification notification = NotificationUtils
                                .getNotification(getContext(), getName(), String.valueOf(text),
                                        new Intent(getContext(), ChatActivity.class)
                                                .putExtra(SERVER_ID, client.getId())
                                                .putExtra(CHANNEL, getName()));

                        NotificationUtils.sendNotification(getContext(),
                                NotificationUtils.HIGHLIGHT_NOTIFICATION,
                                notification);
                    })
                    .addExtensions(new TwitchBadgesExtension(channelBadges))
                    .addExtensions(new BttvMessageExtension(channelBttvEmotes))
                    .addExtensions(new FrankerFaceZExtension(channelFfzEmoteSet))
                    .setWhisper(msg.getCommand().equals("WHISPER"))
                    .build();

            Pattern banPattern = MessagePatterns.getInstance().getBanWordsPattern();
            Pattern ignorePattern = MessagePatterns.getInstance().getIgnoredUsersPattern();

            boolean hasBannedWords = banPattern != null &&
                    banPattern
                            .matcher(mt.getText())
                            .find();
            boolean hasIgnoredUsers = ignorePattern != null && mt.getSender() != null &&
                    ignorePattern
                            .matcher(mt.getSender())
                            .find();

            if (!hasBannedWords && !hasIgnoredUsers) {
                add(mt);
            }
        }
    }

    void add(CharSequence msg) {
        add(new MessageText(msg));
    }

    public void add(@NonNull String msg, int color) {
        add(TextUtils.buildColoredText(msg, color));
    }

    private void add(MessageText msg) {
        messages.add(msg);
        if (messages.size() > 1000) {
            IRCApplication.runOnUiThread(() -> {
                if (messages.size() > 1000) {
                    messages.subList(0, 99).clear();
                    if (ui != null) ui.onMessagesRemoved(0, 100);
                }
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
        if (ui == null) {
            ui = fragment;
        } else {
            throw new IllegalStateException("This Channel is already attached");
        }
    }

    public void detachUi() {
        ui = null;
    }

    @NonNull
    public final List<MessageText> getMessages() {
        return messages;
    }

    public void send(@NonNull String message) {
        client.post(() -> client.sendMessage(getName(), message, userState));
    }

    public interface Callback {
        @UiThread
        void onMessageReceived();

        @UiThread
        void onMessagesRemoved(int start, int count);
    }

    void setUserState(@NonNull TwitchMessage userState) {
        this.userState = userState;
    }

    void setNickname(@NonNull String nick) {
    }

    @Override
    public int hashCode() {
        //TODO: сделать норм хеш функцию. или нет
        return super.hashCode();
    }

    public Context getContext() {
        return client.getContext();
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
        if (lastEmotes.contains(id)) {
            lastEmotes.remove(id);
        }
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