package ru.ifmo.android_2016.irc.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/24/2016.
 */

public class Client implements Runnable {
    private static final String TAG = Client.class.getSimpleName();

    protected ClientService clientService;
    protected ClientSettings clientSettings;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    Client(ClientService clientService) {
        this.clientService = clientService;
    }

    public boolean connect(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
        clientService.executor.execute(this);
        clientService.lbm.registerReceiver(sendMessage, new IntentFilter("send-message"));
        Log.i(TAG, "Client started");
        return true;
    }

    protected void joinChannels(String channel) {
        print("JOIN " + channel);
    }

    protected BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = (Message) intent.getSerializableExtra("ru.ifmo.android_2016.irc.Message");
            print("PRIVMSG " + message.to + " :" + message.text);
            callbackMessage(message);
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

            actions();

        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            close();
        }
        Log.i(TAG, "closed");
    }

    protected void actions() throws IOException, InterruptedException {
        enterPassword(clientSettings.password);
        enterNick(clientSettings.nicks.element());
        joinChannels(clientSettings.channels);

        loop();
    }

    protected void loop() throws IOException, InterruptedException {
        while (socket.isConnected()) {
            if (in.ready()) {
                String s = in.readLine();
                Log.i(TAG, s);
                callbackMessage(parse(s));
            } else {
                Thread.sleep(100);
            }
        }
    }

    protected Message parse(String s) {
        Matcher message = Message.pattern.matcher(s);
        Matcher ping = Pattern.compile("PING ?:?(.*)").matcher(s);
        if (message.find()) {
            return Message.fromString(message.group());
            //return new Message(message.group());
        }
        if (ping.find()) {
            Log.i(TAG, "PING caught");
            print("PONG :" + ping.group(1));
        }
        return null;
    }

    protected void enterPassword(String password) {
        print("PASS " + password);
    }

    protected void enterNick(String nick) {
        print("NICK " + nick);
    }

    protected final void print(String s) {
        out.println(s);
    }

    protected void callbackMessage(Message msg) {
        if (msg != null) {
            clientService.lbm.sendBroadcast(new Intent("new-message")
                    .putExtra("ru.ifmo.android_2016.irc.Message", msg));
        }
    }
}
