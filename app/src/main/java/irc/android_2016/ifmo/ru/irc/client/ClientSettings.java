package irc.android_2016.ifmo.ru.irc.client;

import android.util.Log;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ghost on 10/23/2016.
 */

public class ClientSettings implements Serializable {
    String address;
    int port = 6667;
    boolean ssl = false;
    String username;
    String password;
    Queue<String> nicks = new LinkedList<>();
    Queue<String> joinList = new LinkedList<>();

    public ClientSettings setAddress(String address) throws UnknownHostException {
        this.address = address;
        return this;
    }

    public ClientSettings setPort(int port) {
        this.port = port;
        return this;
    }

    public ClientSettings setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public ClientSettings setUsername(String username) {
        this.username = username;
        return this;
    }

    public ClientSettings setPassword(String password) {
        this.password = password;
        return this;
    }

    public ClientSettings addNicks(String... nicks) {
        Collections.addAll(this.nicks, nicks);
        return this;
    }

    public ClientSettings addChannels(String... channels) {
        Collections.addAll(this.joinList, channels);
        return this;
    }
}
