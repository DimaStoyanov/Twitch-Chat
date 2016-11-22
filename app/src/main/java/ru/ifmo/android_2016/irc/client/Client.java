package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import ru.ifmo.android_2016.irc.utils.FunctionUtils;
import ru.ifmo.android_2016.irc.utils.Log;

/**
 * Created by ghost on 10/24/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Client {
    private static final String TAG = Client.class.getSimpleName();
    protected final static Executor executor = Executors.newCachedThreadPool();

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
    protected Thread responseFetcherThread;
    protected Thread requestListenerThread;
    protected Thread messageHandlerThread;

    protected Function<Exception, Void> defaultExceptionHandler = (e) -> {
        sendStatus(e.toString(), Color.RED);
//        Stream.of(e.getStackTrace()).forEach((ste) -> statusChannel.add(ste.toString(), Color.RED));
        e.printStackTrace();
        quit();
        shutdownThreads();
        reconnect();
        //notifyUi();
        return null;
    };

    private void sendStatus(String msg, int color) {
        statusChannel.add(msg, color);
    }

    protected Function<Exception, Void> interruptedExceptionHandler = e -> {
        if (e instanceof InterruptedException) return null;
        return defaultExceptionHandler.apply(e);
    };
    private boolean connected = false;

    Client(ClientService clientService) {
        this.clientService = clientService;
    }

    void connect(ClientSettings clientSettings) {
        if (!connected) {
            connected = true;
            this.clientSettings = clientSettings;
            connect();
        } else {
            throw new IllegalStateException("Client is already running");
        }
    }

    private void connect() {
        executor.execute(this::run);
        putNewChannel("Status", statusChannel);
        sendStatus("Client started");
    }

    private void reconnect() {
        sendStatus("Reconnecting in 5 seconds");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            //nothing
        }
        executor.execute(this::run);
    }

    private void sendStatus(String message) {
        statusChannel.add(message);
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
        shutdownThreads();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            defaultExceptionHandler.apply(e);
            e.printStackTrace();
        }
    }

    private void shutdownThreads() {
        if (requestListenerThread != null) requestListenerThread.interrupt();
        if (responseFetcherThread != null) responseFetcherThread.interrupt();
        if (messageHandlerThread != null) messageHandlerThread.interrupt();
        requestListenerThread = null;
        responseFetcherThread = null;
        messageHandlerThread = null;
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
                socket = new Socket(clientSettings.getAddress(), clientSettings.getPort());
            } else {
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) (socket = sslFactory.createSocket(
                        clientSettings.getAddress(),
                        clientSettings.getPort()));
                sslSocket.startHandshake();
            }

            sendStatus("Connected");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            executor.execute(FunctionUtils.catchExceptions(
                    this::responseFetcher,
                    interruptedExceptionHandler));

            actions();

            executor.execute(FunctionUtils.catchExceptions(
                    this::messageHandler,
                    interruptedExceptionHandler));

            executor.execute(FunctionUtils.catchExceptions(
                    this::requestListener,
                    interruptedExceptionHandler));
        } catch (IOException x) {
            defaultExceptionHandler.apply(x);
        }
    }

    private void messageHandler() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        messageHandlerThread = thisThread;
        while (!thisThread.isInterrupted()) doCommand(messageQueue.take());
    }

    @WorkerThread
    private void requestListener() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        requestListenerThread = thisThread;
        while (!thisThread.isInterrupted()) {
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

    protected void actions() throws IOException {
        pass(clientSettings.password);
        enterNick(clientSettings.nicks);
        joinChannels(clientSettings.channels);
    }

    protected void responseFetcher() throws IOException, InterruptedException {
        Thread thisThread = Thread.currentThread();
        responseFetcherThread = thisThread;
        while (socket.isConnected() && !thisThread.isInterrupted()) {
            String s = read();
            if (s != null) {
//                Log.d(TAG, s);
                Message parsed = parse(s);
                if (parsed != null) messageQueue.put(parsed);
            }
        }
    }

    @Nullable
    protected String read() throws IOException {
        return in.readLine();
    }

    protected Message parse(String s) {
        try {
            return getMessageFromString(s);
        } catch (ParserException x) {
            statusChannel.add("Can't parse message: " + s);
            defaultExceptionHandler.apply(x);
            return null;
        }
    }

    protected void doCommand(Message msg) {
        switch (msg.getCommand()) {
            case "PING":
                //Log.i(TAG, "PING caught");
                pong();
                break;

            case "PRIVMSG":
                sendToChannel(msg);
                break;

            case "JOIN":
                if (nickname.equals(msg.getNickname()) &&
                        !channels.containsKey(msg.getJoinChannel())) {
                    putNewChannel(msg.getJoinChannel(),
                            new Channel(this, msg.getJoinChannel()));
                    notifyUi();
                }
                break;
        }
    }

    protected void notifyUi() {
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
        nick(nick);
        nickname = nick;
    }

    protected void nick(String nick) {
        send("NICK " + nick);
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
        messageQueue.offer(message);
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
