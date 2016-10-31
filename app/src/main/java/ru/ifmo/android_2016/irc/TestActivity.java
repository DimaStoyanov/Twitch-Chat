package ru.ifmo.android_2016.irc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.Message;

import static android.view.View.GONE;

public class TestActivity extends AppCompatActivity
        implements View.OnClickListener {
    private static final String TAG = TestActivity.class.getSimpleName();

    EditText server, nick, password, channel;
    TextView text;
    ScrollView scroll;
    private ClientSettings clientSettings;
    private boolean isConnected = false;


    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = intent.getParcelableExtra("ru.ifmo.android_2016.irc.Message");
            text.append("<" + msg.from + " to " + msg.to + "> " + msg.text + "\n");
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    };
    private EditText typeMessage;
    private EditText port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        server = (EditText) findViewById(R.id.server);
        nick = (EditText) findViewById(R.id.nick);
        password = (EditText) findViewById(R.id.password);
        channel = (EditText) findViewById(R.id.channel);
        text = (TextView) findViewById(R.id.text);
        scroll = (ScrollView) findViewById(R.id.scroll);
        typeMessage = (EditText) findViewById(R.id.typeMessage);
        port = (EditText) findViewById(R.id.port);

        if (savedInstanceState != null) {
            server.setText(savedInstanceState.getString("Server"));
            nick.setText(savedInstanceState.getString("Nick"));
            password.setText(savedInstanceState.getString("Password"));
            channel.setText(savedInstanceState.getString("Channel"));
            text.setText(savedInstanceState.getString("Text"));
            port.setText(savedInstanceState.getString("Port"));

            if (savedInstanceState.getBoolean("isConnected")) {
                disableEdits();
            }
        } else {
            load();
        }

        findViewById(R.id.connectButton).setOnClickListener(this);
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalBroadcastManager
                        .getInstance(TestActivity.this)
                        .sendBroadcast(new Intent("send-message")
                                .putExtra("ru.ifmo.android_2016.irc.Message",
                                        new Message(
                                                nick.getText().toString(),
                                                channel.getText().toString(),
                                                typeMessage.getText().toString())));
            }
        });
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("new-message"));
    }

    private void load() {
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        server.setText(pref.getString("Server", ""));
        nick.setText(pref.getString("Nick", ""));
        password.setText(pref.getString("Password", ""));
        channel.setText(pref.getString("Channel", ""));
        port.setText(pref.getString("Port", ""));
    }

    void disableEdits() {
        server.setVisibility(GONE);
        nick.setVisibility(GONE);
        password.setVisibility(GONE);
        channel.setEnabled(false);
        findViewById(R.id.connectButton).setVisibility(GONE);
        port.setVisibility(GONE);
        isConnected = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isConnected", isConnected);
        outState.putString("Server", server.getText().toString());
        outState.putString("Nick", nick.getText().toString());
        outState.putString("Password", password.getText().toString());
        outState.putString("Channel", channel.getText().toString());
        outState.putString("Text", text.getText().toString());
        outState.putString("Port", port.getText().toString());
    }

    @Override
    public void onBackPressed() {
        startService(new Intent(this, ClientService.class).setAction(ClientService.STOP_CLIENT));
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = pref.edit();
        ed.putString("Server", server.getText().toString());
        ed.putString("Nick", nick.getText().toString());
        ed.putString("Password", password.getText().toString());
        ed.putString("Channel", channel.getText().toString());
        ed.putString("Port", port.getText().toString());
        ed.apply();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        clientSettings = new ClientSettings.Builder()
                .setAddress(server.getText().toString())
                .setPort(Integer.decode(port.getText().toString()))
                .setPassword(password.getText().toString())
                .addNicks(nick.getText().toString())
                .setChannels(channel.getText().toString())
                .build();

        Intent intent = new Intent(TestActivity.this, ClientService.class);
        intent.setAction(ClientService.START_TWITCH_CLIENT);
        intent.putExtra("ru.ifmo.android_2016.irc.ClientSettings", (Parcelable) clientSettings);
        startService(intent);
        disableEdits();
    }
}
