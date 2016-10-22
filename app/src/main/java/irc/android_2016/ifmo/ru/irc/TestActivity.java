package irc.android_2016.ifmo.ru.irc;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.view.View.GONE;

public class TestActivity extends AppCompatActivity implements View.OnClickListener {

    EditText server, nick, password, channel;
    TextView text;
    ScrollView scroll;
    IRCClientTask task = null;

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
            task = (IRCClientTask) getLastCustomNonConfigurationInstance();
            if (task != null) {
                task.changeActivity(this);
            }
            server.setText(savedInstanceState.getString("Server"));
            nick.setText(savedInstanceState.getString("Nick"));
            password.setText(savedInstanceState.getString("Password"));
            channel.setText(savedInstanceState.getString("Channel"));
            text.setText(savedInstanceState.getString("Text"));

            if (savedInstanceState.getBoolean("isConnected")) {
                disableEdits();
            }
        } else {
            load();
        }

        findViewById(R.id.connectButton).setOnClickListener(this);
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
        //channel.setInputType();
        findViewById(R.id.connectButton).setVisibility(GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isConnected", task != null);
        outState.putString("Server", server.getText().toString());
        outState.putString("Nick", nick.getText().toString());
        outState.putString("Password", password.getText().toString());
        outState.putString("Channel", channel.getText().toString());
        outState.putString("Text", text.getText().toString());
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
        if (task == null) {
            task = new IRCClientTask(this);
            task.execute(server.getText().toString(), nick.getText().toString(),
                    password.getText().toString(), channel.getText().toString());
            disableEdits();
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return task;
    }

    private class IRCClientTask extends AsyncTask<String, String, Void> {
        private volatile TestActivity activity;
        private Pattern message = Pattern.compile(":([\\w]+)![\\w@.]+ PRIVMSG (#?[\\w]+) :(.*)");

        public IRCClientTask(TestActivity activity) {
            super();
            changeActivity(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            String server = params[0], nick = params[1], password = params[2], channel = params[3];
            try {
                String[] sockaddr = server.split(":");
                Socket socket = new Socket(InetAddress.getByName(sockaddr[0]), Integer.decode(sockaddr[1]));

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                out.write(("PASS " + password + "\n").getBytes());
                out.write(("NICK " + nick + "\n").getBytes());
                out.write(("JOIN " + channel + "\n").getBytes());

                final byte[] buf = new byte[8192];

                while (socket.isConnected()) {
                    if (in.available() > 0) {
                        String str = new String(buf, 0, in.read(buf));
                        Matcher matcher = message.matcher(str);
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            String to = matcher.group(2);
                            String msg = matcher.group(3);
                            Log.w("chat", str);
                            publishProgress("<" + sender + "> " + msg + "\n");
                        } else {
                            Log.w("kek", str);
                            publishProgress(str);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }

                publishProgress("Disconnected");
            } catch (Exception x) {
                publishProgress(x.getMessage());
            }
            return null;
        }

        void changeActivity(TestActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            for (String value : values) {
                activity.text.append(value);
            }
            activity.scroll.fullScroll(View.FOCUS_DOWN);
        }
    }
}
