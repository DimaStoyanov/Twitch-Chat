package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.client.Channel;
import ru.ifmo.android_2016.irc.client.Client;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.constant.PreferencesConstant;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.SHOW_TAB_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.SPAM_MODE_KEY;

public class ChatActivity extends BaseActivity
        implements ClientService.OnConnectedListener, Client.Callback, NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = ChatActivity.class.getSimpleName();

    EditText typeMessage;
    private long id = 0;
    private int keyboardHeight;
    private ClientSettings clientSettings;
    @Nullable
    Client client;
    ViewPagerAdapter viewPagerAdapter;
    ViewPager viewPager, emotesViewPager;
    Toolbar toolbar;
    private boolean spamMode = false;
    private TextView chatTitle;
    protected FloatingActionButton fab;


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


        setObserverListener();

        typeMessage.setOnTouchListener(((view, motionEvent) -> {
            if (isEmotesShowing()) closeEmotes();
            return false;
        }));

        findViewById(R.id.send).setOnClickListener(v -> {
            Log.d(TAG, String.valueOf(viewPager.getCurrentItem()));
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (!TextUtils.isEmpty(typeMessage.getText()))
                sendMessage(typeMessage.getText().toString());
            if (!spamMode) typeMessage.setText("");
        });

        viewPager.setAdapter(viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }


            @Override
            public void onPageSelected(int position) {
                try {
                    closeEmotes();
                    changeCheckedMenuItem(position);
                    if (chatTitle != null && viewPagerAdapter.getPageTitle(position) != null) {
                        chatTitle.setText(viewPagerAdapter.getPageTitle(position));
                    }
                    fab.setOnClickListener(view1 -> viewPagerAdapter.fragments.get(position).scrollToBottom());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        //TODO: Temp
        tabLayout.setVisibility(View.GONE);
        ((AppBarLayout.LayoutParams) toolbar.getLayoutParams())
                .setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        //TODO: Temp

        chatTitle = (TextView) findViewById(R.id.title);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (client != null) {
            client.attachUi(this);
            onChannelChange();
        }
    }

    @Override
    public void getStartPreferences() {
        spamMode = prefs.getBoolean(PreferencesConstant.SPAM_MODE_KEY, false);
        findViewById(R.id.tabLayout).setVisibility(prefs.getBoolean(PreferencesConstant.SHOW_TAB_KEY, false) ? View.VISIBLE : View.GONE);

    }


    private void initView() {
        setContentView(R.layout.activity_chat_navigation);
        typeMessage = (EditText) findViewById(R.id.text_message);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        emotesViewPager = (ViewPager) findViewById(R.id.emotes_viewpager);
        fab = (FloatingActionButton) findViewById(R.id.fab);
    }


    private void setObserverListener() {
        // Determine keyboard height
        LinearLayout ll = (LinearLayout) findViewById(R.id.root_view);
        keyboardHeight = 550;
        ll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            ll.getWindowVisibleDisplayFrame(r);

            int screenHeight = ll.getRootView().getHeight();
            int heightDifference = screenHeight - (r.bottom - r.top);
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                heightDifference -= getResources().getDimensionPixelSize(resourceId);
            }
            if (heightDifference > 400) {
                keyboardHeight = heightDifference;
            }
//            Log.d("Keyboard Size", "Size: " + heightDifference);
        });

    }

    private void load() {
        id = getIntent().getLongExtra(SERVER_ID, 0);
        clientSettings = ServerList.getInstance().get(id);

        ClientService.startClient(this, id);
    }

    public void sendMessage(String message) {
        viewPagerAdapter.channels.get(viewPager.getCurrentItem())
                .send(message);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("Id", id);
    }

    public void onEmotesShowClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        if (isEmotesShowing()) {
            closeEmotes();
            return;
        }
        if (client == null || client.getChannelList() == null) {
            Toast.makeText(this, "Client loading, please wait", Toast.LENGTH_SHORT).show();
            return;
        }
        emotesViewPager.setAdapter(new EmotesViewPagerAdapter(getSupportFragmentManager()));
//            getTheme().
        emotesViewPager.setVisibility(View.VISIBLE);
        emotesViewPager.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeight));

    }


    private void closeEmotes() {
        emotesViewPager.setVisibility(View.GONE);
    }

    private boolean isEmotesShowing() {
        return emotesViewPager.getVisibility() == View.VISIBLE;
    }

    public void onClearClick(View view) {
        typeMessage.setText("");
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        viewPager.setCurrentItem(item.getItemId());
        changeCheckedMenuItem(item.getItemId());
        ((DrawerLayout) findViewById(R.id.drawer_layout)).closeDrawer(GravityCompat.START);
        return true;
    }


    private void changeCheckedMenuItem(int position) {
        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }
        menu.getItem(position).setChecked(true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        super.onSharedPreferenceChanged(sharedPreferences, s);
        switch (s) {
            case SHOW_TAB_KEY:
                findViewById(R.id.tabLayout).setVisibility(sharedPreferences.getBoolean(s, false) ? View.VISIBLE : View.GONE);
                break;
            case SPAM_MODE_KEY:
                spamMode = sharedPreferences.getBoolean(s, false);
                break;
        }
    }

    class EmotesViewPagerAdapter extends ViewPagerAdapter {


        EmotesViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return EmoteScrollViewFragment.newInstance(position == 0 ? "twitch" : "bttv");
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        } else if (isEmotesShowing()) {
            closeEmotes();
            return;
        }
        ClientService.stopClient(clientSettings.getId());
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (client != null) client.detachUi();
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
        if (viewPagerAdapter.channels.size() > 1)
            loadMenu();

    }

    public void loadMenu() {
        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
        menu.removeGroup(0);
        List<Channel> channels = client.getChannelList();
        for (int i = 0; i < channels.size(); i++) {
            menu.add(0, i, Menu.CATEGORY_CONTAINER, getChannelName(channels.get(i)))
                    .setIcon(i == 0 ? android.R.drawable.ic_dialog_info : android.R.drawable.stat_notify_chat)
                    .setCheckable(true);
        }
        viewPager.setCurrentItem(1);
        menu.getItem(1).setChecked(true);
    }

    private String getChannelName(Channel channel) {
        String name = channel.getName();
        return name.charAt(0) == '#' ? Character.toUpperCase(name.charAt(1)) + name.substring(2) : name;
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private List<Channel> channels = new ArrayList<>();
        private List<ChatFragment> fragments = new ArrayList<>();

        ViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }


        @Override
        public Fragment getItem(int position) {
            ChatFragment fragment = ChatFragment.newInstance(id, channels.get(position).getName());
            fragments.add(position, fragment);
            return fragment;
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, 1, Menu.CATEGORY_CONTAINER, "Settings").setOnMenuItemClickListener(menuItem -> {
            startActivityForResult(new Intent(this, PreferenceActivity.class), 228);
            return false;
        });
        return true;
    }
}
