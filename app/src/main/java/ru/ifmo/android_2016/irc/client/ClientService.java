package ru.ifmo.android_2016.irc.client;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import ru.ifmo.android_2016.irc.R;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String SERVER_ID = "ru.ifmo.android_2016.irc.id";

    public static final String START_SERVICE = "start-service";
    public static final String FORCE_STOP_SERVICE = "force-stop-service";
    public static final String STOP_SERVICE = "stop-service";
    public static final String START_TWITCH_CLIENT = "start-twitch-client";
    public static final String GET_SERVER_LIST = "server-list";
    public static final String STOP_CLIENT = "stop-client";
    private static final String SERVER_LIST_FILE = null;    //TODO:
    LocalBroadcastManager lbm;
    public ServerList serverList;
    private Map<Long, Client> clients;

    public ClientService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        lbm = LocalBroadcastManager.getInstance(this);
        clients = new HashMap<>();
        loadServerList.execute(SERVER_LIST_FILE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case START_SERVICE:
                startForeground(1, new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Чёт делаем")
                        .setContentText("лол")
                        .build());
                break;
            case FORCE_STOP_SERVICE:
                forceServiceStop.execute();
                break;
            case STOP_SERVICE:
                if (clients.isEmpty()) {
                    stopSelf();
                }
                break;
            case START_TWITCH_CLIENT:
                startClient.execute(intent.getLongExtra(SERVER_ID, 0));
                break;
            case GET_SERVER_LIST:
                loadServerList.execute(SERVER_LIST_FILE);
                break;
            case STOP_CLIENT:
                closeClient.execute(intent.getLongExtra(SERVER_ID, 0));
                break;
            default:
        }
        return START_STICKY;
    }

    private AsyncTask<String, Void, ServerList> loadServerList = new AsyncTask<String, Void, ServerList>() {
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
    };

    private AsyncTask<Void, Void, Void> forceServiceStop = new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... voids) {
            for (Client client : clients.values()) {
                client.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            stopSelf();
        }
    };

    private AsyncTask<Long, Void, Void> startClient = new AsyncTask<Long, Void, Void>() {
        @Override
        protected Void doInBackground(Long... longs) {
            long id = longs[0];
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
            } else {
                Log.i(TAG, "Client " + id + " is already running");
            }
            return null;
        }
    };

    private AsyncTask<Long, Void, Void> closeClient = new AsyncTask<Long, Void, Void>() {
        @Override
        protected Void doInBackground(Long... id) {
            Client client = clients.remove(id[0]);
            if (client != null) {
                client.close();
            }
            return null;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}
