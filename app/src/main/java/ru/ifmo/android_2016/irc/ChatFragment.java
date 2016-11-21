package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.ifmo.android_2016.irc.client.Channel;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;

public class ChatFragment extends Fragment implements Channel.Callback {
    private static final String CHANNEL_NAME = "param1";
    private static final String TAG = ChatFragment.class.getSimpleName();

    private String channelName;
    private Channel channel;
    private RecyclerView recyclerView;
    FloatingActionButton fab;
    private MessageAdapter adapter;
    private long serverId;
    private float startEventY;
    private boolean autoScroll = false, lastActionUp;
    private LinearLayoutManager layoutManager;
    private ChatActivity activity;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(long serverId, String channel) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putLong(SERVER_ID, serverId);
        args.putString(CHANNEL_NAME, channel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            serverId = getArguments().getLong(SERVER_ID);
            channelName = getArguments().getString(CHANNEL_NAME);
        }
        channel = ClientService.getClient(serverId).getChannels().get(channelName);
        channel.attachUi(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (ChatActivity) context;
    }

    @Override
    public void onDetach() {
        channel.detachUi();
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(CHANNEL_NAME, channelName);
        outState.putLong(SERVER_ID, serverId);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        recyclerView = (RecyclerView) view.findViewById(R.id.messages);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new MessageAdapter(channel.getMessages()));
        fab = activity.fab;
//        fab.setOnClickListener(view1 -> {
//            recyclerView.post(() -> {
//                autoScroll = true;
//                layoutManager.scrollToPosition(adapter.getItemCount() - 1);
//            });
//            fab.hide();
//        });
//        recyclerView.setOnTouchListener((view1, motionEvent) -> {
//            switch (motionEvent.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    startEventY = motionEvent.getY();
//                    break;
//                case MotionEvent.ACTION_UP:
//                    lastActionUp = motionEvent.getY() - startEventY > 0;
//                    Log.d(TAG, "Motion event diff = " + (startEventY - motionEvent.getY()));
//                    if (startEventY - motionEvent.getY() > 30) {
//                        // Action Down
//                        fab.show();
//                    } else if (motionEvent.getY() - startEventY > 50) {
//                        autoScroll = false;
//                        // Action Up
//                    }
//            }
//            return false;
//        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (fab.getVisibility() == View.VISIBLE &&
                        (recyclerView.getAdapter().getItemCount() - 1 - layoutManager.findLastVisibleItemPosition()) <= 5) {
//                    fab.hide();
                    if (!lastActionUp)
                        autoScroll = true;
                }
            }
        });
        recyclerView.setItemAnimator(null);
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    @Override
    @UiThread
    public void onMessageReceived() {
        if (adapter != null) {
            adapter.notifyItemChanged(adapter.messages.size());
        }
        if (autoScroll) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }

    @Override
    @UiThread
    public void onMessagesRemoved(int start, int count) {
        adapter.notifyItemRangeRemoved(start, count);
    }


    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        final private List<CharSequence> messages;

        private MessageAdapter(List<CharSequence> list) {
            messages = list;
            //setHasStableIds(true);
        }

        @Override
        public MessageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DraweeTextView view = new DraweeTextView(getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MessageAdapter.ViewHolder holder, int position) {
            holder.itemView.setText(messages.get(position));
            //holder.itemView.setBackgroundColor(Color.argb(255, 180, 0, 0));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

//        @Override
//        public long getItemId(int position) {
//            return position;
//        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private DraweeTextView itemView;

            ViewHolder(View itemView) {
                super(itemView);
                this.itemView = (DraweeTextView) itemView;
                this.itemView.setAutoLinkMask(Linkify.ALL);
                this.itemView.setLinksClickable(true);
            }
        }
    }

}
