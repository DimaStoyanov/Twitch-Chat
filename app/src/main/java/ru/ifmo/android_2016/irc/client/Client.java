package ru.ifmo.android_2016.irc.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import ru.ifmo.android_2016.irc.ChatFragment;
import ru.ifmo.android_2016.irc.utils.Function;

/**
 * Created by ghost on 10/24/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Client implements Runnable {
    private static final String TAG = Client.class.getSimpleName();
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    protected ClientService clientService;
    protected ClientSettings clientSettings;
    protected String nickname;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private Map<String, Channel> channels = new HashMap<>();
    protected Function<Message, CharSequence> defaultPostExecute;

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

    protected void joinChannels(List<String> channels) {
        for (String channel : channels) {
            print("JOIN " + channel);
            this.channels.put(channel, new Channel(channel));
        }
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
        executor.shutdownNow();
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
            if (!clientSettings.isSsl()) {
                socket = new Socket(clientSettings.address, clientSettings.port);
            } else {
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) (socket = sslFactory.createSocket(clientSettings.getAddress(), clientSettings.getPort()));
                sslSocket.startHandshake();
            }

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
        int i = 0;
        while (socket.isConnected()) {
            String s = read();
            Log.i(TAG, s);
            messageQueue.put(parse(s));
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
                sendToChannel(msg);
                break;
        }
    }

    private void sendToChannel(Message msg) {
        if (channels.containsKey(msg.params)) {
            channels.get(msg.params).add(msg);
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
                while (true) {
                    doCommand(messageQueue.take());
                }
        }
    }

    private final class ThreadStarter implements Runnable {
        private final String TAG = ThreadStarter.class.getSimpleName();
        private final String method;

        public ThreadStarter(String method) {
            this.method = method;
        }

        @Override
        public final void run() {
            try {
                Log.i(TAG, method + " at " + Thread.currentThread().getName());
                threadStarter(method);
            } catch (InterruptedException | SocketException x) {
                Log.i(TAG, Thread.currentThread().getName() + " stopped");
            } catch (Exception e) {
                Log.e(TAG, Thread.currentThread().getName());
                e.printStackTrace();
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

    public Map<String, Channel> getChannels() {
        return channels;
    }

    /**
     * Инферфейс между Ui и сетевой частью
     */
    public class Channel {
        private final String channel;
        private final List<CharSequence> messages;
        private Function<Message, CharSequence> postExecute = defaultPostExecute;
        private ChatFragment ui;

        public Channel(String channel) {
            this.channel = channel;
            this.messages = new ArrayList<>(16);
        }

        public void add(Message msg) {
            if (postExecute != null) {
                messages.add(postExecute.apply(msg));
            }
        }

        public String getChannel() {
            return channel;
        }

        public void attachUi(ChatFragment fragment) {
            if (ui == null) {
                ui = fragment;
            } else {
                throw null; //Already attached
            }
        }

        public void detachUi() {
            ui = null;
        }

        public List<CharSequence> getMessages() {
            return messages;
        }
    }
}
