package ru.ifmo.android_2016.irc.client;

import java.io.IOException;

import ru.ifmo.android_2016.irc.utils.Function;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 10/28/2016.
 */

public final class TwitchClient extends Client {
    private static final String TAG = TwitchClient.class.getSimpleName();
    private TwitchMessage userState;

    TwitchClient(ClientService clientService) {
        super(clientService);
        defaultPostExecute = new Function<Message, CharSequence>() {
            @Override
            public CharSequence apply(Message param) {
                return TextUtils.buildTextDraweeView((TwitchMessage) param);
            }
        };
    }

    @Override
    protected void actions() throws IOException, InterruptedException {
        enterPassword(clientSettings.password);
        enterNick(clientSettings.nicks);
        capReq("twitch.tv/membership");
        capReq("twitch.tv/commands");
        capReq("twitch.tv/tags");
        joinChannels(clientSettings.channels);
    }

    private void capReq(String s) {
        print("CAP REQ :" + s);
    }

    @Override
    protected Message getMessageFromString(String s) {
        return TwitchMessage.fromString(s);
    }

    @Override
    protected void doCommand(Message msg) {
        switch (msg.command) {
            case "WHISPER":
                sendPrivmsg(msg);
                break;

            case "USERSTATE":
                userState = (TwitchMessage) msg;
                nickname = userState.getDisplayName();
                break;

            default:
                super.doCommand(msg);
        }
    }

    protected void sendPrivmsg(Message msg) {
        ((TwitchMessage) msg)
                .setColor(userState.getColor());
        super.sendPrivmsg(msg);
    }
}
