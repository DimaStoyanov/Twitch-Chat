package ru.ifmo.android_2016.irc.client;

import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
    private Map<String, Channel> channels = new TreeMap<>();
    @Nullable
    private Callback ui;
    protected Channel statusChannel = new Channel("status");

    Client(ClientService clientService) {
        this.clientService = clientService;
    }

    boolean connect(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        executor.execute(this);
        //channels.put("Status", statusChannel);
        Log.i(TAG, "Client started");
        statusChannel.add("Client started");
        return true;
    }

    protected void joinChannels(List<String> channels) {
        for (String channel : channels) {
            join(channel);
            this.channels.put(channel, new Channel(channel));
        }
        if (ui != null) {
            ui.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ui.onChannelChange();
                }
            });
        }
    }

    protected final void join(String channel) {
        print("JOIN " + channel);
    }

    protected void close() {
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
                SSLSocket sslSocket = (SSLSocket) (socket = sslFactory.createSocket(
                        clientSettings.getAddress(),
                        clientSettings.getPort()));
                sslSocket.startHandshake();
            }

            statusChannel.add("Connected");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        loop();
                    } catch (IOException | InterruptedException | RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            });

            actions();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            doCommand(messageQueue.take());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void actions() throws IOException, InterruptedException {
        pass(clientSettings.password);
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

    @Nullable
    protected String read() throws IOException {
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

    protected void sendToChannel(Message msg) {
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

    protected void pass(String password) {
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

    public void detachUi() {
        this.ui = null;
    }

    public void attachUi(Callback activity) {
        if (ui == null) {
            ui = activity;
        } else {
            throw null; //TODO: Already attached
        }
    }

    public interface Callback {
        void runOnUiThread(Runnable run);

        @UiThread
        void onChannelChange();
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    public Channel getStatusChannel() {
        return statusChannel;
    }
}
