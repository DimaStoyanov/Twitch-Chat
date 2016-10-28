package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost on 10/24/2016.
 */

public interface IClient {
    boolean connect(ClientSettings clientSettings);
    boolean isConnected();
    Exception getLastError();
    boolean joinChannel(String channel);
    boolean sendMessage(Message message);
    void close();
}
