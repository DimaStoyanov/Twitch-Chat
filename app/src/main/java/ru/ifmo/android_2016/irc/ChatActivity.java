package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotes;
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
    private int keyboardHeight;
    private ClientSettings clientSettings;
    @Nullable
    private Client client;
    private ViewPagerAdapter viewPagerAdapter;
    private ViewPager viewPager;
    private ScrollView emotes_scroll;
    private LinearLayout emotes_ll;

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
        // Determine keyboard height
        LinearLayout ll = (LinearLayout) findViewById(R.id.root_view);
        keyboardHeight = 550;
        ll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            ll.getWindowVisibleDisplayFrame(r);

            int screenHeight = ll.getRootView()
                    .getHeight();
            int heightDifference = screenHeight
                    - (r.bottom - r.top);
            int resourceId = getResources()
                    .getIdentifier("status_bar_height",
                            "dimen", "android");
            if (resourceId > 0) {
                heightDifference -= getResources()
                        .getDimensionPixelSize(resourceId);
            }
            if (heightDifference > 100) {
                keyboardHeight = heightDifference;
            }

            Log.d("Keyboard Size", "Size: " + heightDifference);
        });


        typeMessage.setOnTouchListener(((view, motionEvent) -> {
            if (emotes_scroll.getVisibility() == View.VISIBLE)
                closeEmotes();
            return false;
        }));

        findViewById(R.id.send).setOnClickListener(v -> {
            Log.d(TAG, String.valueOf(viewPager.getCurrentItem()));
            viewPagerAdapter.channels.get(viewPager.getCurrentItem())
                    .send(typeMessage.getText().toString());
            typeMessage.setText("");
        });

        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                closeEmotes();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
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
        emotes_scroll = (ScrollView) findViewById(R.id.emotes_scroll);
        emotes_ll = (LinearLayout) findViewById(R.id.emotes_ll);
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

    public void onEmotesShowClick(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        if (isEmotesShowing()) {
            closeEmotes();
            return;
        }
        String channel = client.getChannelList().get(viewPager.getCurrentItem()).getName();
        int columns = viewPager.getWidth() / 120;
        Log.d(TAG, channel);
        Object[] keyset = BttvEmotes.getChannelEmotesKey(channel);
        Log.d(TAG, "Keyset length " + keyset.length);

        int i = 0;
        while (i < keyset.length) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int j = 0;
            while (i < keyset.length && j++ < columns) {
                SimpleDraweeView emote = new SimpleDraweeView(this);
                LinearLayout.LayoutParams emoteParams = new LinearLayout.LayoutParams(100, 100);
                emoteParams.setMargins(10, 10, 10, 10);
                emoteParams.weight = 1;
                emote.setLayoutParams(emoteParams);
                emote.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);
                emote.setOnClickListener(new OnEmotesClickListener((String) keyset[i]));
                emote.setImageURI(Uri.parse(BttvEmotes.getEmoteUrlByCode(String.valueOf(keyset[i++]), channel)));
                row.addView(emote);
            }
            emotes_ll.addView(row);
        }
        emotes_scroll.setVisibility(View.VISIBLE);
        emotes_scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeight));

    }

    private class OnEmotesClickListener implements View.OnClickListener {
        final String code;

        OnEmotesClickListener(String code) {
            this.code = code;
        }

        @Override
        public void onClick(View view) {
            typeMessage.append(code + " ");
        }
    }

    private void closeEmotes() {
        emotes_ll.removeAllViews();
        emotes_scroll.setVisibility(View.GONE);
    }

    private boolean isEmotesShowing() {
        return emotes_scroll.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onBackPressed() {
        if (isEmotesShowing()) {
            closeEmotes();
            return;
        }
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
        onChannelChange();
    }

    @Override
    @UiThread
    public void onChannelChange() {
        Log.d(TAG, "onChannelChange");
        viewPagerAdapter.channels.clear();
        viewPagerAdapter.channels.addAll(client.getChannelList());
        //Stream.of(client.getChannelList()).forEach(c -> Log.d(TAG, c.getName()));
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
    }
}
