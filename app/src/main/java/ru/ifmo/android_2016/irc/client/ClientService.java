package ru.ifmo.android_2016.irc.client;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.UiThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.annimon.stream.Stream;

import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.R;
import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotesLoaderTask;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String SERVER_ID = "ru.ifmo.android_2016.irc.id";

    public static final String START_SERVICE = "start-service";
    public static final String STOP_SERVICE = "stop-service";
    public static final String GET_SERVER_LIST = "server-list";
    private String SERVER_LIST_FILE = "/data.obj";    //TODO:
    LocalBroadcastManager lbm;
    ServerList serverList;

    @SuppressLint("UseSparseArrays")
    private Map<Long, Client> clients = new HashMap<>();
    private boolean isRunning = false;
    private static ClientService instance;

    public ClientService() {
        super();
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        lbm = LocalBroadcastManager.getInstance(this);
        SERVER_LIST_FILE = getFilesDir() + SERVER_LIST_FILE;
        new LoadServerListTask().execute(SERVER_LIST_FILE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case START_SERVICE:
                if (!isRunning) {
                    startForeground(1, getNotification("Service is running"));
                    new BttvEmotesLoaderTask().execute();
                    isRunning = true;
                }
                break;

            case STOP_SERVICE:
                stop();
                break;

            case GET_SERVER_LIST:
                if (serverList != null) {
                    lbm.sendBroadcast(new Intent(ServerList.class.getCanonicalName()));
                }
                break;

            default:
        }
        return START_STICKY;
    }

    public static void stopClient(long serverId) {
        instance.new CloseClientTask().execute(serverId);
    }

    public static void startClient(OnConnectedListener activity, long serverId) {
        instance.new StartClientTask().execute(serverId, activity);
    }

    public static void stop() {
        if (instance.clients.isEmpty()) {
            instance.stopSelf();
            instance = null;
        }
    }

    @SuppressWarnings("unused")
    private static void forceStop() {
        instance.new ForceServiceStopTask().execute();
    }

    @SuppressWarnings("deprecation")
    private Notification getNotification(String text) {
        return new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("IRC client")
                .setContentText(text)
                .getNotification();
    }

    void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, getNotification(text));
    }

    public static Client getClient(long id) {
        return instance.clients.get(id);
    }

    public interface OnConnectedListener {
        @UiThread
        void onConnected(Client client);
    }

    private class LoadServerListTask extends AsyncTask<String, Void, ServerList> {
        @Override
        protected ServerList doInBackground(String... strings) {
            return ServerList.loadFromFile(strings[0]);
        }

        @Override
        protected void onPostExecute(ServerList serverList) {
            ClientService.this.serverList = serverList;
            lbm.sendBroadcast(new Intent(ServerList.class.getCanonicalName()));
            super.onPostExecute(serverList);
        }
    }

    private class ForceServiceStopTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Stream.of(clients.values()).forEach(Client::close);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
        }
    }

    private class StartClientTask extends AsyncTask<Object, Void, String> {
        private long id;
        private OnConnectedListener listener;

        @Override
        protected String doInBackground(Object... args) {
            id = (long) args[0];
            listener = (OnConnectedListener) args[1];
            if (!clients.containsKey(id)) {
                ClientSettings clientSettings;
                if ((clientSettings = serverList.find(id)) != null) {
                    Client client;
                    client = clientSettings.isTwitch() ?
                            new TwitchClient(ClientService.this) :
                            new Client(ClientService.this);
                    clients.put(id, client);
                    client.connect(clientSettings);
                }
                return clientSettings + " is running";
            } else {
                Log.i(TAG, "Client " + id + " is already running");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            updateNotification(result);
            listener.onConnected(getClient(id));
        }
    }

    private class CloseClientTask extends AsyncTask<Long, Void, Void> {
        @Override
        protected Void doInBackground(Long... id) {
            Client client = clients.remove(id[0]);
            if (client != null) {
                client.close();
            }
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        new ServerList.SaveToFile().execute(SERVER_LIST_FILE);
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}
