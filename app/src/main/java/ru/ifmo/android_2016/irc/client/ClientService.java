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
    private String SERVER_LIST_FILE = "/data.obj";    //TODO:
    LocalBroadcastManager lbm;
    public ServerList serverList;
    private Map<Long, Client> clients = new HashMap<>();

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
                startForeground(1, new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("IRC client")
                        .setContentText("Service is running")
                        .build());
                break;

            case FORCE_STOP_SERVICE:
                new ForceServiceStopTask().execute();
                break;

            case STOP_SERVICE:
                if (clients.isEmpty()) {
                    stopSelf();
                }
                break;

            case START_TWITCH_CLIENT:
                new StartClientTask().execute(intent.getLongExtra(SERVER_ID, 0));
                break;

            case GET_SERVER_LIST:
                if (serverList != null) {
                    lbm.sendBroadcast(new Intent(ServerList.class.getCanonicalName()));
                }
                break;

            case STOP_CLIENT:
                new CloseClientTask().execute(intent.getLongExtra(SERVER_ID, 0));
                break;

            default:
        }
        return START_STICKY;
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
    }

    private class StartClientTask extends AsyncTask<Long, Void, Void> {
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
