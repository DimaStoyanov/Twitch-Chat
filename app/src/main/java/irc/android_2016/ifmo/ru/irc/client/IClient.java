package irc.android_2016.ifmo.ru.irc.client;

/**
 * Created by ghost on 10/24/2016.
 */

public interface IClient {
    Exception getLastError();
    boolean attachActivity(ClientServiceCallback activity);
    boolean joinChannel(String message);
    boolean sendMessage(String message);
}
