package ru.ifmo.android_2016.irc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.ifmo.android_2016.irc.client.Message;
import ru.ifmo.android_2016.irc.client.MessageStorage;
import ru.ifmo.android_2016.irc.client.TwitchMessage;
import ru.ifmo.android_2016.irc.drawee.DraweeTextView;
import ru.ifmo.android_2016.irc.utils.TextUtils;

public class ChatFragment extends Fragment {
    private static final String MESSAGE_STORAGE_ID = "param1";
    private static final String TAG = ChatFragment.class.getSimpleName();

    private Long id;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private LocalBroadcastManager lbm;

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance(long id) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putLong(MESSAGE_STORAGE_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getLong(MESSAGE_STORAGE_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onAttach(Context context) {
        lbm = LocalBroadcastManager.getInstance(context);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        lbm.unregisterReceiver(messageReceiver);
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putLong(MESSAGE_STORAGE_ID, id);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        lbm.registerReceiver(messageReceiver, new IntentFilter("new-message"));

        View view = getView();
        recyclerView = (RecyclerView) view.findViewById(R.id.messages);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter = new MessageAdapter(MessageStorage.getInstance()
                        .<SpannableStringBuilder>get(id)));

        //TODO: autoScroll
//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            int lastState;
//            int lastDirection;
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                //Log.d(TAG, dx + ", " + dy);
//                lastDirection = dy;
//            }
//
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                //Log.d(TAG, String.valueOf(newState));
//                autoScroll = lastState == 2 && newState == 0 && (lastDirection >= 0);
//                lastState = newState;
//            }
//        });
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new AsyncTask<Message, Void, SpannableStringBuilder>() {
                @Override
                protected SpannableStringBuilder doInBackground(Message... params) {
                    adapter.messages
                            .add((SpannableStringBuilder) TextUtils.buildTextDraweeView((TwitchMessage)
                                    intent.getParcelableExtra(Message.class.getCanonicalName())));
                    return null;
                }

                @Override
                protected void onPostExecute(SpannableStringBuilder spannableStringBuilder) {
                    adapter.notifyItemInserted(adapter.getItemCount());
                    super.onPostExecute(spannableStringBuilder);
                }
            }.execute();
        }
    };


    class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
        final private List<SpannableStringBuilder> messages;

        private MessageAdapter(List<SpannableStringBuilder> list) {
            messages = list;
            setHasStableIds(true);
        }

        @Override
        public MessageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new DraweeTextView(getActivity()));
        }

        @Override
        public void onBindViewHolder(MessageAdapter.ViewHolder holder, int position) {
            holder.itemView.setText(messages.get(position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private DraweeTextView itemView;

            ViewHolder(View itemView) {
                super(itemView);
                this.itemView = (DraweeTextView) itemView;
            }
        }
    }
}
