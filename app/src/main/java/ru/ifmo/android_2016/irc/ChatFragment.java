package ru.ifmo.android_2016.irc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.ifmo.android_2016.irc.client.Channel;
import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.MessageText;
import ru.ifmo.android_2016.irc.constant.PreferencesConstant;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;

public class ChatFragment extends Fragment implements Channel.Callback {
    private static final String CHANNEL_NAME = "param1";
    private static final String TAG = ChatFragment.class.getSimpleName();

    private String channelName;
    private Channel channel;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private MessageAdapter adapter;
    private long serverId;
    private boolean autoScroll = true;
    private LinearLayoutManager layoutManager;
    private ChatActivity activity;
    private float textSize = 14;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "On create view");
        View root = inflater.inflate(R.layout.fragment_chat, container, false);
        textSize = Float.parseFloat(activity.prefs.getString(PreferencesConstant.TEXT_SIZE_KEY, "14"));
        recyclerView = (RecyclerView) root.findViewById(R.id.messages);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new MessageAdapter(channel.getMessages()));
        fab = activity.fab;

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (layoutManager.findLastVisibleItemPosition() == adapter.messages.size() - 1) {
                    autoScroll = true;
                    fab.hide();
                } else {
                    autoScroll = false;
                }
            }
        });
        recyclerView.setItemAnimator(null);
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (ChatActivity) getActivity();
    }

    @Override
    public void onStart() {
        channel.attachUi(this);
        adapter.notifyDataSetChanged();
        super.onStart();
    }

    @Override
    public void onStop() {
        channel.detachUi();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(CHANNEL_NAME, channelName);
        outState.putLong(SERVER_ID, serverId);
    }

    @Override
    @UiThread
    public void onMessageReceived() {
        if (adapter != null) {
            adapter.notifyItemChanged(adapter.messages.size());
        }
        if (autoScroll && recyclerView != null) {
            //noinspection ConstantConditions
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
//            recyclerView.post(() -> );
        }
    }

    @Override
    @UiThread
    public void onMessagesRemoved(int start, int count) {
        adapter.notifyItemRangeRemoved(start, count);
    }

    public void scrollToBottom() {
        recyclerView.post(() -> {
            autoScroll = true;
            layoutManager.scrollToPosition(adapter.getItemCount() - 1);
        });
        fab.hide();
    }


    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        final private List<MessageText> messages;

        private MessageAdapter(List<MessageText> list) {
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
            holder.setText(position);
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
                this.itemView.setAutoLinkMask(Linkify.WEB_URLS);
                this.itemView.setLinksClickable(true);
                this.itemView.setTextSize(textSize);
//                this.itemView.setOnTouchListener((view, motionEvent) -> {
//                    Log.d(TAG, motionEvent.toString());
//                    switch (motionEvent.getAction()) {
//                        case MotionEvent.ACTION_DOWN:
//                            startY = motionEvent.getY();
//                            startTime = System.currentTimeMillis();
//                            break;
//                        case MotionEvent.ACTION_MOVE:
//                            if (System.currentTimeMillis() - startTime > 100 && Math.abs(motionEvent.getY() - startY) < 5) {
//                                this.itemView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
//                                break;
//                            }
//                        case MotionEvent.ACTION_CANCEL:
//                        case MotionEvent.ACTION_UP:
//                            this.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
//                    }
//                    return false;
//                });
                registerForContextMenu(this.itemView);
            }

            void setText(int position) {
                itemView.setMessage(messages.get(position));
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, Menu.CATEGORY_CONTAINER, "Copy to clipboard this message")
                .setOnMenuItemClickListener(m -> {
                    DraweeTextView textView = (DraweeTextView) v;
                    ClipboardManager clipboard =
                            (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Copied text", textView.getMessage().getText());
                    clipboard.setPrimaryClip(clip);
                    return false;
                });
        menu.add(0, 1, Menu.CATEGORY_CONTAINER, "Copy this message")
                .setOnMenuItemClickListener(m -> {
                    DraweeTextView textView = (DraweeTextView) v;
                    activity.typeMessage.setText(textView.getMessage().isColored() ? "/me " : "");
                    activity.typeMessage.append(textView.getMessage().getText());
                    return false;
                });
        menu.add(0, 2, Menu.CATEGORY_CONTAINER, "Answer").setOnMenuItemClickListener(m -> {
            DraweeTextView textView = (DraweeTextView) v;
            CharSequence message = "@" + textView.getMessage().getSender() + ", ";
            activity.typeMessage.setText(message);
            activity.typeMessage.setSelection(message.length());
            return false;
        });
    }


}
