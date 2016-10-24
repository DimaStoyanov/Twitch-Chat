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
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "ClientService.onDestroy()", Toast.LENGTH_SHORT).show();
        stopForeground(true);
    }

    /*private class Client implements Runnable {
        volatile boolean isConnected = true;

        ClientSettings cs;
        Socket socket;
        InputStream in;
        OutputStream out;

        Client(ClientSettings cs) {
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
                    if (!isConnected) {
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
    }*/
}
