package ru.ifmo.android_2016.irc.client;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.MainThread;

import com.annimon.stream.Stream;

import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.utils.FunctionUtils;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.utils.NotificationUtils.FOREGROUND_NOTIFICATION;
import static ru.ifmo.android_2016.irc.utils.NotificationUtils.getNotification;
import static ru.ifmo.android_2016.irc.utils.NotificationUtils.updateNotification;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String SERVER_ID = "ru.ifmo.android_2016.irc.id";

    final static Object serverListLock = new Object();
    static ServerList serverList;

    @SuppressLint("UseSparseArrays")
    private static Map<Long, Client> clients = new HashMap<>();
    private static boolean isRunning = false;
    private static ClientService instance;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        super.onCreate();
        instance = this;
        ServerList.load(this, ClientService::onServerListLoad);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        new ForceClientStopTask().execute();
        instance = null;
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            startForeground(FOREGROUND_NOTIFICATION, getNotification(this, ""));
            isRunning = true;
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void stopClient(long serverId) {
        new StopClientTask(serverId).execute();
    }

    public static void startClient(Context context,
                                   long serverId,
                                   FunctionUtils.Procedure<Client> onLoadListener) {
        new StartClientTask(context, serverId, onLoadListener).execute();
    }

    public static Client getClient(long id) {
        return clients.get(id);
    }

    private static class ForceClientStopTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Stream.of(clients.values()).forEach(Client::close);
            return null;
        }
    }

    private static class StartClientTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final long id;
        private final FunctionUtils.Procedure<Client> listener;

        StartClientTask(Context context,
                        long serverId,
                        FunctionUtils.Procedure<Client> onLoadListener) {
            this.context = context;
            id = serverId;
            this.listener = onLoadListener;
        }

        @Override
        protected void onPreExecute() {
            ensureServiceRunning(context);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... args) {
            if (!clients.containsKey(id)) {
                ClientSettings clientSettings;
                if (serverList == null) {
                    ServerList.load(context, ClientService::onServerListLoad);
                    synchronized (serverListLock) {
                        try {
                            serverListLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if ((clientSettings = serverList.find(id)) != null) {
                    Client client;
                    client = clientSettings.isTwitch() ?
                            new TwitchClient() :
                            new Client();
                    clients.put(id, client);
                    client.connect(clientSettings);

                    return clientSettings.getName() + " is running";
                }
            } else {
                Log.i(TAG, "Client " + id + " is already running");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            updateNotification(context, result);
            listener.call(getClient(id));
        }
    }

    private static class StopClientTask extends AsyncTask<Void, Void, Void> {
        private final long id;

        StopClientTask(long id) {
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Client client = clients.remove(id);
            if (client != null) client.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (instance != null && clients.isEmpty()) {
                instance.stopSelf();
            }
        }
    }

    private static void ensureServiceRunning(Context context) {
        context.startService(new Intent(context, ClientService.class));
    }

    @MainThread
    private static void onServerListLoad() {
        synchronized (serverListLock) {
            serverList = ServerList.getInstance();
            serverListLock.notifyAll();
        }
    }
}
