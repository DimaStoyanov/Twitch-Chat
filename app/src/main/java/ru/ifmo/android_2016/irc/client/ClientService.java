package ru.ifmo.android_2016.irc.client;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ru.ifmo.android_2016.irc.R;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String SERVER_ID = "ru.ifmo.android_2016.irc.id";

    public static final String SERVER_LIST = "ru.ifmo.android_2016.irc.serversList";

    public static final String START_SERVICE = "start-service";
    public static final String FORCE_STOP_SERVICE = "force-stop-service";
    public static final String STOP_SERVICE = "stop-service";
    public static final String START_TWITCH_CLIENT = "start-twitch-client";
    public static final String GET_SERVER_LIST = "server-list";
    public static final String STOP_CLIENT = "stop-client";
    LocalBroadcastManager lbm;
    Executor executor = Executors.newCachedThreadPool();
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
        serverList = ServerList.loadFromFile();
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
                for (Client client : clients.values()) {
                    closeClient(client);
                }
                clients.clear();
            case STOP_SERVICE:
                if (clients.isEmpty()) {
                    stopSelf();
                }
                break;
            case START_TWITCH_CLIENT:
                long id = intent.getLongExtra(SERVER_ID, 0);
                if (!clients.containsKey(id)) {
                    ClientSettings clientSettings;
                    if ((clientSettings = serverList.find(id)) != null) {
                        Client client;
                        client = clientSettings.twitch ? new TwitchClient(this) : new Client(this);
                        clients.put(id, client);
                        client.connect(clientSettings);
                    }
                } else {
                    Log.i(TAG, "Client " + id + " is already running");
                }
                break;
            case STOP_CLIENT:
                closeClient(clients.remove(intent.getLongExtra(SERVER_ID, 0)));
                break;
            default:
        }
        return START_STICKY;
    }

    protected void closeClient(Client client) {
        if (client != null) {
            client.close();
        }
    }

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
