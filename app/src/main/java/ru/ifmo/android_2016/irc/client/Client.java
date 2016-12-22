package ru.ifmo.android_2016.irc.client;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import ru.ifmo.android_2016.irc.IRCApplication;
import ru.ifmo.android_2016.irc.utils.FunctionUtils;
import ru.ifmo.android_2016.irc.utils.Log;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 10/24/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Client {
    private static final String TAG = Client.class.getSimpleName();
    protected final static Executor executor = Executors.newCachedThreadPool();
    private final Context context;

    protected ClientSettings clientSettings;
    @Nullable
    private String nickname;

    protected Socket socket;
    protected BufferedReader in;
    protected PrintWriter out;
    protected final BlockingQueue<IRCMessage> messageQueue = new LinkedBlockingQueue<>();
    protected final BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<>();
    protected Map<String, Channel> channels = new android.support.v4.util.ArrayMap<>();
    @Nullable
    protected Callback ui;
    protected Channel statusChannel = new Channel(this, "Status");
    protected Thread responseFetcherThread;
    protected Thread requestListenerThread;
    protected Thread messageHandlerThread;
    protected Thread pingSenderThread;

    protected Consumer<Exception> defaultExceptionHandler = e -> {
        sendStatus(e.toString(), Color.RED);
        e.printStackTrace();
        quit();
        shutdownThreads();
        executor.execute(this::reconnect);
        //notifyUiOnChannelChange();
    };

    protected Consumer<Exception> interruptedExceptionHandler = e -> {
        if (e instanceof InterruptedException) return;
        defaultExceptionHandler.accept(e);
    };
    private boolean connected = false;

    Client(Context context) {
        this.context = context;
    }

    void connect(ClientSettings clientSettings) {
        if (!connected) {
            connected = true;
            this.clientSettings = clientSettings;

            putNewChannel("Status", statusChannel);
            notifyUiJoined(statusChannel);
            sendStatus("Client started");

            connect();
        } else {
            throw new IllegalStateException("Client is already running");
        }
    }

    private void connect() {
        executor.execute(this::run);
        notifyUiOnChannelChange();
    }

    protected void reconnect() {
        sendBroadcast("Reconnecting in 3 seconds");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //nothing
        }
        connect();
    }

    protected void close() {
        quit();
        shutdownThreads();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            defaultExceptionHandler.accept(e);
            e.printStackTrace();
        }
    }

    private void shutdownThreads() {
        if (requestListenerThread != null) requestListenerThread.interrupt();
        if (responseFetcherThread != null) responseFetcherThread.interrupt();
        if (messageHandlerThread != null) messageHandlerThread.interrupt();
        if (pingSenderThread != null) pingSenderThread.interrupt();
        requestListenerThread = null;
        responseFetcherThread = null;
        messageHandlerThread = null;
        pingSenderThread = null;
    }

    protected final void join(String channel) {
        send("JOIN " + channel);
    }

    protected final void nick(String nick) {
        send("NICK " + nick);
    }

    protected final void pass(String password) {
        send("PASS " + password);
    }

    protected final void part(String channel) {
        send("PART " + channel);
    }

    protected final void part(String channel, String reason) {
        send("PART " + channel + " :" + reason);
    }

    protected final void ping() {
        send("PING");
    }

    protected final void pong() {
        send("PONG");
    }

    protected final void privmsg(String to, String msg) {
        send("PRIVMSG " + to + " :" + msg);
    }

    protected final void quit() {
        send("QUIT");
    }

    protected final void quit(String quitMessage) {
        send("QUIT :" + quitMessage);
    }

    protected final void run() {
        try {
            if (clientSettings.isSsl()) {
                SSLSocketFactory sslFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket sslSocket = (SSLSocket) (socket = sslFactory.createSocket(
                        clientSettings.getAddress(),
                        clientSettings.getPort()));
                sslSocket.startHandshake();
            } else {
                socket = new Socket(clientSettings.getAddress(), clientSettings.getPort());
            }

            sendStatus("Connected");

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            preLoopActions();

            executor.execute(FunctionUtils.catchExceptions(
                    this::responseHandler,
                    interruptedExceptionHandler));

            executor.execute(FunctionUtils.catchExceptions(
                    this::messageHandler,
                    interruptedExceptionHandler));

            executor.execute(FunctionUtils.catchExceptions(
                    this::clientHandler,
                    interruptedExceptionHandler));

            executor.execute(FunctionUtils.catchExceptions(
                    this::pingSender,
                    interruptedExceptionHandler));
        } catch (IOException x) {
            defaultExceptionHandler.accept(x);
        }
    }

    @WorkerThread
    private void messageHandler() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        messageHandlerThread = thisThread;
        while (!thisThread.isInterrupted()) doCommand(messageQueue.take());
    }

    @WorkerThread
    private void clientHandler() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        requestListenerThread = thisThread;
        while (!thisThread.isInterrupted()) requestQueue.take().run();
    }

    @WorkerThread
    protected void responseHandler() throws IOException, InterruptedException {
        Thread thisThread = Thread.currentThread();
        responseFetcherThread = thisThread;
        while (socket.isConnected() && !thisThread.isInterrupted()) {
            String s = read();
            if (s != null) {
//                Log.d(TAG, s);
                IRCMessage parsed = parse(s);
                if (parsed != null) messageQueue.put(parsed);
            }
        }
    }

    @WorkerThread
    protected void pingSender() throws InterruptedException {
        Thread thisThread = Thread.currentThread();
        pingSenderThread = thisThread;
        while (!thisThread.isInterrupted()) {
            Thread.sleep(60000);
            ping();
        }
    }

    @WorkerThread
    protected void preLoopActions() throws IOException {
        pass(clientSettings.password);
        enterNick(clientSettings.nicks);
        joinChannels(clientSettings.channels);
    }

    @Nullable
    protected String read() throws IOException {
        return in.readLine();
    }

    @WorkerThread
    protected void send(String s) {
        if (out != null) {
            Log.i(TAG, s);
            out.println(s);
        }
    }

    protected IRCMessage parse(String rawMessage) {
        try {
            return getMessageFromString(rawMessage);
        } catch (ParserException | IllegalStateException x) {
            Log.d(TAG, rawMessage);
            x.printStackTrace();
            sendStatus("Can't parse message: " + rawMessage);
            return null;
        }
    }

    protected IRCMessage getMessageFromString(String s) {
        return IRCMessage.fromString(s);
    }

    protected void doCommand(IRCMessage msg) {
        switch (msg.getCommand()) {
            case "PING":
                pong();
                break;

            case "PRIVMSG":
                sendToChannel(msg);
                break;

            case "JOIN":
                if (nickname.equals(msg.getNickname())) {
                    Channel channel = new Channel(this, msg.getJoinChannel());
                    putNewChannel(msg.getJoinChannel(), channel);
                    notifyUiJoined(channel);
                }
                break;

            default:
                sendStatus(msg.toString());
        }
    }

    protected void notifyUiOnChannelChange() {
        if (ui != null) IRCApplication.runOnUiThread(ui::onChannelChange);
    }

    protected void notifyUiJoined(final Channel channel) {
        if (ui != null) IRCApplication.runOnUiThread(() -> {
            if (ui != null) ui.onChannelJoined(channel);
        });
    }

    protected void checkResponse(@NonNull String expectedResponse) throws IOException {
        String actualResponse = read();
        if (!expectedResponse.equals(actualResponse)) {
            throw new IOException("CheckResponse failed: expected \"" + expectedResponse +
                    "\", found \"" + actualResponse + "\"");
        }
    }

    //TODO: Пока что один ник, нужно запилить обработку ошибок
    protected void enterNick(String... nicks) {
        nickname = nicks[0];
        nick(nickname);
    }

    protected void joinChannels(List<String> channels) {
        Stream.of(channels).forEach(this::join);
    }

    protected void putNewChannel(String status, Channel channel) {
        if (!channels.containsKey(status)) {
            channels.put(status, channel);
        }
    }

    protected void sendBroadcast(String message) {
        for (Channel channel : channels.values()) {
            channel.add(message);
        }
    }

    protected void sendBroadcast(IRCMessage message,
                                 TextUtils.TextFunction function) {
        for (Channel channel : channels.values()) {
            channel.add(message, function);
        }
    }

    protected void sendStatus(String message) {
        statusChannel.add(message);
    }

    protected void sendStatus(String msg, int color) {
        statusChannel.add(msg, color);
    }

    protected <T extends IRCMessage> void sendToChannel(T msg) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg);
        }
    }

    protected <T extends IRCMessage> void sendToChannel(T msg,
                                                        TextUtils.TextFunction func) {
        if (channels.containsKey(msg.getPrivmsgTarget())) {
            channels.get(msg.getPrivmsgTarget()).add(msg, func);
        }
    }

    @UiThread
    public void attachUi(Callback activity) {
        if (ui == null) {
            ui = activity;
            ui.onChannelChange();
        } else {
            throw new IllegalStateException("This Client is already attached");
        }
    }

    @UiThread
    public void detachUi() {
        this.ui = null;
    }

    @WorkerThread
    public void sendMessage(String channel, String message, IRCMessage userState) {
        privmsg(channel, message);
        IRCMessage ircMessage = new IRCMessage().setPrivmsg(channel, message);
        messageQueue.offer(ircMessage);
    }

    protected boolean clientCommand(String msg) {
        msg = msg.trim();
        if (msg.startsWith("/join ")) {
            join(msg.substring(6).trim());
            return true;
        }
        return false;
    }

    protected void serverCommand(IRCMessage message) {
        String msg = message.getPrivmsgText().trim();
        if (msg.startsWith("/me ")) {
            message.setAction(true);
            message.setPrivmsgText(msg.substring(4));
        }
    }

    public void post(Runnable runnable) {
        requestQueue.offer(runnable);
    }

    public long getId() {
        return clientSettings.getId();
    }

    public interface Callback {
        @UiThread
        void onChannelChange();

        @UiThread
        void onChannelJoined(Channel channel);
    }

    @SuppressWarnings("deprecation")
    @Nullable
    public Channel getChannel(String channel) {
        return getChannels().get(channel);
    }

    public Collection<Channel> getChannelList() {
        return channels.values();
    }

    @Deprecated
    public Map<String, Channel> getChannels() {
        return channels;
    }

    public Context getContext() {
        return context;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    @SuppressWarnings("unused")
    public Channel getStatusChannel() {
        return statusChannel;
    }
}
