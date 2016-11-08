package ru.ifmo.android_2016.irc;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            id = savedInstanceState.getLong("Id");
            clientSettings = ServerList.getInstance().get(id);
        } else {
            load();
        }

        initView();

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

        startService(
                new Intent(ChatActivity.this, ClientService.class)
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
        private String[] channel = clientSettings.getChannels().split(",");

        public ViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return ChatFragment
                    .newInstance(MessageStorage.getInstance().getNewStorage(new ArrayList<>()));
        }

        @Override
        public int getCount() {
            return channel.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return channel[position];
        }
    }
}
