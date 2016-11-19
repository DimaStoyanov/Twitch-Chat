package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;

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
    private ScrollView emotesScroll;
    private LinearLayout emotesLl;
    private boolean spamMode = false;

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

            int screenHeight = ll.getRootView().getHeight();
            int heightDifference = screenHeight - (r.bottom - r.top);
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                heightDifference -= getResources().getDimensionPixelSize(resourceId);
            }
            if (heightDifference > 400) {
                keyboardHeight = heightDifference;
            }

            Log.d("Keyboard Size", "Size: " + heightDifference);
        });


        typeMessage.setOnTouchListener(((view, motionEvent) -> {
            if (emotesScroll.getVisibility() == View.VISIBLE)
                closeEmotes();
            return false;
        }));

        findViewById(R.id.send).setOnClickListener(v -> {
            Log.d(TAG, String.valueOf(viewPager.getCurrentItem()));
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            viewPagerAdapter.channels.get(viewPager.getCurrentItem())
                    .send(typeMessage.getText().toString());
            if (!spamMode) typeMessage.setText("");
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
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        if (client != null) {
            client.attachUi(this);
            onChannelChange();
        }
    }


    private void initView() {
        setContentView(R.layout.activity_chat);
        typeMessage = (EditText) findViewById(R.id.text_message);
        emotesScroll = (ScrollView) findViewById(R.id.emotes_scroll);
        emotesLl = (LinearLayout) findViewById(R.id.emotes_ll);
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
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
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
                emote.setHapticFeedbackEnabled(true);
                emote.setOnTouchListener(new OnEmotesTouchListener((String) keyset[i], emote));

                DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                        .setUri(BttvEmotes.getEmoteUrlByCode(String.valueOf(keyset[i++]), channel))
                        .setControllerListener(new BaseControllerListener<ImageInfo>() {
                            @Override
                            public void onFinalImageSet(String id, @javax.annotation.Nullable ImageInfo imageInfo, @javax.annotation.Nullable Animatable animatable) {
                                super.onFinalImageSet(id, imageInfo, animatable);
                                if (animatable != null) {
                                    animatable.start();
                                }
                            }
                        })
                        .build();
                emote.setController(draweeController);

                row.addView(emote);
            }
            emotesLl.addView(row);
        }
        emotesScroll.setVisibility(View.VISIBLE);
        emotesScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, keyboardHeight));

    }

    private class OnEmotesTouchListener implements View.OnTouchListener {
        final String code;
        private Handler handler;
        private int duration, totalDuration;
        private View view;

        OnEmotesTouchListener(String code, View view) {
            this.code = code;
            duration = 500;
            totalDuration = 0;
            this.view = view;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (handler != null) return true;
                    handler = new Handler();
                    totalDuration = duration;
                    handler.postDelayed(mAction, duration);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (handler == null) return true;
                    handler.removeCallbacks(mAction);
                    handler = null;
                    break;
            }
            return false;
        }

        Runnable mAction = new Runnable() {
            @Override
            public void run() {
                if (totalDuration == 500) {
                    totalDuration += 500;
                    handler.postDelayed(this, duration);
                    return;
                }
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                typeMessage.append(code + " ");
                if (totalDuration == 2000)
                    duration = 250;
                if (totalDuration == 3500)
                    duration = 100;
                totalDuration += duration;
                handler.postDelayed(this, duration);
            }
        };
    }

    private class OnEmotesClickListener implements View.OnClickListener {
        final String code;

        OnEmotesClickListener(String code) {
            this.code = code;
        }

        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            typeMessage.append(code + " ");
        }
    }

    private void closeEmotes() {
        emotesLl.removeAllViews();
        emotesScroll.setVisibility(View.GONE);
    }

    private boolean isEmotesShowing() {
        return emotesScroll.getVisibility() == View.VISIBLE;
    }

    public void onClearClick(View view) {
        typeMessage.setText("");
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, Menu.CATEGORY_ALTERNATIVE, "Clear type message after send")
                .setCheckable(true)
                .setChecked(true)
                .setOnMenuItemClickListener(menuItem -> {
                    menuItem.setChecked(!menuItem.isChecked());
                    spamMode = !menuItem.isChecked();
                    return false;
                });
        return true;
    }
}
