package irc.android_2016.ifmo.ru.irc.client;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientService extends Service {
    private IBinder binder = new ClientService.Binder();
    private ClientSettings settings;
    private Queue<Message> messages = new ConcurrentLinkedQueue<>();
    private Executor executor = Executors.newCachedThreadPool();
    private ClientServiceCallback activity;
    private Client client = null;

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
        //start(settings = (ClientSettings) intent.getExtras().getSerializable("ClientSettings"));
        return super.onStartCommand(intent, flags, startId);
    }

    public void start(ClientSettings cs) {
        client = new Client(settings = cs);
        executor.execute(client);
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
        if (client != null) {
            client.isRunning = false;
        }
        Toast.makeText(this, "ClientService.onDestroy()", Toast.LENGTH_SHORT).show();
        stopForeground(true);
    }

    public boolean sendMessage(Message msg) {
        if (client != null && client.isRunning) {
            try {
                String message = "PRIVMSG " + msg.to + " :" + msg.message + "\n";
                Log.i("sendMessage", message);
                client.out.write(message.getBytes());

                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public void changeActivity(ClientServiceCallback ac) {
        activity = ac;
    }

    public void disconnect() {
        if (client != null && client.isRunning) {
            client.isRunning = false;
        }
    }

    private boolean callbackMessage(Message msg) {
        if (activity != null) {
            activity.onMessageReceived(msg);
            return true;
        }
        return false;
    }

    private class Client implements Runnable {
        volatile boolean isRunning = true;

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
                            Message msg = new Message(m.group());
                            messages.add(msg);
                            callbackMessage(msg);
                        }
                        if (Pattern.compile("PING :(.*)").matcher(s).find()) {
                            Log.i("client", "pong");
                            out.write("PONG :\n".getBytes());
                        }
                    } else {
                        Thread.sleep(100);
                    }
                    if (!isRunning) {
                        Log.i("@client", "Quitting");
                        out.write("QUIT\n".getBytes());
                        callbackMessage(new Message("@client", "", "Disconnected"));
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                client = null;
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
