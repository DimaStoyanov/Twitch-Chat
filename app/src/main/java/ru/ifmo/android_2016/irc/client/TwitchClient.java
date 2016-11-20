package ru.ifmo.android_2016.irc.client;

import android.support.annotation.WorkerThread;

import com.annimon.stream.function.Function;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.ifmo.android_2016.irc.api.twitch.TwitchEmotesLoaderTask;
import ru.ifmo.android_2016.irc.utils.Splitter;
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


    TwitchClient(ClientService clientService) {
        super(clientService);
    }

    @Override
    protected void actions() throws IOException {
        capReq("twitch.tv/membership");
        capReq("twitch.tv/commands");
        capReq("twitch.tv/tags");
        pass(clientSettings.password);
        enterNick(clientSettings.nicks);
        joinChannels(clientSettings.channels);
    }

    private void capReq(String s) {
        send("CAP REQ :" + s);
    }

    @Override
    protected Message getMessageFromString(String s) {
        return TwitchMessage.fromString(s);
    }

    @Override
    protected void doCommand(Message msg) {
        switch (msg.getCommand()) {
            case "WHISPER":
                sendToChannel(msg, (m) -> TextUtils.buildWhisper((TwitchMessage) m));
                break;

            case "GLOBALUSERSTATE":
                updateGlobalUserstate((TwitchMessage) msg);
                break;

            case "USERSTATE":
                updateUserstate((TwitchMessage) msg);
                break;

            case "CLEARCHAT":
                sendToChannel(msg, (m) -> TextUtils.buildBanText((TwitchMessage) m));
                break;

            case "NOTICE":
                sendToChannel(msg, (m) -> TextUtils.buildNotice((TwitchMessage) m));
                break;

            default:
                super.doCommand(msg);
        }
    }

    @Override
    protected void sendToChannel(Message msg) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg,
                    (m) -> TextUtils.buildTextDraweeView((TwitchMessage) m));
        }
    }

    @Override
    protected void sendToChannel(Message msg, Function<Message, CharSequence> function) {
        if (getOriginalNickname().equals(msg.getPrivmsgTarget())) {
            statusChannel.add(msg, function);
        }
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg, function);
        }
    }

    @Override
    @WorkerThread
    public void sendMessage(Message message) {
        TwitchMessage twitchMessage = (TwitchMessage) message;
        send(message.toString());

        if (parseMyMessage(twitchMessage)) messageQueue.offer(twitchMessage);
    }

    private boolean parseMyMessage(TwitchMessage twitchMessage) {
        if (twitchMessage.getPrivmsgText().startsWith("/")) {
            String message = twitchMessage.getPrivmsgText();
            if (message.startsWith("/me ")) {
                twitchMessage.setAction(true);
            }
            twitchMessage.setPrivmsgText(message.replaceAll("^/\\w+ ", ""));
        }
        twitchMessage
                .setColor(nickColor)
                .setEmotes(Emote.findAllEmotes(
                        twitchMessage.getPrivmsgText(),
                        twitchMessage.getPrivmsgTarget(),
                        emoteSets))
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
        return displayName;
    }

    public String getOriginalNickname() {
        return super.getNickname();
    }

    private void updateGlobalUserstate(TwitchMessage userState) {
//        globalUserState = userState;
        updateUserstate(userState); //TODO:
    }

    private void updateUserstate(TwitchMessage userState) {
        //TODO:
        nickColor = userState.getColor();
        displayName = userState.getDisplayName();
        emoteSets.addAll(Arrays.asList(userState.getEmoteSets()));
        new TwitchEmotesLoaderTask().execute(userState.getEmoteSets());
    }
}
