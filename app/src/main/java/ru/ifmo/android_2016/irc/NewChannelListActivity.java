package ru.ifmo.android_2016.irc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Collection;

import ru.ifmo.android_2016.irc.client.ClientService;
import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.constant.PreferencesConstant;
import ru.ifmo.android_2016.irc.loader.LoadResult;
import ru.ifmo.android_2016.irc.loader.ResultType;
import ru.ifmo.android_2016.irc.loader.TwitchUserNickLoader;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.client.ClientService.SERVER_ID;

/**
 * Created by Dima Stoyanov on 21.11.2016.
 * Project Android-IRC
 * Start time : 18:09
 */


public class NewChannelListActivity extends BaseActivity {

    private ArrayAdapter<String> adapter;
    private ArrayList<ClientSettings> clientSettings;
    private ArrayList<String> channels;
    public final String TAG = NewChannelListActivity.class.getSimpleName();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        channels = new ArrayList<>();
        clientSettings = new ArrayList<>();

        setContentView(R.layout.activity_new_channel_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, channels);

        ListView listView = (ListView) findViewById(R.id.list_veiw);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> startActivity(new Intent(context, ChatActivity.class)
                .putExtra(SERVER_ID, clientSettings.get(i).getId())));
        registerForContextMenu(listView);
        context = this;

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    protected void onStart() {
        ServerList.load(this, this::updateChannelList);
        super.onStart();
    }

    @Override
    protected void onStop() {
        ServerList.save(IRCApplication.getFilesDirectory());
        super.onStop();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        super.onSharedPreferenceChanged(sharedPreferences, s);
        if (s.equals(PreferencesConstant.CLEAR_LOGIN_KEY)) {
            updateChannelList();
        }
    }


    @UiThread
    private void updateChannelList() {
        ServerList serverList = ServerList.getInstance();

        if (serverList == null) return;

        Collection<ClientSettings> data = serverList.values();
        clientSettings = new ArrayList<>();
        channels = new ArrayList<>();
        clientSettings.addAll(data);
        channels.addAll(Stream.of(clientSettings)
                .map(ClientSettings::getName)
                .collect(Collectors.toList()));

        Log.d(TAG, clientSettings.toString());
        Log.d(TAG, channels.toString());
        adapter.clear();
        adapter.addAll(channels);
        adapter.notifyDataSetChanged();
    }


    public void onTwitchOauthClick(View view) {
        startActivityForResult(new Intent(this, TwitchLoginActivity.class), 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 200) {
            getSupportLoaderManager().initLoader(10, null,
                    new NewChannelListActivity.NickLoaderCallback(data.getStringExtra("ru.ifmo.android_2016.irc.Token")));

        }
    }


    private class NickLoaderCallback implements LoaderManager.LoaderCallbacks<LoadResult<String>> {
        private final String token;

        NickLoaderCallback(String access_token) {
            token = access_token;
        }

        @Override
        public Loader<LoadResult<String>> onCreateLoader(int id, Bundle args) {
            return new TwitchUserNickLoader(context, token);
        }

        @Override
        public void onLoadFinished(Loader<LoadResult<String>> loader, final LoadResult<String> result) {
            Log.d(TAG, "Load login data finished");
            AlertDialog.Builder builder;
            final AlertDialog alert;
            if (result.resultType == ResultType.OK) {
                builder = new AlertDialog.Builder(context)
                        .setCancelable(true)
                        .setTitle("Type channel")
                        .setMessage("You can also type several channels, separated by a comma")
                        .setView(R.layout.dialog_login_twitch)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Cancel", null);

                alert = builder.create();


                alert.setOnShowListener(dialogInterface -> {
                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        Toast toast = new Toast(context);

                        @SuppressWarnings("ConstantConditions")
                        @Override
                        public void onClick(View view) {
                            final EditText name = (EditText) alert.findViewById(R.id.name);
                            final EditText channel = (EditText) alert.findViewById(R.id.channel);
                            final CheckBox ssl = (CheckBox) alert.findViewById(R.id.use_ssl);
                            if (!TextUtils.isEmpty(channel.getText())) {
                                @SuppressWarnings("deprecation") long id = ServerList.getInstance()
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
                                toast = Toast.makeText(context, "Empty channel", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
                    alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> alert.dismiss());
                });
                alert.show();
                Log.d(TAG, "Twitch Authorized successfully");
            } else {
                Toast.makeText(context, "error request api", Toast.LENGTH_SHORT).show();
            }
            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<LoadResult<String>> loader) {
        }

    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
      /*  menu.add(0, 1, Menu.CATEGORY_CONTAINER, "#ERROR#").setOnMenuItemClickListener(menuItem -> {
            startActivity(new Intent(this, ErrorActivity.class));
            return false;
        });*/
        menu.add(0, 2, Menu.CATEGORY_CONTAINER, getString(R.string.settings)).setOnMenuItemClickListener(menuItem -> {
            startActivity(new Intent(this, PreferenceActivity.class));
            return false;
        });
        menu.add(0, 3, Menu.CATEGORY_CONTAINER, getString(R.string.exit)).setOnMenuItemClickListener(menuItem -> {
            ClientService.stopAllClients();
            finish();
            return false;
        });
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.list_veiw) {
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;

            menu.add(0, 0, Menu.CATEGORY_CONTAINER, "Connect").setOnMenuItemClickListener(menuItem -> {
                startActivity(new Intent(context, ChatActivity.class)
                        .putExtra(SERVER_ID, clientSettings.get(acmi.position).getId()));
                return false;
            });
            menu.add(0, 1, Menu.CATEGORY_CONTAINER, "Edit").setOnMenuItemClickListener(menuItem -> {
                onEditChannel(clientSettings.get(acmi.position));
                return false;
            });
            menu.add(0, 2, Menu.CATEGORY_CONTAINER, "Delete").setOnMenuItemClickListener(menuItem -> {
                AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    protected Void doInBackground(Void... voids) {
                        ServerList.getInstance().remove(clientSettings.get(acmi.position).getId());
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        updateChannelList();
                    }
                };
                deleteTask.execute();
                Log.d(TAG, "Delete channel");
                return false;
            });

        }
    }


    @SuppressWarnings("ConstantConditions")
    private void onEditChannel(ClientSettings data) {
        Log.d(TAG, "On edit channel click");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .setView(R.layout.dialog_login_twitch);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            final EditText name = (EditText) dialog.findViewById(R.id.name);
            final EditText channel = (EditText) dialog.findViewById(R.id.channel);
            final CheckBox ssl = (CheckBox) dialog.findViewById(R.id.use_ssl);
            name.setText(data.getName());
            channel.setText(data.getChannels());
            ssl.setChecked(data.isSsl());
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> dialog.dismiss());
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {

                Toast toast = new Toast(context);
                if (!TextUtils.isEmpty(channel.getText())) {

                    dialog.cancel();

                    //noinspection deprecation
                    data.setName(name.getText().toString())
                            .setChannel(channel.getText().toString())
                            .setSsl(ssl.isChecked());
                    updateChannelList();
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

