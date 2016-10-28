package irc.android_2016.ifmo.ru.irc.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/24/2016.
 */

public class Client implements Runnable {
    private static final String TAG = Client.class.getSimpleName();

    private boolean isRunning = true;

    private ClientService clientService;
    private ClientSettings clientSettings;

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

    public Exception getLastError() {
        return null;
    }

    public boolean joinChannel(String channel) {
        if (isRunning) {
            print("JOIN " + channel);
        }
        return false;
    }

    private BroadcastReceiver sendMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = (Message) intent.getSerializableExtra("irc.Message");
            if (isRunning) {
                print("PRIVMSG " + message.to + " :" + message.text);
                callbackMessage(message);
            }
        }
    };

    public void close() {
        isRunning = false;
        clientService.lbm.unregisterReceiver(sendMessage);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            isRunning = true;

            socket = new Socket(clientSettings.address, clientSettings.port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            enterPassword();
            enterNick();
            autoJoin();

            while (socket.isConnected()) {
                if (in.ready()) {
                    String s = in.readLine();
                    Log.i(TAG, s);
                    parse(s);
                } else {
                    Log.i(TAG, "Thread.sleep(100)");
                    Thread.sleep(100);
                }
                if (!isRunning) {
                    //TODO:
                    break;
                }
            }

        } catch (SocketException e) {
            //
            Log.e(TAG, e.toString());
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e("Client.run()", e.toString());
            }
            in = null;
            out = null;
            isRunning = false;
        }
        Log.i("Client.run()", "closed");
    }

    private void parse(String s) {
        Matcher message = Message.pattern.matcher(s);
        Matcher ping = Pattern.compile("PING ?:?(.*)").matcher(s);
        if (message.find()) {
            callbackMessage(new Message(message.group()));
        }
        if (ping.find()) {
            print("PONG :" + ping.group(1));
        }
    }

    private void enterPassword() {
        print("PASS " + clientSettings.password);
    }

    private void enterNick() {
        for (String nick : clientSettings.nicks) {
            print("NICK " + nick);
        }
    }

    private void print(String s) {
        out.println(s);
    }

    private void autoJoin() {
        for (String channel : clientSettings.joinList) {
            joinChannel(channel);
        }
    }

    private boolean callbackMessage(Message msg) {
        clientService.lbm.sendBroadcast(new Intent("new-message").putExtra("irc.Message", msg));
        return true;
    }

    public boolean isConnected() {
        return isRunning;
    }
}
