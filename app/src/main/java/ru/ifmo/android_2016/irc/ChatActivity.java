package ru.ifmo.android_2016.irc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;
import ru.ifmo.android_2016.irc.utils.ObjectUtils;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;
import static ru.ifmo.android_2016.irc.client.ClientService.START_TWITCH_CLIENT;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private LinearLayout chat_msg_container;
    private NestedScrollView scroll;
    private EditText typeMessage;
    private ProgressBar progressBar;
    private long id = 0;
    private ClientSettings clientSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();

        if (savedInstanceState != null) {
            id = savedInstanceState.getLong("Id");
            clientSettings = ServerList.getInstance().get(id);
        } else {
            load();
        }
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, clientSettings.getNicks()[0] + " " + clientSettings.getChannels() + " " + typeMessage.getText().toString());
                LocalBroadcastManager
                        .getInstance(ChatActivity.this)
                        .sendBroadcast(new Intent("send-message")
                                .putExtra(Message.class.getCanonicalName(),
                                        new TwitchMessage().genPrivmsg(
                                                clientSettings.getChannels(),
                                                typeMessage.getText().toString())));
            }
        });
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("new-message"));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText(clientSettings.getChannels()));
        tabLayout.addTab(tabLayout.newTab().setText("Lol"));
        tabLayout.addTab(tabLayout.newTab().setText("1"));
        tabLayout.addTab(tabLayout.newTab().setText("2"));
        tabLayout.addTab(tabLayout.newTab().setText("3"));
        tabLayout.addTab(tabLayout.newTab().setText("4"));
        tabLayout.addTab(tabLayout.newTab().setText("5"));
        tabLayout.addTab(tabLayout.newTab().setText("6"));
        tabLayout.addTab(tabLayout.newTab().setText("7"));
        tabLayout.addTab(tabLayout.newTab().setText("8"));
        tabLayout.addTab(tabLayout.newTab().setText("9"));
        tabLayout.addTab(tabLayout.newTab().setText("10"));
        tabLayout.addTab(tabLayout.newTab().setText("11"));
        tabLayout.addTab(tabLayout.newTab().setText("12"));
        tabLayout.addTab(tabLayout.newTab().setText("13"));
        tabLayout.addTab(tabLayout.newTab().setText("14"));
        tabLayout.addTab(tabLayout.newTab().setText("15"));
        tabLayout.addTab(tabLayout.newTab().setText("16"));
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }


    private void initView() {
        setContentView(R.layout.activity_chat);
        chat_msg_container = (LinearLayout) findViewById(R.id.messages);
        scroll = (NestedScrollView) findViewById(R.id.scrollv);
        typeMessage = (EditText) findViewById(R.id.text_message);
        progressBar = (ProgressBar) findViewById(R.id.pbar);
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, Thread.currentThread().getName());
            TwitchMessage msg = intent.getParcelableExtra(Message.class.getCanonicalName());
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
        builder.append(msg.getNickname());
        builder.append(msg.getAction() ? " " : ": ");
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        List<TwitchMessage.Emote> emotes = msg.getEmotes();
        if (emotes == null) {
            return builder.append(msg.getTrailing());
        }
        String msg_content = msg.getTrailing();
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
        if (msg.getAction() && msg.getColor() != 0)
            builder.setSpan(new ForegroundColorSpan(msg.getColor()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private void load() {
        id = getIntent().getLongExtra(SERVER_ID, 0);
        clientSettings = ServerList.getInstance().get(id);

        startService(
                new Intent(ChatActivity.this, ClientService.class)
                .setAction(START_TWITCH_CLIENT)
                .putExtra(SERVER_ID, id));
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("Messages", getMessages());
        outState.putLong("Id", id);
    }

    @Override
    public void onBackPressed() {
        startService(new Intent(this, ClientService.class)
                .setAction(ClientService.STOP_CLIENT)
                .putExtra(SERVER_ID, id));
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
