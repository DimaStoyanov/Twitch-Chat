package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Optional;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.android_2016.irc.client.Channel;
import ru.ifmo.android_2016.irc.client.Client;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.constant.PreferencesConstant;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.SHOW_TAB_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.SPAM_MODE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXT_SIZE_KEY;

public final class ChatActivity extends BaseActivity implements Client.Callback {
    private static final String TAG = ChatActivity.class.getSimpleName();
    public static final String CHANNEL = "Channel";

    EditText typeMessage;
    private long id = 0;
    private int keyboardHeight;
    @Nullable
    Client client;
    private ChatFragmentPagerAdapter viewPagerAdapter;
    ViewPager viewPager;
    private ViewPager emotesViewPager;
    private LinearLayout content, emotesLl;
    private boolean spamMode = false;
    private TextView chatTitle;
    FloatingActionButton fab;
    @Nullable
    private CharSequence lastPosition = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            id = savedInstanceState.getLong("Id");
        } else {
            id = getIntent().getLongExtra(SERVER_ID, 0);
        }

        ClientService.startClient(this, id, this::onConnected);

        initView();


        setObserverListener();

        typeMessage.setOnTouchListener(((view, motionEvent) -> {
            if (isEmotesShowing()) closeEmotes();
            return false;
        }));

        findViewById(R.id.send).setOnClickListener(v -> {
            Log.d(TAG, String.valueOf(viewPager.getCurrentItem()));
            if (((ViewPager) findViewById(R.id.viewPager)).getCurrentItem() == 0) return;
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (!TextUtils.isEmpty(typeMessage.getText()))
                sendMessage(typeMessage.getText().toString());
            if (!spamMode) typeMessage.setText("");
        });

        viewPager.setAdapter(viewPagerAdapter = new ChatFragmentPagerAdapter(getSupportFragmentManager()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }


            @Override
            public void onPageSelected(final int position) {
                lastPosition = viewPagerAdapter.getPageTitle(position);

                try {
                    closeEmotes();
                    changeCheckedMenuItem(position);
                    if (chatTitle != null) {
                        chatTitle.setText(viewPagerAdapter.getPageTitle(position));
                    }
                    //TODO:
                    fab.setOnClickListener(view1 ->
                            ((ChatFragment) viewPagerAdapter.fragments.get(position))
                                    .scrollToBottom());
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        chatTitle = (TextView) findViewById(R.id.title);

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        fab.hide();
    }

    @Override
    protected void onStart() {
        if (client != null) {
            client.attachUi(this);
        }
        super.onStart();

        Optional.ofNullable(getIntent().getStringExtra(CHANNEL))
                .executeIfPresent(s -> viewPagerAdapter.setChannel(s));
        setIntent(new Intent());

        if (lastPosition != null) {
            viewPagerAdapter.setChannel(lastPosition);
        }
    }

    @Override
    protected void onStop() {
        if (client != null) {
            client.detachUi();
        }
        super.onStop();
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
        emotesLl = (LinearLayout) findViewById(R.id.emotes_ll);
        content = (LinearLayout) findViewById(R.id.content);
    }


    private void setObserverListener() {
        // Determine keyboard height
        LinearLayout ll = (LinearLayout) findViewById(R.id.root_view);
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        int defaultHeight = point.y / 3;
        keyboardHeight = defaultHeight;
        ll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            ll.getWindowVisibleDisplayFrame(r);

            int screenHeight = ll.getRootView().getHeight();
            int heightDifference = screenHeight - (r.bottom - r.top);
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                heightDifference -= getResources().getDimensionPixelSize(resourceId);
            }
            if (heightDifference > defaultHeight) {
                keyboardHeight = heightDifference;
            }
//            Log.d("Keyboard Size", "Size: " + heightDifference);
        });

    }

    public void sendMessage(String message) {
        getChannels().get(viewPager.getCurrentItem()).send(message);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        lastPosition = savedInstanceState.getCharSequence("Last position");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("Id", id);
        outState.putCharSequence("Last position", lastPosition);
    }

    private boolean isEmotesShowing = false;

    public void onEmotesShowClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        if (isEmotesShowing()) {
            closeEmotes();
            return;
        }
        if (client == null || getChannels() == null) {
            Toast.makeText(this, R.string.client_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        emotesViewPager.setAdapter(new EmotesViewPagerAdapter(getSupportFragmentManager()));
        emotesLl.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeight));
        isEmotesShowing = true;
        emotesViewPager.setCurrentItem(1);
        emotesViewPager.setOffscreenPageLimit(3);
        // TRANSLATE UP
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            content.animate().translationY(-keyboardHeight).setInterpolator(new LinearInterpolator()).start();
            emotesLl.animate().translationY(-keyboardHeight).setInterpolator(new LinearInterpolator()).start();
        }, 100);
    }


    private void closeEmotes() {
        //TRANSLATE DOWN
        emotesLl.animate().translationY(emotesLl.getHeight()).setInterpolator(new LinearInterpolator()).start();
        content.animate().translationY(0).setInterpolator(new LinearInterpolator()).start();
        isEmotesShowing = false;
    }

    private boolean isEmotesShowing() {
        return isEmotesShowing;
    }


    @SuppressWarnings("unused")
    public void onEmoteTabClick(View view) {
        switch (view.getId()) {
            case R.id.recent_emotes:
                emotesViewPager.setCurrentItem(0);
                break;
            case R.id.bttv_emotes:
                emotesViewPager.setCurrentItem(1);
                break;
            case R.id.twitch_emotes:
                emotesViewPager.setCurrentItem(2);
                break;
            case R.id.ffz_emotes:
                emotesViewPager.setCurrentItem(3);
                break;

        }
    }

    public void onClearClick(View view) {
        typeMessage.setText("");
    }

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
            case TEXT_SIZE_KEY:
                recreate();
                break;
        }
    }

    class EmotesViewPagerAdapter extends FragmentPagerAdapter {

        private final String[] type = new String[]{"recent", "twitch", "bttv", "ffz"};

        EmotesViewPagerAdapter(FragmentManager supportFragmentManager) {
            super(supportFragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            return EmoteScrollViewFragment.newInstance(type[position % type.length]);
        }

        @Override
        public int getCount() {
            return type.length;
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
        super.onBackPressed();
    }

    @UiThread
    public void onConnected(@NonNull final Client client) {
        ChatActivity.this.client = client;
        client.attachUi(this);
        viewPagerAdapter.setChannel(lastPosition);
    }

    @Override
    @UiThread
    public void onChannelChange() {
        getChannels().clear();
        if (client != null) {
            getChannels().addAll(client.getChannelList());
        }
        viewPagerAdapter.notifyDataSetChanged();

        if (getChannels().size() > 1) loadMenu();
    }

    @Override
    @UiThread
    public void onChannelJoined(Channel channel) {
        viewPagerAdapter.add(channel);

        if (getChannels().size() > 1) loadMenu();
    }

    public void loadMenu() {
        Menu menu = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
        menu.removeGroup(0);
        List<Channel> channels = getChannels();
        for (int i = 0; i < channels.size(); i++) {
            menu.add(0, i, Menu.CATEGORY_CONTAINER, getChannelName(channels.get(i)))
                    .setIcon(i == 0 ? android.R.drawable.ic_dialog_info : android.R.drawable.stat_notify_chat)
                    .setCheckable(true);
        }
//        viewPager.setCurrentItem(1);
//        menu.getItem(1).setChecked(true);
    }

    public List<Channel> getChannels() {
        return viewPagerAdapter.getChannels();
    }

    private String getChannelName(Channel channel) {
        String name = channel.getName();
        return name.charAt(0) == '#' ? Character.toUpperCase(name.charAt(1)) + name.substring(2) : name;
    }

    private final class ChatFragmentPagerAdapter extends FragmentPagerAdapter {
        private List<Channel> channels = new ArrayList<>();
        private SparseArray<Fragment> fragments = new SparseArray<>();

        ChatFragmentPagerAdapter(FragmentManager supportFragmentManager) {
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

        @Override
        public long getItemId(int position) {
            return channels.get(position).hashCode();
        }

        //TODO: спарс может заменить как нить?
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = ((Fragment) super.instantiateItem(container, position));
            fragments.put(position, fragment);
            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }

        public void add(Channel channel) {
            //TODO: o(n) complexity
            String name = channel.getName();
            int contains = -1;
            for (int i = 0; i < channels.size(); i++) {
                String lul = channels.get(i).getName();
                if (lul.equals(name)) {
                    contains = i;
                    break;
                }
            }
            if (contains >= 0) {
                channels.set(contains, channel);
            } else {
                channels.add(channel);
            }
            notifyDataSetChanged();
        }

        public void add(int position, Channel channel) {
            channels.add(position, channel);
            notifyDataSetChanged();
        }

        public void setChannel(CharSequence channelName) {
            int position = -1;
            for (int i = 0; i < channels.size(); i++) {
                if (channels.get(i).getName().equals(channelName)) {
                    position = i;
                }
            }
            if (position >= 0) {
                viewPager.setCurrentItem(position);
            }
        }

        List<Channel> getChannels() {
            return channels;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 1, Menu.CATEGORY_CONTAINER, getResources().getString(R.string.settings)).setOnMenuItemClickListener(m -> {
            startActivity(new Intent(this, PreferenceActivity.class));
            return false;
        });
        menu.add(Menu.NONE, 2, Menu.CATEGORY_CONTAINER, getResources().getString(R.string.disconnect)).setOnMenuItemClickListener(m -> {
            ClientService.stopClient(id);
            finish();
            return false;
        });
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
