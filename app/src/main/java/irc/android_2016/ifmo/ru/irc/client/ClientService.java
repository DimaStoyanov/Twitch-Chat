package irc.android_2016.ifmo.ru.irc.client;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ClientService extends Service {
    private ClientService.Binder binder = new ClientService.Binder();
    private Queue<Message> messages = new ConcurrentLinkedQueue<>();
    Executor executor = Executors.newCachedThreadPool();

    public class Binder extends android.os.Binder {
        Client client;

        public Client getClient() {
            if (client == null) {
                client = new Client(ClientService.this);
            }
            return client;
        }
    }

    public ClientService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "ClientService.onCreate()", Toast.LENGTH_SHORT).show();
        startForeground(1, new Notification());
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("onBind", "onBind");
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i("onRebind", "onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("onUnbind", "onUnbind");
        return !super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "ClientService.onDestroy()", Toast.LENGTH_SHORT).show();
        stopForeground(true);
    }
}
