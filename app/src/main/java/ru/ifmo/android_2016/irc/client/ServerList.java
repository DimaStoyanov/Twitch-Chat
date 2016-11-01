package ru.ifmo.android_2016.irc.client;

import android.util.Log;

import java.util.HashMap;

/**
 * Created by ghost3432 on 01.11.16.
 */

public final class ServerList extends HashMap<Long, ClientSettings> {
    private static final String TAG = ServerList.class.getSimpleName();
    private static ServerList instance = null;
    private long lastId = 1;

    ClientSettings find(long id) {
        if (id != 0) {
            ClientSettings clientSettings = get(id);
            if (clientSettings != null) {
                return clientSettings;
            }
        }
        Log.i(TAG, "Can't find setting with id " + id);
        return null;
    }

    private ServerList() {
    }

    public static ServerList getInstance() {
        return instance;
    }

    static ServerList loadFromFile() {
        return instance = new ServerList();
    }

    public long add(ClientSettings clientSettings) {
        if (clientSettings.id == 0) {
            put(lastId, clientSettings.setId(lastId));
            return lastId++;
        } else {
            put(clientSettings.id, clientSettings);
            return clientSettings.id;
        }
    }
}
