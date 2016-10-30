package ru.ifmo.android_2016.irc.client;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ru.ifmo.android_2016.irc.R;

public class ClientService extends Service {
    private static final String TAG = ClientService.class.getSimpleName();

    public static final String START_FOREGROUND = "start-foreground";
    public static final String START_TWITCH_CLIENT = "start-client";
    public static final String STOP_CLIENT = "stop-client";
    LocalBroadcastManager lbm;
    Executor executor = Executors.newCachedThreadPool();
    private Client client = null;

    public ClientService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        lbm = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case START_FOREGROUND:
                startForeground(1, new Notification.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Чёт делаем")
                        .setContentText("лол")
                        .build());
                break;
            case START_TWITCH_CLIENT:
                if (client == null) {
                    client = new TwitchClient(this);
                    client.connect(intent
                            .<ClientSettings>getParcelableExtra(
                                    "ru.ifmo.android_2016.irc.ClientSettings"));
                }
                break;
            case STOP_CLIENT:
                closeClient();
                break;
            default:
        }
        return START_STICKY;
    }

    protected void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "ClientService.onDestroy()", Toast.LENGTH_SHORT).show();
        closeClient();
        super.onDestroy();
    }
}
