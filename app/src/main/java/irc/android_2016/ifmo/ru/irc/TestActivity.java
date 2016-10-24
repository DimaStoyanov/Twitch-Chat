package irc.android_2016.ifmo.ru.irc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import irc.android_2016.ifmo.ru.irc.client.Client;
import irc.android_2016.ifmo.ru.irc.client.ClientService;
import irc.android_2016.ifmo.ru.irc.client.ClientServiceCallback;
import irc.android_2016.ifmo.ru.irc.client.ClientSettings;
import irc.android_2016.ifmo.ru.irc.client.Message;

import static android.view.View.GONE;

public class TestActivity extends AppCompatActivity
        implements View.OnClickListener, ClientServiceCallback {

    EditText server, nick, password, channel;
    TextView text;
    ScrollView scroll;

    ClientService.Binder binder = null;
    ClientSettings clientSettings;
    Client client;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (ClientService.Binder) service;
            Log.i("onServiceConnected", name.toString());
            ClientService.Binder binder = (ClientService.Binder) service;
            client = binder.getClient();
            client.attachActivity(TestActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            client.attachActivity(null);
            client = null;
            binder = null;
        }
    };

    @Override
    public void onMessageReceived(final Message msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append("<" + msg.from + " to " + msg.to + "> " + msg.text + "\n");
                scroll.post(new Runnable() {
                    @Override
                    public void run() {
                        scroll.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

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

        if (savedInstanceState != null) {
            server.setText(savedInstanceState.getString("Server"));
            nick.setText(savedInstanceState.getString("Nick"));
            password.setText(savedInstanceState.getString("Password"));
            channel.setText(savedInstanceState.getString("Channel"));
            text.setText(savedInstanceState.getString("Text"));

            if (savedInstanceState.getBoolean("isConnected")) {
                clientSettings = (ClientSettings) savedInstanceState.getSerializable("ClientSettings");
                Intent intent = new Intent(this, ClientService.class);
                intent.putExtra("ClientSettings", clientSettings);
                //bindService(intent, serviceConnection, BIND_AUTO_CREATE);
                disableEdits();
            }
        } else {
            load();
        }

        findViewById(R.id.connectButton).setOnClickListener(this);
        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (client != null) {
                    if (!client.sendMessage(new Message(
                            nick.getText().toString(),
                            channel.getText().toString(),
                            ((EditText) findViewById(R.id.typeMessage)).getText().toString()))) {
                        Toast.makeText(TestActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TestActivity.this, "Connect first!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void load() {
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        server.setText(pref.getString("Server", ""));
        nick.setText(pref.getString("Nick", ""));
        password.setText(pref.getString("Password", ""));
        channel.setText(pref.getString("Channel", ""));
    }

    void disableEdits() {
        server.setVisibility(GONE);
        nick.setVisibility(GONE);
        password.setVisibility(GONE);
        channel.setEnabled(false);
        findViewById(R.id.connectButton).setVisibility(GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isConnected", client != null);
        outState.putString("Server", server.getText().toString());
        outState.putString("Nick", nick.getText().toString());
        outState.putString("Password", password.getText().toString());
        outState.putString("Channel", channel.getText().toString());
        outState.putString("Text", text.getText().toString());
        outState.putSerializable("ClientSettings", clientSettings);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (client != null) {
            client.close();
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = pref.edit();
        ed.putString("Server", server.getText().toString());
        ed.putString("Nick", nick.getText().toString());
        ed.putString("Password", password.getText().toString());
        ed.putString("Channel", channel.getText().toString());
        ed.commit();
    }

    @Override
    public void onClick(View v) {
        Matcher addrport = Pattern.compile("([\\w.]+):?(\\d+)?").matcher(server.getText().toString());

        try {
            Intent intent = new Intent(this, ClientService.class);
            if (addrport.find()) {
                clientSettings = new ClientSettings()
                        .setAddress(addrport.group(1))
                        .setPort(Integer.decode(addrport.group(2)))
                        .setPassword(password.getText().toString())
                        .addNicks(nick.getText().toString())
                        .addChannels(channel.getText().toString().split(", "));
                intent.putExtra("ClientSettings", clientSettings);

                Log.i("onClick", "LUL");
                bindService(intent, serviceConnection, BIND_AUTO_CREATE);
            }
        } catch (UnknownHostException x) {
            Toast.makeText(this, x.getMessage(), Toast.LENGTH_SHORT).show();
        }
        disableEdits();
    }
}
