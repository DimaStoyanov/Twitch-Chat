package ru.ifmo.android_2016.irc;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Collection;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.loader.LoadResult;
import ru.ifmo.android_2016.irc.loader.ResultType;
import ru.ifmo.android_2016.irc.loader.TwitchUserNickLoader;
import ru.ifmo.android_2016.irc.utils.IOUtils;

import static ru.ifmo.android_2016.irc.client.ClientService.GET_SERVER_LIST;
import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;
import static ru.ifmo.android_2016.irc.client.ClientService.START_SERVICE;
import static ru.ifmo.android_2016.irc.client.ClientService.STOP_SERVICE;

public class ChannelsListActivity extends AppCompatActivity {

    private LinearLayout ll;
    private boolean updateDataFromCache = false;
    //private FileManager fileManager;
    private ProgressBar pb;
    public final String TAG = ChannelsListActivity.class.getSimpleName();
    private Context context;
    private LocalBroadcastManager lbm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels_list);
        ll = (LinearLayout) findViewById(R.id.channels_ll);
        pb = (ProgressBar) findViewById(R.id.pbar);
        Button button = (Button) findViewById(R.id.twitch_login);
        button.getBackground().setColorFilter(0xFF6441A5, PorterDuff.Mode.MULTIPLY);
        context = this;
        Log.d(TAG, "On create");
        startService(new Intent(this, ClientService.class).setAction(START_SERVICE));
        lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateChannelList();
            }
        }, new IntentFilter(ServerList.class.getCanonicalName()));

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "On pause");
        updateDataFromCache = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "On start");
        if (!updateDataFromCache) {
            ll.removeAllViews();
            startService(new Intent(this, ClientService.class).setAction(GET_SERVER_LIST));
            updateDataFromCache = true;
        }

    }


    @UiThread
    private void updateChannelList() {
        pb.setVisibility(View.GONE);
        ll.removeAllViews();
        Collection<ClientSettings> data = ServerList.getInstance().values();
        View item;
        Log.d(TAG, "LOGIN DATA:");
        for (ClientSettings aData : data) {
            Log.d(TAG, aData.toString());
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ClientSettings aData : data) {
            item = inflater.inflate(R.layout.chanel_item, null);
            Button selectChannel = (Button) item.findViewById(R.id.selected_channel);

            selectChannel.setText(aData.getName());
            selectChannel.setOnClickListener(new OnSelectChannelListener(aData));
            (item.findViewById(R.id.delete_channel)).setOnClickListener(new OnDeleteChannelListener(aData));
            (item.findViewById(R.id.edit_channel)).setOnClickListener(new OnEditChannelListener(aData));
            ll.addView(item);
        }
    }

    @UiThread
    private class OnSelectChannelListener implements View.OnClickListener {

        private final long id;

        OnSelectChannelListener(ClientSettings data) {
            this.id = data.getId();
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On select channel click");
            startActivity(new Intent(ChannelsListActivity.this, ChatActivity.class)
                    .putExtra(SERVER_ID, id));
        }
    }

    @UiThread
    private class OnDeleteChannelListener implements View.OnClickListener {

        private long id;

        OnDeleteChannelListener(ClientSettings data) {
            this.id = data.getId();
        }

        @Override
        public void onClick(View v) {
            ((ViewGroup) (v.getParent()).getParent()).removeView((View) v.getParent());
            AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    ServerList.getInstance().remove(id);
                    return null;
                }
            };
            deleteTask.execute();
            Log.d(TAG, "Delete channel");
        }
    }

    @UiThread
    private class OnEditChannelListener implements View.OnClickListener {
        private ClientSettings data;

        OnEditChannelListener(ClientSettings data) {
            this.data = data;
        }

        @Override
        public void onClick(final View edView) {
            Log.d(TAG, "On edit channel click");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton("Ok", null)
                    .setNegativeButton("Cancel", null)
                    .setView(R.layout.dialog_login);
            final AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> {
                final EditText name = (EditText) dialog.findViewById(R.id.name);
                final EditText server = (EditText) dialog.findViewById(R.id.server);
                final EditText port = (EditText) dialog.findViewById(R.id.port);
                final EditText username = (EditText) dialog.findViewById(R.id.username);
                final EditText password = (EditText) dialog.findViewById(R.id.password);
                final EditText channel = (EditText) dialog.findViewById(R.id.channel);
                final CheckBox ssl = (CheckBox) dialog.findViewById(R.id.use_ssl);
                name.setText(data.getName());
                server.setText(data.getAddress());
                port.setText(Integer.toString(data.getPort()));
                username.setText(data.getUsername());
                password.setText(data.getPassword());
                channel.setText(data.getChannel());
                ssl.setChecked(data.isSsl());
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> dialog.dismiss());
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {

                    Toast toast = new Toast(context);
                    if (!TextUtils.isEmpty(server.getText()) &&
                            !TextUtils.isEmpty(port.getText())
                           /* && !TextUtils.isEmpty(username.getText())*/
                            && !TextUtils.isEmpty(password.getText())
                            && !TextUtils.isEmpty(channel.getText())) {

                        dialog.cancel();
                        new EditChannelTask().execute(data.getId(), buildClientSettings(name.getText().toString(), server.getText().toString(),
                                port.getText().toString(), username.getText().toString(), password.getText().toString(),
                                channel.getText().toString(), String.valueOf(ssl.isChecked())));
                    } else {
                        toast.cancel();
                        toast = Toast.makeText(context, "Fill the fields correctly", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            });
            dialog.show();
        }
    }


    class EditChannelTask extends AsyncTask<Object, Void, Void> {
        @Override
        @WorkerThread
        protected Void doInBackground(Object... params) {
            ServerList.getInstance().put((Long) params[0], (ClientSettings) params[1]);
            return null;
        }

        @Override
        @UiThread
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateChannelList();
        }
    }

    @UiThread
    public void onAddChannelClick(View v) {
        Log.d(TAG, "On add channel click");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .setView(R.layout.dialog_login)
                .setTitle("Login data")
                .setIcon(R.mipmap.ic_launcher);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            final EditText name = (EditText) dialog.findViewById(R.id.name);
            final EditText server = (EditText) dialog.findViewById(R.id.server);
            final EditText port = (EditText) dialog.findViewById(R.id.port);
            final EditText username = (EditText) dialog.findViewById(R.id.username);
            final EditText password = (EditText) dialog.findViewById(R.id.password);
            final EditText channel = (EditText) dialog.findViewById(R.id.channel);
            final CheckBox ssl = (CheckBox) dialog.findViewById(R.id.use_ssl);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> dialog.dismiss());
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {

                Toast toast = new Toast(context);
                if (!TextUtils.isEmpty(server.getText()) &&
                        !TextUtils.isEmpty(port.getText())
                        /*&& !TextUtils.isEmpty(username.getText())*/
                        && !TextUtils.isEmpty(password.getText())
                        && !TextUtils.isEmpty(channel.getText())) {

                    long id = ServerList.getInstance().add(new ClientSettings()
                            .setName(name.getText().toString())
                            .setAddress(server.getText().toString())
                            .setPort(Integer.parseInt(port.getText().toString()))
                            .setUsername(username.getText().toString())
                            .setPassword(password.getText().toString())
                            .setChannel(channel.getText().toString())
                            .setSsl(ssl != null && ssl.isChecked()));

                    dialog.cancel();

                    startActivity(new Intent(context, ChatActivity.class)
                            .putExtra(SERVER_ID, id));
                } else {
                    toast.cancel();
                    toast = Toast.makeText(context, "Fill the fields correctly", Toast.LENGTH_SHORT);
                    toast.show();
                }
            });
            final ImageView server_status = (ImageView) dialog.findViewById(R.id.server_status);
            final ImageView port_status = (ImageView) dialog.findViewById(R.id.port_status);
            final ImageView nick_status = (ImageView) dialog.findViewById(R.id.username_status);
            final ImageView password_status = (ImageView) dialog.findViewById(R.id.password_status);
            final ImageView channel_status = (ImageView) dialog.findViewById(R.id.channel_status);


            server.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    server_status.setImageResource(Patterns.WEB_URL.matcher(editable).matches() ?
                            R.drawable.ok : R.drawable.not_ok);
                }

            });

            port.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    port_status.setImageResource(Pattern.compile("^[0-9]+$").matcher(editable).matches() ?
                            R.drawable.ok : R.drawable.not_ok);
                }
            });

            username.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    nick_status.setImageResource(Pattern.compile("^[_a-z-A-Z0-9]+$").matcher(editable).matches() ?
                            R.drawable.ok : R.drawable.not_ok);
                }
            });

            password.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    password_status.setImageResource(Pattern.compile("^$").matcher(editable).matches() ?
                            R.drawable.not_ok : R.drawable.ok);

                }
            });

            channel.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    channel_status.setImageResource(Pattern.compile("^#[_a-z-A-Z0-9]+$").matcher(editable).matches() ?
                            R.drawable.ok : R.drawable.not_ok);
                }
            });
        });
        dialog.show();
    }

    @UiThread
    private ClientSettings buildClientSettings(String... args) {
        return new ClientSettings()
                .setName(TextUtils.isEmpty(args[0]) ? args[5] : args[0])
                .setAddress(args[1])
                .setPort(Integer.parseInt(args[2]))
                .setUsername(args[3])
                .setPassword(args[4])
                .setChannel(args[5])
                .setSsl(Boolean.parseBoolean(args[6]));
    }


    public void onTwitchLoginClick(View v) {
        startActivityForResult(new Intent(this, TwitchLoginActivity.class), 1);
    }

    @UiThread
    private boolean checkInternetConnection(boolean flag) {
        if (!flag) return false;
        if (!IOUtils.isConnectionAvailable(this, false)) {
            showNoInternetDialog();
        } else
            return true;
        return false;
    }

    @UiThread
    private void showNoInternetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No internet connection")
                .setNegativeButton("Retry", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    checkInternetConnection(true);
                }).setNeutralButton("Exit", (dialogInterface, i) -> finish()).setPositiveButton("OK", (dialogInterface, i) -> {
            dialogInterface.cancel();
            checkInternetConnection(false);

        });
        builder.create().show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 200) {
            getSupportLoaderManager().initLoader(10, null, new NickLoaderCallback(data.getStringExtra("ru.ifmo.android_2016.irc.Token")));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class NickLoaderCallback implements LoaderManager.LoaderCallbacks<LoadResult<String>> {
        private final String token;

        NickLoaderCallback(String access_token) {
            token = access_token;
        }

        @Override
        public Loader<LoadResult<String>> onCreateLoader(int id, Bundle args) {
            return new TwitchUserNickLoader(ChannelsListActivity.this, token);
        }

        @Override
        public void onLoadFinished(Loader<LoadResult<String>> loader, final LoadResult<String> result) {
            Log.d(TAG, "Load login data finished");
            AlertDialog.Builder builder;
            final AlertDialog alert;
            if (result.resultType == ResultType.OK) {
                builder = new AlertDialog.Builder(ChannelsListActivity.this)
                        .setCancelable(true)
                        .setTitle("Type channel")
                        .setMessage("You can also type several channels, separated by a comma")
                        .setView(R.layout.dialog_login_twitch)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Cancel", null);

                alert = builder.create();


                alert.setOnShowListener(dialogInterface -> {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        Toast toast = new Toast(ChannelsListActivity.this);

                        @Override
                        public void onClick(View view) {
                            final EditText name = (EditText) alert.findViewById(R.id.name);
                            final EditText channel = (EditText) alert.findViewById(R.id.channel);
                            final CheckBox ssl = (CheckBox) alert.findViewById(R.id.use_ssl);
                            if (!TextUtils.isEmpty(channel.getText())) {
                                long id = ServerList.getInstance()
                                        .add(ClientSettings.getTwitchSettings(token, ssl.isChecked())
                                                .setName(TextUtils.isEmpty(name.getText()) ?
                                                        channel.getText().toString() :
                                                        name.getText().toString())
                                                .setNicks(result.data)
                                                .setChannel(channel.getText().toString()));

                                alert.dismiss();

                                startActivity(new Intent(context, ChatActivity.class)
                                        .putExtra(SERVER_ID, id));
                            } else {
                                toast.cancel();
                                toast = Toast.makeText(ChannelsListActivity.this, "Empty channel", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
                    alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> alert.dismiss());
                });
                pb.setVisibility(View.GONE);
                alert.show();
                Log.d(TAG, "Twitch Authorized successfully");
            } else {
                Toast.makeText(ChannelsListActivity.this, "error request api", Toast.LENGTH_SHORT).show();
            }
            pb.setVisibility(View.GONE);
            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<LoadResult<String>> loader) {
        }

    }

    @Override
    public void finish() {
        startService(new Intent(this, ClientService.class).setAction(STOP_SERVICE));
        super.finish();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        getMenuInflater().inflate(R.menu.channel_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.dark_theme:

        }

        return super.onOptionsItemSelected(item);
    }
}
