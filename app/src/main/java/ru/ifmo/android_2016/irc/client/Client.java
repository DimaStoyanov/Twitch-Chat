package ru.ifmo.android_2016.irc.client;

import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import ru.ifmo.android_2016.irc.utils.FunctionUtils;

/**
 * Created by ghost on 10/24/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Client {
    private static final String TAG = Client.class.getSimpleName();
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    protected ClientService clientService;
    protected ClientSettings clientSettings;
    private String nickname;

    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    protected final BlockingQueue<Request> requestQueue = new LinkedBlockingQueue<>();
    protected List<Channel> channelList = new ArrayList<>();
    protected Map<String, Channel> channels = new android.support.v4.util.ArrayMap<>();
    @Nullable
    protected Callback ui;
    protected Channel statusChannel = new Channel(this, "Status");
    protected Function<Exception, Void> defaultExceptionHandler = (e) -> {
        statusChannel.add(e.toString());
        Stream.of(e.getStackTrace())
                .forEach((ste) -> statusChannel.add(ste.toString()));
        e.printStackTrace();
        //notifyUi();
        return null;
    };

    Client(ClientService clientService) {
        this.clientService = clientService;
    }

    boolean connect(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        executor.execute(this::run);

        putNewChannel("Status", statusChannel);
        statusChannel.add("Client started");
        return true;
    }

    protected void putNewChannel(String status, Channel channel) {
        channelList.add(channel);
        channels.put(status, channel);
    }

    protected void joinChannels(List<String> channels) {
        Stream.of(channels).forEach(this::join);
    }

    protected final void join(String channel) {
        send("JOIN " + channel);
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
        send("QUIT");
    }

    @SuppressWarnings("unused")
    protected void quit(String quitMessage) {
        send("QUIT :" + quitMessage);
    }

    protected final void run() {
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

            executor.execute(FunctionUtils.catchExceptions(
                    this::responseFetcher,
                    defaultExceptionHandler));

            actions();

            executor.execute(FunctionUtils.catchExceptions(() -> {
                //noinspection InfiniteLoopStatement
                while (true) doCommand(messageQueue.take());
            }, defaultExceptionHandler));

            executor.execute(FunctionUtils.catchExceptions(
                    this::requestListener,
                    defaultExceptionHandler));

        } catch (IOException | RuntimeException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    private void requestListener() throws InterruptedException {
        while (true) {
            Request request = requestQueue.take();
            switch (request.type) {
                case SEND:
                    sendMessage((Message) request.msg);
                    break;

                default:
                    statusChannel.add("Unknown request: " + request.type.name());
            }
        }
    }

    protected void actions() throws IOException, InterruptedException {
        pass(clientSettings.password);
        enterNick(clientSettings.nicks);
        joinChannels(clientSettings.channels);
    }

    protected void responseFetcher() throws IOException, InterruptedException {
        while (socket.isConnected()) {
            String s = read();
            if (s != null) {
//                Log.d(TAG, s);
                messageQueue.put(parse(s));
            }
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
                //Log.i(TAG, "PING caught");
                pong();
                break;

            case "PRIVMSG":
                sendToChannel(msg);
                break;

            case "JOIN":
                if (nickname.toLowerCase().equals(msg.getNickname().toLowerCase())) {
                    putNewChannel(msg.getPrivmsgTarget(),
                            new Channel(this, msg.getPrivmsgTarget()));
                    notifyUi();
                }
                break;
        }
    }

    private void notifyUi() {
        if (ui != null) ui.runOnUiThread(ui::onChannelChange);
    }

    protected void sendToChannel(Message msg) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg);
        }
    }

    protected void sendToChannel(Message msg, Function<Message, CharSequence> func) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg, func);
        }
    }

    protected void pong() {
        send("PONG");
    }

    protected Message getMessageFromString(String s) {
        return Message.fromString(s);
    }

    protected void pass(String password) {
        send("PASS " + password);
    }

    protected void enterNick(String... nicks) {
        String nick = nicks[0];
        send("NICK " + nick);
        nickname = nick;
    }

    protected void send(String s) {
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

    @WorkerThread
    public void sendMessage(Message message) {
        send(message.toString());
        sendToChannel(message.setNickname(getNickname()));
    }

    public String getNickname() {
        return nickname;
    }

    @UiThread
    @WorkerThread
    public void request(Request request) {
        requestQueue.offer(request);
    }

    public interface Callback {
        void runOnUiThread(Runnable run);

        @UiThread
        void onChannelChange();
    }

    public List<Channel> getChannelList() {
        return channelList;
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    @SuppressWarnings("unused")
    public Channel getStatusChannel() {
        return statusChannel;
    }

    public static class Request {
        private Type type;
        private Object msg;

        public Request(Type type, Object msg) {
            this.type = type;
            this.msg = msg;
        }

        enum Type {
            SEND,
        }
    }
}
