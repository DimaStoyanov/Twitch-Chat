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
//    private TwitchMessage userState;
//    private TwitchMessage globalUserState;

    private String displayName;
    private int nickColor = 0;
    private final Set<Integer> emoteSets = new HashSet<>();

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
    public void sendMessage(IRCMessage message) {
        TwitchMessage twitchMessage = (TwitchMessage) message;
        send(message.toString());

        if (parseMyMessage(twitchMessage)) messageQueue.offer(twitchMessage);
    }

    private boolean parseMyMessage(TwitchMessage twitchMessage) {
        if (twitchMessage.getPrivmsgText().startsWith("/")) {
            String message = twitchMessage.getPrivmsgText();
            if (message.startsWith("/me ")) {
                twitchMessage.setAction(true);
                twitchMessage.setPrivmsgText(message.substring(4));
            } else if (message.startsWith("/w ")) {

            }
            return false;
        }
        twitchMessage
                .setColor(nickColor)
                .applyExtension(new TwitchEmotesExtension(getEmoteSets()))
                .setNickname(getNickname());

        return true;
    }

    @Override
    protected void send(String s) {
        //TODO: global ban protection
        super.send(s);
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
        nickColor = userState.getColor();
        displayName = userState.getDisplayName();
        emoteSets.addAll(Arrays.asList(userState.getEmoteSets()));
        new TwitchEmotesLoader().execute(userState.getEmoteSets());
    }

    private void updateUserstate(TwitchMessage userState) {
        updateGlobalUserstate(userState);
        getChannel(userState.getPrivmsgTarget()).setUserState(userState);
    }

    public Set<Integer> getEmoteSets() {
        return emoteSets;
    }
}
