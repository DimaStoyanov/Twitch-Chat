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
    private boolean autoScroll = false, isScrolling = false;
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
        fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(view1 -> {
            recyclerView.post(() -> {
                recyclerView.scrollToPosition(adapter.getItemCount());
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            });
            // TODO !!! scroll&snap toolbar
            fab.setVisibility(View.GONE);
        });

        //TODO: autoScroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastState;
            int lastDirection;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                lastDirection = dy;
                isScrolling = true;
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                autoScroll = lastState == 2 && newState == 0 && (lastDirection >= 0);
                lastState = newState;
                if (fab.getVisibility() == View.VISIBLE &&
                        (recyclerView.getAdapter().getItemCount() - 1 - layoutManager.findLastVisibleItemPosition()) <= 5)
                    fab.setVisibility(View.GONE);
                isScrolling = false;
            }
        });
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    @Override
    public void runOnUiThread(Runnable run) {
        getActivity().runOnUiThread(run);
    }

    @Override
    @UiThread
    public void onMessageReceived() {
        if (adapter != null) {
            adapter.notifyItemChanged(adapter.messages.size());
            adapter.tryToClearOldMessages();
        }
        if (!isScrolling && (recyclerView.getAdapter().getItemCount() - 1) - layoutManager.findLastVisibleItemPosition() >= 10)
            fab.setVisibility(View.VISIBLE);
        if (autoScroll) {
            recyclerView.post(() -> {
                recyclerView.scrollToPosition(adapter.getItemCount());
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            });
        }
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

        public void tryToClearOldMessages() {
            if (messages.size() > 300) {
                synchronized (messages) {
                    messages.subList(0, 199).clear();
                }
                notifyItemRangeRemoved(0, 200);
            }
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
