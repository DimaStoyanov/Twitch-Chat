package ru.ifmo.android_2016.irc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.api.TwitchApi;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.Message;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;

public class NewChatActivity extends AppCompatActivity {
    private static final String TAG = NewChatActivity.class.getSimpleName();

    private LinearLayout chat_msg_container;
    private ScrollView scroll;
    private ClientSettings clientSettings;
    private EditText typeMessage;
    private ProgressBar progressBar;
    private String server, port, nick, password, channel;
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        if (savedInstanceState != null) {
            server = savedInstanceState.getString("Server");
            port = savedInstanceState.getString("Port");
            nick = savedInstanceState.getString("Nick");
            password = savedInstanceState.getString("Password");
            channel = savedInstanceState.getString("Channel");
        } else {
            load();
        }
        ((TextView) findViewById(R.id.channel)).setText(channel);
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, nick + " " + channel + " " + typeMessage.getText().toString());
                LocalBroadcastManager
                        .getInstance(NewChatActivity.this)
                        .sendBroadcast(new Intent("send-message")
                                .putExtra("ru.ifmo.android_2016.irc.Message",
                                        new TwitchMessage(nick, channel, typeMessage.getText().toString())));
            }
        });
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("new-message"));


    }


    private void initView() {
        setContentView(R.layout.activity_chat);
        chat_msg_container = (LinearLayout) findViewById(R.id.messages);
        scroll = (ScrollView) findViewById(R.id.scrollv);
        typeMessage = (EditText) findViewById(R.id.text_message);
        progressBar = (ProgressBar) findViewById(R.id.pbar);
        context = NewChatActivity.this;
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TwitchMessage msg = intent.getParcelableExtra("ru.ifmo.android_2016.irc.Message");
            DraweeTextView text = new DraweeTextView(context);
            text.setText(buildTextDraweeView(msg));
            chat_msg_container.addView(text);
            scroll.post(new Runnable() {
                @Override
                public void run() {
                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    };

    private CharSequence buildTextDraweeView(TwitchMessage msg) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(msg.getNickname()).append(": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        List<TwitchMessage.Emote> emotes = msg.getEmotes();
        if (emotes == null) {
            return builder.append(msg.text);
        }
        for (int i = 0; i < emotes.size(); i++) {
            Log.d(TAG, emotes.get(i).toString() + "\n");
        }
        String msg_content = msg.text;
        int offset = 0;
        TwitchMessage.Emote cur_emote;
        for (int i = 0; i < emotes.size(); i++) {
            cur_emote = emotes.get(i);
            if (cur_emote.getLength() == 0) continue;
            if (offset < cur_emote.getBegin()) {
                builder.append(msg_content.substring(offset, cur_emote.getBegin()));
                Log.d(TAG, "inserting text" + offset + " " + cur_emote.getBegin());
                offset = cur_emote.getBegin();

            }
            if (offset == cur_emote.getBegin()) {
                int start = builder.length();
                builder.append(" [img]");
                builder.setSpan(new DraweeSpan.Builder(TwitchApi.getEmoticonUrl(cur_emote.getEmoteName()))
                                .setLayout(50, 50).build(),
                        start, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(" ");
                offset += cur_emote.getLength();
            } else {
                throw new RuntimeException("Error while parsing message" + msg.text
                        + " with badges" + emotes.toString());
            }
        }
        if (offset < msg_content.length() - 1) {
            builder.append(msg_content.substring(offset));
        }
        return builder;
    }

    private void load() {
        Bundle data = getIntent().getExtras();
        server = data.getString("Server");
        port = data.getString("Port");
        nick = data.getString("Nick");
        password = data.getString("Password");
        channel = data.getString("Channel");
        if (!checkNonNull(server, port, nick, password, channel)) {
            throw new RuntimeException("Invalid login data");
        }

        clientSettings = new ClientSettings.Builder()
                .setAddress(server)
                .setPort(Integer.parseInt(port))
                .setPassword(password)
                .addNicks(nick)
                .setChannels(channel)
                .build();

        Intent intent = new Intent(NewChatActivity.this, ClientService.class);
        intent.setAction(ClientService.START_TWITCH_CLIENT);
        intent.putExtra("ru.ifmo.android_2016.irc.ClientSettings", (Parcelable) clientSettings);
        startService(intent);
    }


    private boolean checkNonNull(Object... args) {
        for (Object o : args) {
            if (o == null)
                return false;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("Server", server);
        outState.putString("Nick", nick);
        outState.putString("Password", password);
        outState.putString("Channel", channel);
        outState.putStringArrayList("Messages", getMessages());
        outState.putString("Port", port);
    }

    @Override
    public void onBackPressed() {
        startService(new Intent(this, ClientService.class).setAction(ClientService.STOP_CLIENT));
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        super.onDestroy();
    }


    public ArrayList<String> getMessages() {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < chat_msg_container.getChildCount(); i++) {
            result.add(((DraweeTextView) chat_msg_container.getChildAt(i)).getText().toString());
        }
        return result;
    }
}
