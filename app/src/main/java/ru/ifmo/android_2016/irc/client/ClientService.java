package ru.ifmo.android_2016.irc.client;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.MainThread;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ru.ifmo.android_2016.irc.NewChannelListActivity;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.utils.NotificationUtils.FOREGROUND_NOTIFICATION;
import static ru.ifmo.android_2016.irc.utils.NotificationUtils.getNotification;
import static ru.ifmo.android_2016.irc.utils.NotificationUtils.getNotificationBuilder;
import static ru.ifmo.android_2016.irc.utils.NotificationUtils.sendNotification;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String SERVER_ID = "ru.ifmo.android_2016.irc.id";

    final static Object serverListLock = new Object();
    static ServerList serverList;

    @SuppressLint("UseSparseArrays")
    private static Map<Long, Client> clients = new HashMap<>();
    private static boolean isRunning = false;
    private static ClientService instance;
    private static List<Long> ids = new LinkedList<>();


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
            Notification notification = getNotificationBuilder(this)
                    .setContentText("Service started")
                    .build();

            startForeground(FOREGROUND_NOTIFICATION, notification);
            isRunning = true;
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void stopClient(long serverId) {
        ids.remove(serverId);
        new StopClientTask(serverId).execute();
    }

    public static void stopAllClients() {
        while (!ids.isEmpty()) {
            stopClient(ids.get(0));
        }
    }

    public static void startClient(Context context,
                                   long serverId,
                                   Consumer<Client> onLoadListener) {
        ids.add(serverId);
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
        private final Consumer<Client> listener;

        StartClientTask(Context context,
                        long serverId,
                        Consumer<Client> onLoadListener) {
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
                            new TwitchClient(context) :
                            new Client(context);
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
            String notificationText = clients.size() + " client running";
            Notification notification = getNotification(context, "IRC Client", notificationText,
                    new Intent(context, NewChannelListActivity.class));
            notification.vibrate = null;

            sendNotification(context, FOREGROUND_NOTIFICATION, notification);
            listener.accept(getClient(id));
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
