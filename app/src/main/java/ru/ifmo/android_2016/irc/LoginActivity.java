package ru.ifmo.android_2016.irc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.client.ClientService;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText server, nick, password, channel;
    ProgressBar pb;
    ImageView server_status, nick_status, password_status, channel_status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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

    // Пока нету шаблона логина, все же пусть будет чтения с кэша
    private void readFromCache() {
        pb.setVisibility(View.VISIBLE);
        (new AsyncTask<Void, String[], String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                SharedPreferences pref = getPreferences(MODE_PRIVATE);
                return new String[]{pref.getString("Server", ""), pref.getString("Nick", ""),
                        pref.getString("Password", ""), pref.getString("Channel", "")};
            }

            @Override
            protected void onPostExecute(String[] res) {
                super.onPostExecute(res);
                server.setText(res[0]);
                nick.setText(res[1]);
                password.setText(res[2]);
                channel.setText(res[3]);
                pb.setVisibility(View.GONE);
            }
        }).execute();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // тут не нужен лоадер, ибо apply пиште данные в бекграунде
        SharedPreferences.Editor ed = getPreferences(MODE_PRIVATE).edit();
        ed.putString("Server", server.getText().toString());
        ed.putString("Nick", nick.getText().toString());
        ed.putString("Password", password.getText().toString());
        ed.putString("Channel", channel.getText().toString());
        ed.apply();
        /* Test */
        stopService(new Intent(this, ClientService.class));
        /* Test */
    }

    private void init() {
        server = (EditText) findViewById(R.id.server);
        nick = (EditText) findViewById(R.id.nick);
        password = (EditText) findViewById(R.id.password);
        channel = (EditText) findViewById(R.id.channel);
        server_status = (ImageView) findViewById(R.id.server_status);
        nick_status = (ImageView) findViewById(R.id.nick_status);
        password_status = (ImageView) findViewById(R.id.password_status);
        channel_status = (ImageView) findViewById(R.id.channel_status);


        server.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                server_status.setImageResource(Patterns.WEB_URL.matcher(editable).matches() ?
                        R.drawable.ok : R.drawable.not_ok);
            }

        });

        nick.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                nick_status.setImageResource(Pattern.compile("^[_a-z-A-Z0-9]+$").matcher(editable).matches() ?
                        R.drawable.ok : R.drawable.not_ok);
            }
        });

        password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                password_status.setImageResource(Pattern.compile("^[_a-z-A-Z0-9]+$").matcher(editable).matches() ?
                        R.drawable.ok : R.drawable.not_ok);

            }
        });

        channel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                channel_status.setImageResource(Pattern.compile("^#[_a-z-A-Z0-9]+$").matcher(editable).matches() ?
                        R.drawable.ok : R.drawable.not_ok);
            }
        });

        pb = (ProgressBar) findViewById(R.id.pbar);
    }

    private void readFromBundle(Bundle bundle) {
        server.setText(bundle.getString("Server"));
        nick.setText(bundle.getString("Nick"));
        password.setText(bundle.getString("Password"));
        channel.setText(bundle.getString("Channel"));
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, NewChatActivity.class);
        intent.putExtra("Server", server.getText().toString());
        intent.putExtra("Port", "6667");
        intent.putExtra("Nick", nick.getText().toString());
        intent.putExtra("Password", password.getText().toString());
        intent.putExtra("Channel", channel.getText().toString());
        intent.putExtra("Saved", false);
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


}
