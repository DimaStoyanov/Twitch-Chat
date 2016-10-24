package irc.android_2016.ifmo.ru.irc;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import irc.android_2016.ifmo.ru.irc.model.LoginData;
import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    IRCClientTask task = null;
    ScrollView scrollView;
    LinearLayout ll;
    TextView chanel;
    EditText msg;
    boolean saved;
    ArrayList<String> messages;
    boolean cancelTask = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        scrollView = (ScrollView) findViewById(R.id.scrollv);
        chanel = (TextView) findViewById(R.id.chanel);
        ll = (LinearLayout) findViewById(R.id.messages);
        msg = (EditText) findViewById(R.id.text_message);
//        msg.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean hasFocus) {
//                if (hasFocus) {
//                    scrollView.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
//                        }
//                    }, 12200);
//                } else {
//                }
//            }
//        });

        findViewById(R.id.send).setOnClickListener(this);
        messages = new ArrayList<>();
        if (savedInstanceState != null) {
            task = (IRCClientTask) getLastCustomNonConfigurationInstance();
            if (task != null) {
                Log.d("IRC Chat", "task restored");
                restoreFromBundle(savedInstanceState);
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


    private void restoreFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey("cancelTask") && !savedInstanceState.getBoolean("cancelTask")
                && savedInstanceState.getStringArrayList("messages") != null) {
            messages = savedInstanceState.getStringArrayList("messages");
            task.initChatMessages(messages);

        }
    }


    @Override
    public void onClick(View v) {
        task.onMessageSend(getIntent().getExtras().getString("Nick"),
                msg.getText().toString(), getIntent().getExtras().getString("Channel"));
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
            for (String s : values) {
                messages.add(s);
                publishMessage(s);
            }

        }

        private void initChatMessages(List<String> messages) {
            for (String s : messages) {
                publishProgress(s);
            }
        }

        private void publishMessage(String messageStr) {
            TextView textView = new TextView(activity);
            if (messageStr.contains("PRIVMSG ")) {
                messageStr = messageStr.substring(messageStr.indexOf("PRIVMSG ") + 8);
                String author = messageStr.substring(1, messageStr.indexOf(">:")) + ":";
                String content = messageStr.substring(messageStr.indexOf(">:") + 3, messageStr.indexOf("\n"));
                Spannable text = new SpannableString(author + " " + content);
                text.setSpan(new StyleSpan(Typeface.BOLD), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(text);
            } else {
                // можно парсить как служебные сообщения в будущем
                textView.setText(messageStr);
            }
            textView.setLinksClickable(true);
            textView.setTextIsSelectable(true);
            activity.ll.addView(textView);
            activity.scrollView.post(new Runnable() {
                @Override
                public void run() {
                    activity.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
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
    public Object onRetainCustomNonConfigurationInstance() {
        return task;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("IRC chat", "on back pressed");
        task.cancel(true);
        cancelTask = true;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle data = getIntent().getExtras();
        outState.putString("Server", data.getString("Server"));
        outState.putString("Nick", data.getString("Nick"));
        outState.putString("Password", data.getString("Password"));
        outState.putString("Channel", data.getString("Channel"));
        outState.putStringArrayList("messages", messages);
        outState.putBoolean("cancelTask", cancelTask);
    }


}
