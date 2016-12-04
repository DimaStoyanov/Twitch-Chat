package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.IRCApplication;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvMessageExtension;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes.BttvEmotesLoader;
import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadgesExtension;
import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadgesLoader;
import ru.ifmo.android_2016.irc.utils.TextUtils;
import ru.ifmo.android_2016.irc.utils.TextUtils.TextFunction;

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
    private final TextFunction textFunction;
    @Nullable
    private Callback ui;
    @Nullable
    private TwitchMessage userState;
    @NonNull
    private Map<String, String> channelBadges = new HashMap<>();
    @NonNull
    private Map<String, String> channelEmotes = new HashMap<>();
    @Nullable
    private Pattern nicknamePattern = Pattern.compile("\0");
    @Nullable
    private final Pattern[] raffleListLul = {
            Pattern.compile("raffle", Pattern.CASE_INSENSITIVE),
            Pattern.compile("waffIe", Pattern.CASE_INSENSITIVE),
            Pattern.compile("waffle", Pattern.CASE_INSENSITIVE),
    };

    Channel(@NonNull Client client, @NonNull String name) {
        this(client, name, TextUtils::buildDefaultText);
    }

    @SuppressWarnings("WeakerAccess")
    Channel(@NonNull Client client,
            @NonNull String name,
            @Nullable TextFunction textFunction) {
        this.client = client;
        this.name = name;
        this.messages = new ArrayList<>(16);
        this.textFunction = textFunction;
        loadExtensions(name);

        if (client.getNickname() != null) {
            setNickname(client.getNickname());
        }
    }

    private void loadExtensions(@NonNull String name) {
        new BttvEmotesLoader(name, channelEmotes::putAll).execute();
        new TwitchBadgesLoader(name, channelBadges::putAll).execute();
    }

    <T extends Message> void add(T msg) {
        add(msg, textFunction);
    }

    <T extends Message> void add(T msg, TextFunction func) {
        if (func != null) {
            add(new MessageText.Builder(msg)
                    .setFunction(func)
                    .addHighlights(nicknamePattern)
                    .addHighlights(raffleListLul)
                    .addExtensions(new TwitchBadgesExtension(channelBadges))
                    .addExtensions(new BttvMessageExtension(channelEmotes))
                    .build());
        }
    }

    void add(CharSequence msg) {
        add(new MessageText(msg));
    }

    public void add(@NonNull String msg, int color) {
        add(TextUtils.buildColoredText(msg, color));
    }

    private void add(MessageText msg) {
        synchronized (messages) {
            messages.add(msg);
        }
        if (messages.size() > 1000) {
            IRCApplication.runOnUiThread(() -> {
                if (messages.size() > 1000) {
                    synchronized (messages) {
                        messages.subList(0, 99).clear();
                    }
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
        final Message msg = new TwitchMessage()
                .setPrivmsg(getName(), message);

        client.post(() -> client.sendMessage(msg));
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
        nicknamePattern = Pattern.compile(nick, Pattern.CASE_INSENSITIVE);
    }

    Map<String, String> getBadges() {
        return channelBadges;
    }

    @Override
    public int hashCode() {
        //TODO: сделать норм хеш функцию. или нет
        return super.hashCode();
    }
}