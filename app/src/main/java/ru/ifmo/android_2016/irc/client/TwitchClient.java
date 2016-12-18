package ru.ifmo.android_2016.irc.client;

import android.content.Context;
import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.twitch.emotes.TwitchEmotesExtension;
import ru.ifmo.android_2016.irc.api.twitch.emotes.TwitchEmotesLoader;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 10/28/2016.
 */

public final class TwitchClient extends Client {
    private static final String TAG = TwitchClient.class.getSimpleName();
    private TwitchMessage globalUserState;

    private String displayName;
    private Integer nickColor = null;
    private final Set<Integer> emoteSets = new HashSet<>();

    int globalCount = 0;
    long lastTime = 0;

    TwitchClient(Context context) {
        super(context);
    }

    @Override
    protected void preLoopActions() throws IOException {
        //TODO: remove these TEST lines
        sendToChannel(parse("@bits=0;display-name=cheer_test :jtv PRIVMSG Status :cheer1 cheer10 cheer100 cheer1000 cheer5000 cheer10000"));
        //TEST
        capReq("twitch.tv/membership");
        capReq("twitch.tv/commands");
        capReq("twitch.tv/tags");
        super.preLoopActions();
    }

    private void capReq(String s) {
        send("CAP REQ :" + s);
    }

    @Override
    protected IRCMessage getMessageFromString(String s) {
        return TwitchMessage.fromString(s);
    }

    @Override
    protected void doCommand(IRCMessage msg) {
        doCommand((TwitchMessage) msg);
    }

    protected void doCommand(TwitchMessage msg) {
        switch (msg.getCommand()) {
            case "WHISPER":
                sendBroadcast(msg, TextUtils::buildWhisper);
                break;

            case "GLOBALUSERSTATE":
                updateGlobalUserstate(msg);
                break;

            case "USERSTATE":
                updateUserstate(msg);
                break;

            case "CLEARCHAT":
                sendToChannel(msg, TextUtils::buildBanText);
                break;

            case "NOTICE":
                sendToChannel(msg, TextUtils::buildNotice);
                break;

            default:
                super.doCommand(msg);
        }
    }

    @Override
    protected void sendToChannel(IRCMessage msg) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            Channel channel = channels.get(msg.getPrivmsgTarget());
            channel.add(msg, TextUtils::buildMessage);
        }
    }

    @Override
    protected <T extends IRCMessage> void sendToChannel(T msg,
                                                        TextUtils.TextFunction function) {
        if (getOriginalNickname().equals(msg.getPrivmsgTarget())) {
            statusChannel.add(msg, function);
        }
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg, function);
        }
    }

    @Override
    @WorkerThread
    public void sendMessage(String channel, String message, IRCMessage userState) {
        message = message.trim();

        if (message.startsWith("/") && clientCommand(message)) {
            return;
        }

        privmsg(channel, message);
        TwitchMessage msg;
        if (userState != null) {
            msg = (TwitchMessage) userState.clone();
        } else {
            msg = globalUserState.clone();
        }
        msg.setPrivmsg(channel, message);
        msg.applyExtension(new TwitchEmotesExtension(getEmoteSets()));

        if (message.startsWith("/")) {
            serverCommand(msg);
        } else {
            messageQueue.offer(msg);
        }
    }

    @Override
    protected void serverCommand(IRCMessage msg) {
        String message = msg.getPrivmsgText();
        if (message.startsWith("/w ")) {
            String[] parts = message.substring(3).split(" ", 2);
            if (parts.length > 1) {
                msg.setNickname(getNickname());
                msg.setCommand("WHISPER");
                msg.setPrivmsgTarget(parts[0]);
                msg.setPrivmsgText(parts[1]);
                sendBroadcast(msg, TextUtils::buildWhisper);
            }
            return;
        }
        super.serverCommand(msg);
    }

    @Override
    protected void send(String s) {
        //global ban protection LUL
        if (lastTime + 30_000_000_000L > System.nanoTime()) {
            globalCount += 1;
            if (globalCount < 19) {
                super.send(s);
            } else {
                sendBroadcast("You have sent too many messages in a row. Please wait "
                        + (30_000_000_000L + lastTime - System.nanoTime()) + " nanoseconds LUL");
            }
        } else {
            lastTime = System.nanoTime();
            globalCount = 1;
            super.send(s);
        }
    }

    @Override
    public String getNickname() {
        return displayName == null ? getOriginalNickname() : displayName;
    }

    public String getOriginalNickname() {
        return super.getNickname();
    }

    private void updateGlobalUserstate(TwitchMessage userState) {
        //TODO:
        globalUserState = userState;
        nickColor = userState.getColor();
        displayName = userState.getDisplayName();
        emoteSets.addAll(Arrays.asList(userState.getEmoteSets()));
        new TwitchEmotesLoader().execute(userState.getEmoteSets());
    }

    private void updateUserstate(TwitchMessage userState) {
//        updateGlobalUserstate(userState);
        getChannel(userState.getPrivmsgTarget()).setUserState(userState);
    }

    public Set<Integer> getEmoteSets() {
        return emoteSets;
    }
}
