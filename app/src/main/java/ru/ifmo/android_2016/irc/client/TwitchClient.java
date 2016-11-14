package ru.ifmo.android_2016.irc.client;

import com.annimon.stream.function.Function;

import java.io.IOException;

import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 10/28/2016.
 */

public final class TwitchClient extends Client {
    private static final String TAG = TwitchClient.class.getSimpleName();
    private TwitchMessage userState;

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
                sendToChannel(msg);
                break;

            case "USERSTATE":
            case "GLOBALUSERSTATE":
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
            channels.get(msg.params).add(msg, (m) -> TextUtils.buildTextDraweeView((TwitchMessage) m));
        }
    }

    protected void sendToChannel(Message msg, Function<Message, CharSequence> function) {
        if (channels.containsKey(msg.params)) {
            channels.get(msg.params).add(msg, function);
        }
    }
}
