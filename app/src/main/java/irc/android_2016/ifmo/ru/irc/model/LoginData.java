package irc.android_2016.ifmo.ru.irc.model;

import android.support.annotation.NonNull;

import java.util.StringTokenizer;

import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

/**
 * Created by Dima Stoyanov on 23.10.2016.
 * Project Android-IRC
 * Start time : 23:10
 */

public class LoginData {

    public int id;

    public final
    @NonNull
    String server;

    public final
    @NonNull
    String nick;

    public final
    @NonNull
    String password;

    public final
    @NonNull
    String channel;

    public LoginData(String... data) {
        server = data[0];
        nick = data[1];
        password = data[2];
        channel = data[3];
    }

    public LoginData(StringTokenizer tokens) {
        id = Integer.parseInt(tokens.nextToken());
        server = tokens.nextToken();
        nick = tokens.nextToken();
        password = tokens.nextToken();
        channel = tokens.nextToken();
    }

    @Override
    public String toString() {
        return FileUtils.getDataString(String.valueOf(id), server, nick, password, channel);
    }
}
