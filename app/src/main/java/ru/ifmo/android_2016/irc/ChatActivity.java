package ru.ifmo.android_2016.irc;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.R;
import ru.ifmo.android_2016.irc.constant.FilePathConstant;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;
import ru.ifmo.android_2016.irc.loader.LoadResult;
import ru.ifmo.android_2016.irc.loader.TwitchEmotionsLoader;
import ru.ifmo.android_2016.irc.model.LoginData;
import ru.ifmo.android_2016.irc.model.TwitchEmoticon;
import ru.ifmo.android_2016.irc.utils.FileUtils;
import ru.ifmo.android_2016.irc.utils.IOUtils;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    IRCClientTask task = null;
    ScrollView scrollView;
    LinearLayout ll;
    TextView chanel;
    EditText msg;
    ArrayList<String> messages;
    ProgressBar pb;
    boolean saved, alertShow = true;
    boolean cancelTask = false;
    FilePathConstant constant;
    final String TAG = "IRC Chat";
    AlertDialog alertDialog;
    HashMap<String, String> emoticons; // all emoticons in pair key == regex value == url
    HashSet<String> simple_regex_emoticons; // all simple regex, that contains only alphabetic symbols
    List<String> regex_emoticons; // rest emoticons
    Bundle saveState;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        saveState = savedInstanceState;
        scrollView = (ScrollView) findViewById(R.id.scrollv);
        chanel = (TextView) findViewById(R.id.chanel);
        ll = (LinearLayout) findViewById(R.id.messages);
        msg = (EditText) findViewById(R.id.text_message);
        pb = (ProgressBar) findViewById(R.id.pbar);
        messages = new ArrayList<>();
        constant = new FilePathConstant(this);
        findViewById(R.id.send).setOnClickListener(this);
        emoticons = new HashMap<>();
        simple_regex_emoticons = new HashSet<>();
        regex_emoticons = new ArrayList<>();
        prepareEmoticons("global");
        String ch = getIntent().getExtras().getString("Channel");
        prepareEmoticons("forsenlol");
        initAlert();
    }


    private void initAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        builder.setTitle("No internet connection")
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        task.cancel(true);
                        initTask(null);
                    }
                }).setNeutralButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertShow = false;
                finishAffinity();
            }
        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertShow = false;
                dialogInterface.cancel();
            }
        });
        alertDialog = builder.create();

    }

    private void initTask(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            task = (IRCClientTask) getLastCustomNonConfigurationInstance();
            if (task != null) {
                Log.d(TAG, "task restored");
                restoreFromBundle(savedInstanceState);
                task.changeActivity(this);
            } else {
                Log.d(TAG, "New task from state");
                chanel.setText(savedInstanceState.getString("Channel"));
                task = new ChatActivity.IRCClientTask(this);
                task.execute(savedInstanceState.getString("Server"), savedInstanceState.getString("Nick"),
                        savedInstanceState.getString("Password"), savedInstanceState.getString("Channel"));
            }

        }

        if (task == null) {
            Log.d(TAG, "New task from intent");
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
            msg.setText(savedInstanceState.getString("TypedMessage"));
            messages = savedInstanceState.getStringArrayList("messages");
            task.initChatMessages(messages);

        }
    }


    private void prepareEmoticons(String channel) {
        Log.d(TAG, "Prepare emoticons");
        pb.setVisibility(View.VISIBLE);
        String path = constant.EMOTICONS_PACKAGE + File.separator + channel + File.separator + constant.EMOTICON_INFO_NAME
                + "." + constant.EMOTICON_INFO_EXTENSION;
        if (!new File(path).isFile()) {
            downloadEmoticons(channel, path);
        } else {
            readEmoticonsFromStorage(path, channel);
        }

    }


    private void downloadEmoticons(final String channel, final String path) {
        Log.d(TAG, "Download emoticons from internet");
        getSupportLoaderManager().initLoader(0, getIntent().getExtras(), new LoaderManager.LoaderCallbacks<LoadResult<List<TwitchEmoticon>>>() {
            @Override
            public Loader<LoadResult<List<TwitchEmoticon>>> onCreateLoader(int id, Bundle args) {
                return new TwitchEmotionsLoader(ChatActivity.this, channel, constant);
            }

            @Override
            public void onLoadFinished(Loader<LoadResult<List<TwitchEmoticon>>> loader, LoadResult<List<TwitchEmoticon>> data) {
                readEmoticonsFromStorage(path, channel);
                getSupportLoaderManager().destroyLoader(loader.getId());
            }

            @Override
            public void onLoaderReset(Loader<LoadResult<List<TwitchEmoticon>>> loader) {

            }
        });
    }

    // Maybe need retainFromCustom...
    private void readEmoticonsFromStorage(final String path, final String channel) {
        Log.d(TAG, "Read emoticons from file");
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(path));
                    String[] temp;
                    String line;
                    while ((line = in.readLine()) != null) {
                        temp = line.split("↨");
                        emoticons.put(temp[0], temp[2]);
                        if (Boolean.parseBoolean(temp[1])) {
                            simple_regex_emoticons.add(temp[0]);
                        } else {
                            regex_emoticons.add(temp[0]);
                        }
                    }
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                pb.setVisibility(View.GONE);
                if (channel.equals("global")) {
                    initTask(saveState);
                }
            }
        };
        task.executeOnExecutor(Executors.newFixedThreadPool(2));
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
                while (alertShow && !IOUtils.isConnectionAvailable(ChatActivity.this, false)) {
                    publishProgress("No internet connection\n");
                    Thread.sleep(100);
                }
                String[] sockaddr = server.split(":");
                socket = new Socket(InetAddress.getByName(sockaddr[0]), Integer.decode(sockaddr[1]));

                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();

                out.write(("PASS " + password + "\n").getBytes());
                out.write(("NICK " + nick + "\n").getBytes());
                out.write(("JOIN " + channel + "\n").getBytes());

                final byte[] buf = new byte[8192];
                // Если мы еще не сохраняли этот канал, нужно записать данные в файл
                if (!saved && socket.isConnected() && IOUtils.isConnectionAvailable(ChatActivity.this, false)) {
                    Log.d(TAG, "Save data to cache");
                    String file_path = constant.LOGIN_DATA;
                    if (!(new File(file_path)).exists()) {
                        Log.d(TAG, "Created new temp file");
                        file_path = FileUtils.createExternalFile(ChatActivity.this, constant.LOGIN_PACKAGE,
                                "login", "bin").getPath();
                    }
                    FileUtils.addLoginData(file_path, new LoginData(server, nick, password, channel));
                }

                while (socket.isConnected()) {
                    if (!IOUtils.isConnectionAvailable(activity, false)) {
                        publishProgress("No internet connection\n");
                    }
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
                            Log.w("System message", str);
                            publishProgress(str);
                        }
                    } else {
                        Thread.sleep(100);
                    }
                }

                publishProgress("Disconnected");
            } catch (InterruptedException | IOException x) {
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
                if ("No internet connection\n".equals(s)) {
                    if (!alertShow) return;
                    alertDialog.show();
                    return;
                }
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
            DraweeTextView textView = new DraweeTextView(activity);
            if (messageStr.contains("PRIVMSG ")) {
                messageStr = messageStr.substring(messageStr.indexOf("PRIVMSG ") + 8);
                String author = messageStr.substring(1, messageStr.indexOf(">:")) + ":";
                String content = messageStr.substring(messageStr.indexOf(">:") + 3, messageStr.indexOf("\n"));
                Spannable text = new SpannableString(author + " " + prepareMessageContent(content));
                text.setSpan(new StyleSpan(Typeface.BOLD), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(prepareMessageContent(author + " " + content));

            } else {
                // можно парсить как служебные сообщения в будущем
                textView.setText(messageStr);
            }
            textView.setLinksClickable(true);
            textView.setAutoLinkMask(Linkify.WEB_URLS);
            activity.ll.addView(textView);
            activity.scrollView.post(new Runnable() {
                @Override
                public void run() {
                    activity.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

        private CharSequence prepareMessageContent(String content) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            if (content.contains(" ")) {
                String author = content.substring(0, content.indexOf(" "));
                builder.append(author);
                builder.setSpan(new StyleSpan(Typeface.BOLD), 0, author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                content = content.substring(content.indexOf(" ") + 1);
            }
            String[] a = content.split(" ");
            for (String t : a) {
                if (simple_regex_emoticons.contains(t)) {
                    attachEmoticon(builder, emoticons.get(t));
                } else {
                    boolean is_emoticon = false;
                    for (int i = 0; i < regex_emoticons.size(); i++) {
                        if (Pattern.compile(regex_emoticons.get(i)).matcher(t).matches()) {
                            attachEmoticon(builder, emoticons.get(t));
                            is_emoticon = true;
                            break;
                        }
                    }
                    if (is_emoticon) continue;
                    builder.append(" ");
                    builder.append(t);
                }
            }
            return builder;
        }

        private void attachEmoticon(SpannableStringBuilder builder, String url) {
            int start = builder.length();
            builder.append(" [img]");
            builder.setSpan(new DraweeSpan.Builder(url)
                            .setLayout(30, 30).build(),
                    start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(" ");
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
        Log.d(TAG, "on back pressed");
       if(task != null) task.cancel(true);
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
        outState.putString("TypedMessage", msg.getText().toString());
        outState.putStringArrayList("messages", messages);
        outState.putBoolean("cancelTask", cancelTask);
    }


}
