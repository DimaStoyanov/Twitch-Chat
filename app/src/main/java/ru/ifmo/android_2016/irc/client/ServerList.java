package ru.ifmo.android_2016.irc.client;

import android.os.AsyncTask;
import android.support.annotation.WorkerThread;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import ru.ifmo.android_2016.irc.utils.FileUtils;
import ru.ifmo.android_2016.irc.utils.Log;

/**
 * Created by ghost3432 on 01.11.16.
 */

public final class ServerList extends HashMap<Long, ClientSettings> {
    private static final String TAG = ServerList.class.getSimpleName();
    private static ServerList instance = null;
    private AtomicLong lastId = new AtomicLong(1);

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

    @WorkerThread
    static ServerList loadFromFile(String filename) {
        if (instance == null) {
            Log.i(TAG, "Loading " + filename);
            instance = FileUtils.readObjectFromFile(filename);
            instance = instance == null ? new ServerList() : instance;
        }
        return instance;
    }

    static class SaveToFile extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            if (instance != null) {
                Log.i(TAG, "Saving to " + strings[0]);
                FileUtils.writeObjectToFile(strings[0], instance);
            }
            return null;
        }
    }

    public long add(ClientSettings clientSettings) {
        if (clientSettings.id == 0) {
            put(lastId.get(), clientSettings.setId(lastId.get()));
            return lastId.getAndIncrement();
        } else {
            put(clientSettings.id, clientSettings);
            return clientSettings.id;
        }
    }

    @Override
    public void clear() {
        lastId.set(1);
        super.clear();
    }
}
