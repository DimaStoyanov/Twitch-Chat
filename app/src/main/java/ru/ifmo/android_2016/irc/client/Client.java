package ru.ifmo.android_2016.irc.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by ghost on 10/24/2016.
 */

public class Client implements Runnable {
    private static final String TAG = Client.class.getSimpleName();
    protected static final Executor executor = Executors.newCachedThreadPool();

    protected ClientService clientService;
    protected ClientSettings clientSettings;
    protected String nickname;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    Client(ClientService clientService) {
        this.clientService = clientService;
    }

    boolean connect(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        executor.execute(this);
        clientService.lbm.registerReceiver(sendMessage, new IntentFilter("send-message"));
        Log.i(TAG, "Client started");
        return true;
    }

    protected void joinChannels(String channel) {
        print("JOIN " + channel);
    }

    protected BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @UiThread
        @Override
        public void onReceive(Context context, Intent intent) {
            executor.execute(new SendMessageTask(intent));
        }
    };

    protected void close() {
        clientService.lbm.unregisterReceiver(sendMessage);
        quit();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void quit() {
        print("QUIT");
    }

    protected void quit(String message) {
        print("QUIT :" + message);
    }

    @Override
    public final void run() {
        try {
            socket = new Socket(clientSettings.address, clientSettings.port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            executor.execute(new ThreadStarter("loop"));

            actions();

            executor.execute(new ThreadStarter("doCommand"));

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Log.i(TAG, "closed");
    }

    protected void actions() throws IOException, InterruptedException {
        enterPassword(clientSettings.password);
        enterNick(clientSettings.nicks);
        joinChannels(clientSettings.channels);
    }

    protected void loop() throws IOException, InterruptedException {
        while (socket.isConnected()) {
            if (in.ready()) {
                String s = read();
                Log.i(TAG, s);
                messageQueue.add(parse(s));
            } else {
                Thread.sleep(100);
            }
        }
    }

    private String read() throws IOException {
        return in.readLine();
    }

    protected Message parse(String s) {
        return getMessageFromString(s);
    }

    protected void doCommand(Message msg) {
        switch (msg.command) {
            case "PING":
                Log.i(TAG, "PING caught");
                pong();
                break;

            case "PRIVMSG":
                sendToActivity(msg);
                break;
        }
    }

    protected void pong() {
        print("PONG");
    }

    protected Message getMessageFromString(String s) {
        return Message.fromString(s);
    }

    protected void enterPassword(String password) {
        print("PASS " + password);
    }

    protected void enterNick(String... nicks) {
        String nick = nicks[0];
        print("NICK " + nick);
        nickname = nick;
    }

    protected final void print(String s) {
        if (out != null) {
            Log.i(TAG, s);
            out.println(s);
        }
    }

    protected void sendToActivity(Message msg) {
        if (msg != null) {
            clientService.lbm.sendBroadcast(new Intent("new-message")
                    .putExtra(Message.class.getCanonicalName(), msg));
        }
    }

    protected void threadStarter(String thread) throws Exception {
        switch (thread) {
            case "loop":
                loop();
                break;
            case "doCommand":
                doCommand();
                break;
        }
    }

    private final class ThreadStarter implements Runnable {
        private final String thread;

        public ThreadStarter(String thread) {
            this.thread = thread;
        }

        @Override
        public final void run() {
            try {
                threadStarter(thread);
            } catch (Exception x) {
                Log.e(TAG, Thread.currentThread().getName());
                Log.e(TAG, x.toString());
            }
        }
    }

    private class SendMessageTask implements Runnable {
        private final
        @Nullable
        Message msg;

        public SendMessageTask(Intent intent) {
            this.msg = intent.getParcelableExtra(Message.class.getCanonicalName());
        }

        @Override
        public void run() {
            if (msg != null) {
                sendPrivmsg(msg);
            }
        }
    }

    protected void sendPrivmsg(Message msg) {
        print(msg.toString());
        sendToActivity(msg.setNickName(nickname));
    }
}
