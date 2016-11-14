package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.EditText;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.client.Channel;
import ru.ifmo.android_2016.irc.client.Client;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.ServerList;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;

public class ChatActivity extends AppCompatActivity
        implements ClientService.OnConnectedListener, Client.Callback {
    private static final String TAG = ChatActivity.class.getSimpleName();

    private EditText typeMessage;
    private long id = 0;
    private ClientSettings clientSettings;
    @Nullable
    private Client client;
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;

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

        findViewById(R.id.send).setOnClickListener(v -> {
            viewPager.getCurrentItem();
        });

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        toolbar.setNavigationOnClickListener(v -> finish());

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (client != null) {
            client.attachUi(this);
            onChannelChange();
        }
    }


    private void initView() {
        setContentView(R.layout.activity_chat);
        typeMessage = (EditText) findViewById(R.id.text_message);
    }

    private void load() {
        id = getIntent().getLongExtra(SERVER_ID, 0);
        clientSettings = ServerList.getInstance().get(id);

        ClientService.startClient(this, id);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("Id", id);
    }

    @Override
    public void onBackPressed() {
        ClientService.stopClient(clientSettings.getId());
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        client.detachUi();
        super.onDestroy();
    }

    @Override
    @UiThread
    public void onConnected(final Client client) {
        ChatActivity.this.client = client;
        client.attachUi(this);
        //onChannelChange();
    }

    @Override
    @UiThread
    public void onChannelChange() {
        Log.d(TAG, "onChannelChange");
        viewPagerAdapter.clearAndDetach();
        viewPagerAdapter.channels.addAll(client.getChannels().values());
        Stream.of(client.getChannels().values()).forEach(c -> Log.d(TAG, c.getName()));
        viewPagerAdapter.notifyDataSetChanged();
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<Channel> channels = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return ChatFragment.newInstance(id, channels.get(position).getName());
        }

        @Override
        public int getCount() {
            return channels.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return channels.get(position).getName();
        }

        public void clearAndDetach() {
            Stream.of(channels).forEach(Channel::detachUi);
            channels.clear();
            notifyDataSetChanged();
        }
    }
}
