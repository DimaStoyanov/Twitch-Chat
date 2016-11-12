package ru.ifmo.android_2016.irc;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.client.Client;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.Message;
import ru.ifmo.android_2016.irc.client.MessageStorage;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.client.TwitchMessage;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;
import static ru.ifmo.android_2016.irc.client.ClientService.START_TWITCH_CLIENT;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = ChatActivity.class.getSimpleName();
    public static final String MESSAGE_STORAGE_ID = MessageStorage.class.getCanonicalName();

    private EditText typeMessage;
    private long id = 0;
    private ClientSettings clientSettings;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            id = savedInstanceState.getLong("Id");
            clientSettings = ServerList.getInstance().get(id);
            client = ClientService.getClient(id);
        } else {
            load();
        }

        initView();

        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, clientSettings.getNicks()[0] + " " + clientSettings.getChannel() + " " + typeMessage.getText().toString());
                LocalBroadcastManager
                        .getInstance(ChatActivity.this)
                        .sendBroadcast(new Intent("send-message")
                                .putExtra(Message.class.getCanonicalName(),
                                        new TwitchMessage().genPrivmsg(
                                                clientSettings.getChannel(),
                                                typeMessage.getText().toString())));
            }
        });

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
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
        typeMessage = (EditText) findViewById(R.id.text_message);
    }

    private void load() {
        id = getIntent().getLongExtra(SERVER_ID, 0);
        clientSettings = ServerList.getInstance().get(id);
        client = ClientService.getClient(id);

        startService(new Intent(ChatActivity.this, ClientService.class)
                .setAction(START_TWITCH_CLIENT)
                .putExtra(SERVER_ID, id));
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
        super.onDestroy();
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<String> channels = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
            if (client != null) {
                for (String channel : client.getChannels().keySet()) {
                    channels.add(channel);
                }
            }
        }

        @Override
        public Fragment getItem(int position) {
            return ChatFragment.newInstance(id, channels.get(position));
        }

        @Override
        public int getCount() {
            return channels.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return channels.get(position);
        }
    }
}
