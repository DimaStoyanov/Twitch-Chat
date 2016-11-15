package ru.ifmo.android_2016.irc.client;

import android.support.annotation.WorkerThread;

import com.annimon.stream.function.Function;

import java.io.IOException;

import ru.ifmo.android_2016.irc.utils.Splitter;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 10/28/2016.
 */

public final class TwitchClient extends Client {
    private static final String TAG = TwitchClient.class.getSimpleName();
    private TwitchMessage userState;
    private TwitchMessage globalUserState;

    TwitchClient(ClientService clientService) {
        super(clientService);
    }

    @Override
    protected void actions() throws IOException, InterruptedException {
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
        switch (msg.command) {
            case "WHISPER":
                sendToChannel(msg, (m) -> TextUtils.buildWhisper((TwitchMessage) m));
                break;

            case "GLOBALUSERSTATE":
                globalUserState = (TwitchMessage) msg;
                nickname = globalUserState.getDisplayName();
                break;

            case "USERSTATE":
                userState = (TwitchMessage) msg;
                nickname = userState.getDisplayName();
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
        if (channels.containsKey(msg.params)) {
            channels.get(msg.params).add(msg,
                    (m) -> TextUtils.buildTextDraweeView((TwitchMessage) m));
        }
    }

    @Override
    protected void sendToChannel(Message msg, Function<Message, CharSequence> function) {
        if (nickname.toLowerCase().equals(msg.getParams().toLowerCase())) {
            statusChannel.add(msg, function);
        }
        if (channels.containsKey(msg.params)) {
            channels.get(msg.params).add(msg, function);
        }
    }

    @Override
    @WorkerThread
    public void sendMessage(Message message) {
        TwitchMessage twitchMessage = (TwitchMessage) message;
        send(message.toString());
        twitchMessage
                .setColor(globalUserState.getColor())
                .setEmotes(Emote.parse(null, Splitter.splitWithSpace(twitchMessage.getTrailing()),
                        twitchMessage.getParams()))
                .setNickname(nickname);
        messageQueue.add(message);
    }
}
