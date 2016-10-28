package irc.android_2016.ifmo.ru.irc.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ghost on 10/23/2016.
 */

public class ClientSettings implements Serializable {
    final String address, username, password;
    final Queue<String> nicks, joinList;
    final int port;
    final boolean ssl;

    private ClientSettings(String address, String username, String password, Queue<String> nicks,
                           Queue<String> joinList, int port, boolean ssl) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.nicks = nicks;
        this.joinList = joinList;
        this.port = port;
        this.ssl = ssl;
    }

    public static class Builder {
        String address;
        int port = 6667;
        boolean ssl = false;
        String username;
        String password;
        Queue<String> nicks = new LinkedList<>();
        Queue<String> joinList = new LinkedList<>();

        public ClientSettings.Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public ClientSettings.Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public ClientSettings.Builder setSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public ClientSettings.Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public ClientSettings.Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public ClientSettings.Builder addNicks(String... nicks) {
            Collections.addAll(this.nicks, nicks);
            return this;
        }

        public ClientSettings.Builder addChannels(String... channels) {
            Collections.addAll(this.joinList, channels);
            return this;
        }

        public ClientSettings build() {
            return new ClientSettings(address, username, password, nicks, joinList, port, ssl);
        }
    }
}
