package ru.ifmo.android_2016.irc.client;

import java.util.HashMap;

/**
 * Created by ghost on 10/29/2016.
 */

public final class TwitchMessage extends Message {

    private TwitchMessage() {
    }

    public static TwitchMessage fromString(String rawMessage) {
        return new TwitchMessage().parse(rawMessage);
    }

    @Override
    protected TwitchMessage parse(String rawMessage) {
        super.parse(rawMessage);
        HashMap<String, String> map = new HashMap<>();
        if (opt_prefix != null) {
            String[] params = opt_prefix.split(";");
            for (String param : params) {
                String[] p = param.split("=");
                String key = null, value = null;
                if (p.length > 0) {
                    key = p[0];
                }
                if (p.length > 1) {
                    value = p[1];
                }
                map.put(key, value);
            }
        }
        return this;
    }
}
