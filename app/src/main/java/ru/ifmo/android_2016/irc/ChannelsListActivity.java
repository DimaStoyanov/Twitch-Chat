package ru.ifmo.android_2016.irc;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.client.ClientSettings;
import ru.ifmo.android_2016.irc.constant.FilePathConstant;
import ru.ifmo.android_2016.irc.loader.LoadResult;
import ru.ifmo.android_2016.irc.loader.ResultType;
import ru.ifmo.android_2016.irc.loader.TwitchUserNickLoader;
import ru.ifmo.android_2016.irc.utils.FileManager;
import ru.ifmo.android_2016.irc.utils.IOUtils;

import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.OAUTH_URL;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.REDIRECT_URL;
import static ru.ifmo.android_2016.irc.utils.WebUtils.getAccessToken;

public class ChannelsListActivity extends AppCompatActivity {

    private LinearLayout ll;
    private boolean updateDataFromCache = false;
    private FileManager fileManager;
    private ProgressBar pb;
    public final String TAG = ChannelsListActivity.class.getSimpleName();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels_list);
        ll = (LinearLayout) findViewById(R.id.channels_ll);
        pb = (ProgressBar) findViewById(R.id.pbar);
        fileManager = new FileManager(FileManager.FileType.Login, new FilePathConstant(this));
        context = this;
        Log.d(TAG, "On create");
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
            readFromCache();
            updateDataFromCache = true;
        }

    }

    @UiThread
    private void readFromCache() {
        Log.d(TAG, "Read from cache");
        pb.setVisibility(View.VISIBLE);

        // мб сделать чтобы при повороте экрана не начиналась новая загрузка
        AsyncTask<Void, Void, ArrayList<ClientSettings>> readTask = new AsyncTask<Void, Void, ArrayList<ClientSettings>>() {

            @Override
            protected void onPostExecute(ArrayList<ClientSettings> clientSettingses) {
                super.onPostExecute(clientSettingses);
                addChannels(clientSettingses);
            }

            @Override
            @WorkerThread
            protected ArrayList<ClientSettings> doInBackground(Void... voids) {
                ArrayList<ClientSettings> data = null;
                try {
                    data = (ArrayList<ClientSettings>) fileManager.getData();
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage() == null ? "null" : e.getMessage());
                }
                return data;

            }
        };
        readTask.execute();
    }


    @UiThread
    private void addChannels(ArrayList<ClientSettings> data) {
        pb.setVisibility(View.GONE);
        if (data == null) {
            Log.d(TAG, "Empty channel lists");
            return;
        }
        ll.removeAllViews();
        View item;
        Log.d(TAG, "LOGIN DATA:");
        for (int i = 0; i < data.size(); i++) {
            Log.d(TAG, data.get(i).toString());
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < data.size(); i++) {
            item = inflater.inflate(R.layout.chanel_item, null);
            Button selectChannel = (Button) item.findViewById(R.id.selected_channel);
            if (data.get(i) == null) {
                Log.d(TAG, "data " + i + " = null");
                continue;
            }
            selectChannel.setText(data.get(i).getName());
            selectChannel.setOnClickListener(new OnSelectChannelListener(data.get(i)));
            (item.findViewById(R.id.delete_channel)).setOnClickListener(new OnDeleteChannelListener(data.get(i)));
            (item.findViewById(R.id.edit_channel)).setOnClickListener(new OnEditChannelListener(data.get(i)));
            ll.addView(item);
        }
    }

    @UiThread
    private class OnSelectChannelListener implements View.OnClickListener {

        private ClientSettings data;

        OnSelectChannelListener(ClientSettings data) {
            this.data = data;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On select channel click");
            Intent intent = new Intent(ChannelsListActivity.this, ChatActivity.class);
            intent.putExtra("Name", data.getName());
            intent.putExtra("Server", data.getServer());
            intent.putExtra("Port", data.getPort());
            intent.putExtra("Username", data.getUsername());
            intent.putExtra("Password", data.getPassword());
            intent.putExtra("Channel", data.getChannels());
            intent.putExtra("SSL", data.getSSL());
            startActivity(intent);
        }
    }

    @UiThread
    private class OnDeleteChannelListener implements View.OnClickListener {

        private ClientSettings data;

        OnDeleteChannelListener(ClientSettings data) {
            this.data = data;
        }

        @Override
        public void onClick(View v) {
            ((ViewGroup) (v.getParent()).getParent()).removeView((View) v.getParent());
            AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        fileManager.deleteData(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    final EditText name = (EditText) dialog.findViewById(R.id.name);
                    final EditText server = (EditText) dialog.findViewById(R.id.server);
                    final EditText port = (EditText) dialog.findViewById(R.id.port);
                    final EditText username = (EditText) dialog.findViewById(R.id.username);
                    final EditText password = (EditText) dialog.findViewById(R.id.password);
                    final EditText channel = (EditText) dialog.findViewById(R.id.channel);
                    final CheckBox ssl = (CheckBox) dialog.findViewById(R.id.use_ssl);
                    name.setText(data.getName());
                    server.setText(data.getServer());
                    port.setText(data.getPort());
                    username.setText(data.getUsername());
                    password.setText(data.getPassword());
                    channel.setText(data.getChannels());
                    ssl.setChecked(data.getSSL());
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            Toast toast = new Toast(context);
                            if (!TextUtils.isEmpty(server.getText()) &&
                                    !TextUtils.isEmpty(port.getText())
                                    && !TextUtils.isEmpty(username.getText())
                                    && !TextUtils.isEmpty(password.getText())
                                    && !TextUtils.isEmpty(channel.getText())) {

                                dialog.cancel();
                                new EditChannelTask().execute(data, buildClientSettings(name.getText().toString(), server.getText().toString(),
                                        port.getText().toString(), username.getText().toString(), password.getText().toString(),
                                        channel.getText().toString(), String.valueOf(ssl.isChecked())));
                            } else {
                                toast.cancel();
                                toast = Toast.makeText(context, "Fill the fields correctly", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
                }
            });
            dialog.show();
        }
    }


    class EditChannelTask extends AsyncTask<ClientSettings, Void, Void> {
        @Override
        @WorkerThread
        protected Void doInBackground(ClientSettings... clientSettingses) {
            try {
                fileManager.editData(clientSettingses[0], clientSettingses[1]);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        @UiThread
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            readFromCache();
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

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                final EditText name = (EditText) dialog.findViewById(R.id.name);
                final EditText server = (EditText) dialog.findViewById(R.id.server);
                final EditText port = (EditText) dialog.findViewById(R.id.port);
                final EditText username = (EditText) dialog.findViewById(R.id.username);
                final EditText password = (EditText) dialog.findViewById(R.id.password);
                final EditText channel = (EditText) dialog.findViewById(R.id.channel);
                final CheckBox ssl = (CheckBox) dialog.findViewById(R.id.use_ssl);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Toast toast = new Toast(context);
                        if (!TextUtils.isEmpty(server.getText()) &&
                                !TextUtils.isEmpty(port.getText())
                                && !TextUtils.isEmpty(username.getText())
                                && !TextUtils.isEmpty(password.getText())
                                && !TextUtils.isEmpty(channel.getText())) {

                            Intent intent = new Intent(context, ChatActivity.class);

                            intent.putExtra("Name", name.getText().toString());
                            intent.putExtra("Server", server.getText().toString());
                            intent.putExtra("Port", port.getText().toString());
                            intent.putExtra("Username", username.getText().toString());
                            intent.putExtra("Password", password.getText().toString());
                            intent.putExtra("Channel", channel.getText().toString());
                            intent.putExtra("SSL", ssl != null && ssl.isChecked());

                            dialog.cancel();
                            (new AddChannelsTask()).execute(buildClientSettings(name.getText().toString(), server.getText().toString(),
                                    port.getText().toString(), username.getText().toString(), password.getText().toString(),
                                    channel.getText().toString(), String.valueOf(ssl.isChecked())));
                            startActivity(intent);

                        } else {
                            toast.cancel();
                            toast = Toast.makeText(context, "Fill the fields correctly", Toast.LENGTH_SHORT);
                            toast.show();
                        }
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
            }
        });
        dialog.show();
    }

    @UiThread
    private ClientSettings buildClientSettings(String... args) {
        return new ClientSettings.Builder()
                .setName(TextUtils.isEmpty(args[0]) ? args[5] : args[0])
                .setAddress(args[1])
                .setPort(Integer.parseInt(args[2]))
                .setUsername(args[3])
                .setPassword(args[4])
                .setChannels(args[5])
                .setSsl(Boolean.parseBoolean(args[6]))
                .build();
    }

    @WorkerThread
    private class AddChannelsTask extends AsyncTask<ClientSettings, Void, Void> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "Start adding client settings");
        }

        @Override
        protected Void doInBackground(ClientSettings... clientSettingses) {
            try {
                fileManager.addData(clientSettingses[0]);
            } catch (IOException e) {
                Log.d(TAG, "Can't add files to storage " + e.getMessage());
            }
            return null;

        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void onTwitchLoginClick(View v) {
        final Dialog dialog = new Dialog(this);
        if (dialog.getWindow() != null)
            dialog.getWindow().setTitle("Twitch OAuth 2.0");
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_twitch_oauth);
        WebView wv = (WebView) dialog.findViewById(R.id.wv);
        final ProgressBar progressBar = (ProgressBar) dialog.findViewById(R.id.pbar);
        wv.getSettings().setJavaScriptEnabled(true);

        wv.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(REDIRECT_URL)) {
                    dialog.cancel();
                    getSupportLoaderManager().initLoader(10, null, new NickLoaderCallback(getAccessToken(url, context)));
                    return true;
                }
                return false;

            }
        });
        if (checkInternetConnection(true)) {
            wv.loadUrl(OAUTH_URL);
            dialog.show();
        }
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
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        checkInternetConnection(true);
                    }
                }).setNeutralButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finishAffinity();
            }
        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                checkInternetConnection(false);

            }
        });
        builder.create().show();

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


                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            Toast toast = new Toast(ChannelsListActivity.this);

                            @Override
                            public void onClick(View view) {
                                final EditText name = (EditText) alert.findViewById(R.id.name);
                                final EditText channel = (EditText) alert.findViewById(R.id.channel);
                                final CheckBox ssl = (CheckBox) alert.findViewById(R.id.use_ssl);
                                if (!TextUtils.isEmpty(channel.getText())) {
                                    Intent intent = new Intent(ChannelsListActivity.this, ChatActivity.class);
                                    intent.putExtra("Name", TextUtils.isEmpty(name.getText()) ?
                                            channel.getText().toString() : name.getText().toString());
                                    intent.putExtra("Server", "irc.chat.twitch.tv");
                                    intent.putExtra("Port", "6667");
                                    intent.putExtra("Username", result.data);
                                    intent.putExtra("Password", "oauth:" + token);
                                    intent.putExtra("Channel", channel.getText().toString());
                                    intent.putExtra("SSL", ssl.isChecked());
                                    alert.dismiss();
                                    new AddChannelsTask().execute(buildClientSettings(name.getText().toString(),
                                            "irc.chat.twitch.tv", "6667", result.data,
                                            "oauth:" + token, channel.getText().toString(), String.valueOf(ssl.isChecked())));
                                    startActivity(intent);


                                } else {
                                    toast.cancel();
                                    toast = Toast.makeText(ChannelsListActivity.this, "Empty channel", Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            }
                        });
                        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                alert.dismiss();
                            }
                        });
                    }
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


}
