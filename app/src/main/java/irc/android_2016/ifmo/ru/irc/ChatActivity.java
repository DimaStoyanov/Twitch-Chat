package irc.android_2016.ifmo.ru.irc;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import irc.android_2016.ifmo.ru.irc.model.LoginData;
import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    IRCClientTask task;
    ScrollView scrollView;
    LinearLayout ll;
    TextView chanel;
    EditText msg;
    boolean saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        scrollView = (ScrollView) findViewById(R.id.scrollv);
        chanel = (TextView) findViewById(R.id.chanel);
        ll = (LinearLayout) findViewById(R.id.messages);
        msg = (EditText) findViewById(R.id.text_message);
        findViewById(R.id.send).setOnClickListener(this);
        if (savedInstanceState != null) {
            task = (IRCClientTask) getLastCustomNonConfigurationInstance();
            if (task != null) {
                Log.d("IRC Chat", "task restored");
                task.changeActivity(this);
            } else {
                Log.d("IRC Chat", "New task from state");
                chanel.setText(savedInstanceState.getString("Channel"));
                task = new ChatActivity.IRCClientTask(this);
                task.execute(savedInstanceState.getString("Server"), savedInstanceState.getString("Nick"),
                        savedInstanceState.getString("Password"), savedInstanceState.getString("Channel"));
            }

        }
        if (task == null) {
            Log.d("IRC Chat", "New task from intent");
            task = new ChatActivity.IRCClientTask(this);
            Bundle data = getIntent().getExtras();
            saved = data.getBoolean("Saved");
            System.out.println("Saved= " + saved);
            chanel.setText(data.getString("Channel"));
            task.execute(data.getString("Server"), data.getString("Nick"),
                    data.getString("Password"), data.getString("Channel"));
        }
    }

    @Override
    public void onClick(View v) {
        task.onMessageSend(getIntent().getExtras().getString("Nick"),
                msg.getText().toString(), getIntent().getExtras().getString("Channel"));
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle data = getIntent().getExtras();
        outState.putString("Server", data.getString("Server"));
        outState.putString("Nick", data.getString("Nick"));
        outState.putString("Password", data.getString("Password"));
        outState.putString("Channel", data.getString("Channel"));

    }

    private class IRCClientTask extends AsyncTask<String, String, Void> {
        private volatile ChatActivity activity;
        private Pattern message = Pattern.compile(":([\\w]+)![\\w@.]+ PRIVMSG (#?[\\w]+) :(.*)");
        private Socket socket;
        private OutputStream out;

        public IRCClientTask(ChatActivity activity) {
            super();
            changeActivity(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            String server = params[0], nick = params[1], password = params[2], channel = params[3];
            Log.d("IRC", "Server=" + server + " nick=" + nick + " password=" + password + " channel=" + channel);
            try {
                String[] sockaddr = server.split(":");
                socket = new Socket(InetAddress.getByName(sockaddr[0]), Integer.decode(sockaddr[1]));

                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();

                out.write(("PASS " + password + "\n").getBytes());
                out.write(("NICK " + nick + "\n").getBytes());
                out.write(("JOIN " + channel + "\n").getBytes());

                final byte[] buf = new byte[8192];

                // Если мы еще не сохраняли этот канал, нужно записать данные в файл
                if (!saved && socket.isConnected()) {
                    Log.d("IRC Chat", "Save data to cache");
                    SharedPreferences pref = getSharedPreferences("Login_data", MODE_PRIVATE);
                    String file_path = pref.getString("file_path", "");
                    if (!(new File(file_path)).exists()) {
                        Log.d("IRC Chat", "Created new temp file");
                        file_path = FileUtils.getPathOfExternalFile(ChatActivity.this);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("file_path", file_path);
                        editor.apply();
                    }
                    FileUtils.addData(file_path, new LoginData(server, nick, password, channel));
                }

                while (socket.isConnected()) {
                    if (in.available() > 0) {
                        String str = new String(buf, 0, in.read(buf));
                        Matcher matcher = message.matcher(str);
                        if (matcher.find()) {
                            String sender = matcher.group(1);
                            String to = matcher.group(2);
                            String msg = matcher.group(3);
                            Log.w("chat", str);
                            publishProgress("PRIVMSG <" + sender + ">: " + msg + "\n");
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

        @Override
        protected void onCancelled() {
            super.onCancelled();
            try {
                socket.close();
            } catch (IOException x) {
                Log.e("IRCTask.onCanceled", x.getMessage());
            }
            Log.i("IRCTask", "canceled");
        }

        void changeActivity(ChatActivity activity) {
            this.activity = activity;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            LayoutInflater inflater = LayoutInflater.from(ChatActivity.this);
            View message;
            for (String value : values) {
                if (value.contains("PRIVMSG ")) {
                    value = value.substring(value.indexOf("PRIVMSG ") + 8);

                    message = inflater.inflate(R.layout.chat_message, null);
                    ((TextView) message.findViewById(R.id.author)).setText(value.substring(1, value.indexOf(">")));
                    ((TextView) message.findViewById(R.id.content)).setText(value.substring(value.indexOf(">") + 1, value.indexOf("\n")));
                    activity.ll.addView(message);
                } else {
                    // можно парсить как служебные сообщения в будущем
                    TextView tv = new TextView(ChatActivity.this);
                    tv.setText(value);
                    activity.ll.addView(tv);
                }
            }
            activity.scrollView.fullScroll(View.FOCUS_DOWN);
        }

        void onMessageSend(String nick, String message, String channel) {
            try {
                if (out != null) {
                    Log.d("IRC", "sending: <" + nick + "> " + message);
                    out.write(("PRIVMSG " + channel + " :" + message + "\n").getBytes());
                    onProgressUpdate("PRIVMSG <" + nick + ">: " + message + "\n");
                    msg.setText("");
                }
            } catch (IOException e) {
                publishProgress(e.getMessage());
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("IRC Chat", "On destroy");
        task.cancel(true);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return task;
    }

}
