package irc.android_2016.ifmo.ru.irc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import irc.android_2016.ifmo.ru.irc.client.ClientService;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText server, nick, password, channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d("IRC", "on create");
        init();
        findViewById(R.id.connectButton).setOnClickListener(this);
        if (savedInstanceState != null) {
            Log.d("IRC Login", "Read from saved state");
            readFromBundle(savedInstanceState);
        } else {
            Log.d("IRC Login", "Read from cache");
            readFromCache();
        }
        /* Test */
        Button test = new Button(this);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, TestActivity.class));
            }
        });
        test.setText("TEST");
        ((RelativeLayout) findViewById(R.id.connectButton).getParent()).addView(test);
        Intent intent = new Intent(this, ClientService.class);
        intent.setAction(ClientService.START_FOREGROUND);
        startService(intent);
        /* Test */
    }

    private void init() {
        server = (EditText) findViewById(R.id.server);
        nick = (EditText) findViewById(R.id.nick);
        password = (EditText) findViewById(R.id.password);
        channel = (EditText) findViewById(R.id.channel);
    }

    private void readFromBundle(Bundle bundle) {
        server.setText(bundle.getString("Server"));
        nick.setText(bundle.getString("Nick"));
        password.setText(bundle.getString("Password"));
        channel.setText(bundle.getString("Channel"));
    }

    private void readFromCache() {
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        server.setText(pref.getString("Server", ""));
        nick.setText(pref.getString("Nick", ""));
        password.setText(pref.getString("Password", ""));
        channel.setText(pref.getString("Channel", ""));
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("Server", server.getText().toString());
        intent.putExtra("Nick", nick.getText().toString());
        intent.putExtra("Password", password.getText().toString());
        intent.putExtra("Channel", channel.getText().toString());
        startActivity(intent);

    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("IRC Login", "Save instance state");
        outState.putString("Server", server.getText().toString());
        outState.putString("Nick", nick.getText().toString());
        outState.putString("Password", password.getText().toString());
        outState.putString("Channel", channel.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences pref = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor ed = pref.edit();
        ed.putString("Server", server.getText().toString());
        ed.putString("Nick", nick.getText().toString());
        ed.putString("Password", password.getText().toString());
        ed.putString("Channel", channel.getText().toString());
        Log.d("IRC Login", "Save data to cache");
        ed.commit();
        /* Test */
        stopService(new Intent(this, ClientService.class));
        /* Test */
    }

}
