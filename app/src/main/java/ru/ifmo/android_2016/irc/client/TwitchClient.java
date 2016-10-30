package ru.ifmo.android_2016.irc.client;

import android.util.Log;

import java.io.IOException;

/**
 * Created by ghost on 10/28/2016.
 */

public final class TwitchClient extends Client {
    private static final String TAG = TwitchClient.class.getSimpleName();

    TwitchClient(ClientService clientService) {
        super(clientService);
    }

    @Override
    protected void actions() throws IOException, InterruptedException {
        enterPassword(clientSettings.password);
        enterNick(clientSettings.nicks);
        capReq("twitch.tv/membership");
        capReq("twitch.tv/commands");
        capReq("twitch.tv/tags");
        joinChannels(clientSettings.channels);

        loop();
    }

    private void capReq(String s) {
        print("CAP REQ :" + s);
    }
}
