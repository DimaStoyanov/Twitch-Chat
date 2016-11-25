package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.Space;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotes;
import ru.ifmo.android_2016.irc.api.twitch.TwitchEmotes;
import ru.ifmo.android_2016.irc.utils.Log;


public class EmoteScrollViewFragment extends Fragment {

    String currentEmotes;
    ChatActivity activity;
    ScrollView scrollView;

    final String TAG = EmoteScrollViewFragment.class.getSimpleName();
    final static String EMOTE_LIST = "emote_list";


    public EmoteScrollViewFragment() {
    }


    public static EmoteScrollViewFragment newInstance(String emoteList) {
        EmoteScrollViewFragment fragment = new EmoteScrollViewFragment();
        Bundle args = new Bundle();
        args.putString(EMOTE_LIST, emoteList);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentEmotes = getArguments().getString(EMOTE_LIST);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emote_scroll_view, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        scrollView = (ScrollView) getView().findViewById(R.id.emotes_scroll);
        String channel = activity.client.getChannelList().get(activity.viewPager.getCurrentItem()).getName();

        Point point = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(point);
        int columns = point.x / 120;

        Log.d(TAG, channel);
        Object[] keyset = getEmotesKeyset(channel);
        Log.d(TAG, "Keyset length " + keyset.length);
        LinearLayout emotesLl = new LinearLayout(activity);
        emotesLl.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        emotesLl.setOrientation(LinearLayout.VERTICAL);
        int i = 0;
        while (i < keyset.length) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            int j = 0;
            while (i < keyset.length && j++ < columns) {
                SimpleDraweeView emote = new SimpleDraweeView(activity);
                LinearLayout.LayoutParams emoteParams = new LinearLayout.LayoutParams(100, 100);
                emoteParams.setMargins(10, 10, 10, 10);
                emoteParams.weight = 1;
                emote.setLayoutParams(emoteParams);
                emote.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_INSIDE);
                emote.setOnClickListener(new OnEmoteClickListener((String) keyset[i]));
                emote.setHapticFeedbackEnabled(true);
                emote.setOnTouchListener(new OnEmoteTouchListener((String) keyset[i], emote));
                DraweeController draweeController = Fresco.newDraweeControllerBuilder()
                        .setUri(getImageUri((String) keyset[i++], channel))
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
            // Symmetry for last row (if emotes in last row less than at all columns)
            while (j++ < columns) {
                Space space = new Space(activity);
                LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(100, 100);
                spaceParams.setMargins(10, 10, 10, 10);
                spaceParams.weight = 1;
                space.setLayoutParams(spaceParams);
                row.addView(space);
            }
            emotesLl.addView(row);
        }
        scrollView.addView(emotesLl);
    }

    private Object[] getEmotesKeyset(String channel) {
        switch (currentEmotes) {
            case "twitch":
                return TwitchEmotes.getGlobalEmotesList().toArray();
            case "bttv":
                return BttvEmotes.getChannelEmotesKey(channel);
            default:
                return null;
        }
    }

    private Uri getImageUri(String code, String channel) {
        switch (currentEmotes) {
            case "twitch":
                return Uri.parse(TwitchEmotes.getEmoteUrlByCode(code));
            case "bttv":
                return Uri.parse(BttvEmotes.getEmoteUrlByCode(String.valueOf(code), channel));
            default:
                return null;
        }
    }

    private class OnEmoteClickListener implements View.OnClickListener {
        private final String code;

        OnEmoteClickListener(String code) {
            this.code = code;
        }


        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            activity.typeMessage.append(code + " ");
        }
    }

    private class OnEmoteTouchListener implements View.OnTouchListener {
        private Handler handler;
        private int duration, totalDuration;
        private final String code;
        private View view;

        OnEmoteTouchListener(String code, View view) {
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
                activity.typeMessage.append(code + " ");
                if (totalDuration == 2000)
                    duration = 250;
                if (totalDuration == 3500)
                    duration = 100;
                totalDuration += duration;
                handler.postDelayed(this, duration);
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (ChatActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


}
