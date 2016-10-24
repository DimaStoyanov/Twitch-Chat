package irc.android_2016.ifmo.ru.irc.client;

/**
 * Created by ghost on 10/23/2016.
 */

public interface ClientServiceCallback {
    void onMessageReceived(final Message msg);
}
