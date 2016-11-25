package ru.ifmo.android_2016.irc.client;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import ru.ifmo.android_2016.irc.IRCApplication;
import ru.ifmo.android_2016.irc.utils.BackgroundTask;
import ru.ifmo.android_2016.irc.utils.FileUtils;
import ru.ifmo.android_2016.irc.utils.Log;

/**
 * Created by ghost3432 on 01.11.16.
 */

public final class ServerList extends HashMap<Long, ClientSettings> {
    private static final String TAG = ServerList.class.getSimpleName();

    private final static String SERVER_LIST_FILE = "/data.obj"; //TODO:
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

    @Nullable
    public static ServerList getInstance() {
        return instance;
    }

    public static void load(Context context, final Runnable onLoadListener) {
        final String path = context.getFilesDir() + SERVER_LIST_FILE;
        new BackgroundTask(() -> {
            loadFromFile(path);
            IRCApplication.runOnUiThread(onLoadListener);
        }).executeOnExecutor(Client.executor);
        //TODO: костыль чтоб таск заработал пока другой таск работает
    }

    @WorkerThread
    private static ServerList loadFromFile(String filename) {
        if (instance == null) {
            Log.i(TAG, "Loading " + filename);
            instance = FileUtils.readObjectFromFile(filename);
            instance = instance == null ? new ServerList() : instance;
        }
        return instance;
    }

    public static void save(String filesDir) {  //TODO: разобраться с filesDir
        new SaveToFile(filesDir + SERVER_LIST_FILE).execute();
    }

    private static class SaveToFile extends AsyncTask<Void, Void, Void> {
        private String serverListFile;

        SaveToFile(String serverListFile) {
            this.serverListFile = serverListFile;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (instance != null) {
                Log.i(TAG, "Saving to " + serverListFile);
                FileUtils.writeObjectToFile(serverListFile, instance);
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
