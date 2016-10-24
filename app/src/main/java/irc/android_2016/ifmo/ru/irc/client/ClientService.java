package irc.android_2016.ifmo.ru.irc.client;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import irc.android_2016.ifmo.ru.irc.TestActivity;

public class ClientService extends Service {
    private IBinder binder = new ClientService.Binder();
    private ClientSettings settings;
    private Queue<Message> messages = new ConcurrentLinkedQueue<>();
    private Executor executor = Executors.newCachedThreadPool();
    private ClientServiceCallback activity;

    public class Binder extends android.os.Binder {
        public ClientService getService() {
            return ClientService.this;
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
        start(settings = (ClientSettings) intent.getExtras().getSerializable("ClientSettings"));
        return super.onStartCommand(intent, flags, startId);
    }

    public void start(ClientSettings cs) {
        settings = cs;
        executor.execute(new Client(cs));
    }

    @Override
    public IBinder onBind(Intent intent) {
        start((ClientSettings) intent.getExtras().getSerializable("ClientSettings"));
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "ClientService.onDestroy()", Toast.LENGTH_SHORT).show();
        stopForeground(true);
    }

    public void changeActivity(ClientServiceCallback ac) {
        activity = ac;
    }

    private boolean callbackMessage(Message msg) {
        if (activity != null) {
            activity.onMessageReceived(msg);
            return true;
        }
        return false;
    }

    private class Client implements Runnable {
        ClientSettings cs;
        Socket socket;
        InputStream in;
        OutputStream out;

        public Client(ClientSettings cs) {
            this.cs = cs;
        }

        @Override
        public void run() {
            try {
                socket = new Socket(cs.address, cs.port);
                in = socket.getInputStream();
                out = socket.getOutputStream();

                out.write(("PASS " + cs.password + "\n").getBytes());
                for (String nick : cs.nicks) {
                    out.write(("NICK " + nick + "\n").getBytes());
                }
                for (String channel : cs.joinList) {
                    out.write(("JOIN " + channel + "\n").getBytes());
                }

                final byte[] buffer = new byte[8192];

                while (socket.isConnected()) {
                    if (in.available() > 0) {
                        String s = new String(buffer, 0, in.read(buffer));
                        Log.i("chat", s);
                        Matcher m = Message.pattern.matcher(s);
                        while (m.find()) {
                            callbackMessage(new Message(m.group()));
                        }
                        if (Pattern.compile("PING :(.*)").matcher(s).find()) {
                            Log.i("client", "pong");
                            out.write("PONG :\n".getBytes());
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
