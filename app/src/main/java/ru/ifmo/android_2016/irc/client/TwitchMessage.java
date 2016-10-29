package ru.ifmo.android_2016.irc.client;

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
        //TODO: parse opt-prefix
        return this;
    }
}
