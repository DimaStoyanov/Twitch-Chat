package ru.ifmo.android_2016.irc;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ru.ifmo.android_2016.irc.constant.FilePathConstant;
import ru.ifmo.android_2016.irc.loader.DeleteDataTask;
import ru.ifmo.android_2016.irc.loader.LoadResult;
import ru.ifmo.android_2016.irc.loader.LoginReadTask;
import ru.ifmo.android_2016.irc.loader.ResultType;
import ru.ifmo.android_2016.irc.loader.TwitchUserNickLoader;
import ru.ifmo.android_2016.irc.model.LoginData;
import ru.ifmo.android_2016.irc.utils.SessionStore;

import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.OAUTH_URL;
import static ru.ifmo.android_2016.irc.constant.TwitchApiConstant.REDIRECT_URL;

public class ChannelsListActivity extends AppCompatActivity {

    private LinearLayout ll;
    private List<LoginData> data;
    private boolean updateDataFromCache = false;
    private FilePathConstant constant;
    private ProgressBar pb;
    private final String TAG = "IRC Channel list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels_list);
        Log.d(TAG, "On create");
        constant = new FilePathConstant(this);
        ll = (LinearLayout) findViewById(R.id.channels_ll);
        pb = (ProgressBar) findViewById(R.id.pbar);
//        registerForContextMenu(findViewById(R.id.settings));
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
        // Способ не самый эффективный - возможно нужно добавить одну вьюшку, а мы все стираем, а потом добавляем.
        // Но тут данных мало и пока сойдет. И я не знаю как понять какую вьюшку удалять, id не подходит,
        // разве что с позицией придумать что-нибудь
        if (!updateDataFromCache) {
            ll.removeAllViews();
            readFromCache();
            updateDataFromCache = true;
        }

    }


    private void readFromCache() {
        Log.d(TAG, "Read from cache");
        Bundle bundle = new Bundle();
        if (!(new File(constant.LOGIN_DATA)).isFile()) {
            Log.d(TAG, "Login file not exists");
            return;
        }
        bundle.putString("data", constant.LOGIN_DATA);
        getSupportLoaderManager().initLoader(0, bundle, new LoaderManager.LoaderCallbacks<LoadResult<List<LoginData>>>() {
            @Override
            public Loader<LoadResult<List<LoginData>>> onCreateLoader(int id, Bundle args) {

                pb.setVisibility(View.VISIBLE);
                return new LoginReadTask(ChannelsListActivity.this, args.getString("data", ""));
            }

            @Override
            public void onLoadFinished(Loader<LoadResult<List<LoginData>>> loader, LoadResult<List<LoginData>> result) {
                Log.d(TAG, "Load login data finished");
                if (result.resultType == ResultType.OK) {
                    data = result.data;
                    addChannels(data);
                    Log.d(TAG, "Channel added");
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChannelsListActivity.this)
                            .setCancelable(false).setTitle("File reading error")
                            .setMessage("Can't read login data from external storage")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                    readFromCache();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                pb.setVisibility(View.GONE);
                getSupportLoaderManager().destroyLoader(loader.getId());
            }


            @Override
            public void onLoaderReset(Loader<LoadResult<List<LoginData>>> loader) {
            }
        });
    }


    public void onTwitchLoginClick(View v) {

        final Dialog dialog = new Dialog(this);
        dialog.getWindow().setTitle("Twitch OAuth 2.0");

        dialog.setCancelable(true);
        dialog.setContentView(R.layout.twitch_dialog);
        WebView wv = (WebView) dialog.findViewById(R.id.wv);
        final ProgressBar progressBar = (ProgressBar) dialog.findViewById(R.id.pbar);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
            }
        });
        wv.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                progressBar.setMax(100);
                progressBar.setProgress(0);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(REDIRECT_URL)) {
                    dialog.cancel();
                    parseResult(url);
                    return true;
                }
                return false;

            }
        });
        wv.loadUrl(OAUTH_URL);
        dialog.show();
        pb.setVisibility(View.VISIBLE);
//        alert.show();
    }

    private void parseResult(String url) {
        final Uri uri = Uri.parse(url);
        final String fragment = uri.getFragment();

        if (fragment == null) {
            finish();
            return;
        }

        String error = null;
        String accessToken = null;
        String sessionSecretKey = null;

        int off = 0;
        int equalSignPosition;
        int length = fragment.length();

        while (off < length && (equalSignPosition = fragment.indexOf('=', off)) != -1) {
            final String key = fragment.substring(off, equalSignPosition);
            final int andSignPosition = fragment.indexOf('&', equalSignPosition + 1);
            final int valueEnd = andSignPosition > equalSignPosition ? andSignPosition : length;
            final String value = fragment.substring(equalSignPosition + 1, valueEnd);
            switch (key) {
                case "access_token":
                    accessToken = value;
                    break;
                case "session_secret_key":
                    sessionSecretKey = value;
                    break;
                case "error":
                    error = value;
                    break;
            }
            off = valueEnd + 1;
        }

        final Intent data = new Intent();
        if (!TextUtils.isEmpty(accessToken) /*&& !TextUtils.isEmpty(sessionSecretKey)*/) {
            SessionStore.getInstance().updateKeys(this, accessToken, sessionSecretKey);
            setResult(Activity.RESULT_OK);
        }
        if (!TextUtils.isEmpty(error)) {
            data.putExtra("error", error);
        }
        Log.d("token", accessToken);
        getSupportLoaderManager().initLoader(10, null, new NickLoaderCallback(accessToken));
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
                final EditText text = new EditText(ChannelsListActivity.this);
                text.setText("#");
                text.setSelection(1);
                builder = new AlertDialog.Builder(ChannelsListActivity.this)
                        .setCancelable(true)
                        .setTitle("Type channel")
                        .setMessage("You can also type several channels, separated by a comma")
                        .setView(text)
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
                                Log.d(TAG, "EditText view " + text.getText());
                                if (!text.getText().toString().equals("#")) {
                                    Intent intent = new Intent(ChannelsListActivity.this, ChatActivity.class);
                                    intent.putExtra("Server", "irc.chat.twitch.tv:6667");
                                    intent.putExtra("Nick", result.data);
                                    intent.putExtra("Password", "oauth:" + token);
                                    intent.putExtra("Channel", text.getText().toString());
                                    intent.putExtra("Saved", false);
                                    Log.d("nick", result.data);
                                    startActivity(intent);
                                    alert.dismiss();
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
                builder = new AlertDialog.Builder(ChannelsListActivity.this)
                        .setCancelable(false).setTitle("File reading error")
                        .setMessage("Can't read login data from external storage")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                readFromCache();
                            }
                        });
                alert = builder.create();
                alert.show();
            }

            pb.setVisibility(View.GONE);
            getSupportLoaderManager().destroyLoader(loader.getId());
        }

        @Override
        public void onLoaderReset(Loader<LoadResult<String>> loader) {

        }
    }

    private void addChannels(List<LoginData> data) {
        if (data == null) {
            Log.d(TAG, "Empty channel lists");
            return;
        }
        View item;
        Log.d(TAG, "LOGIN DATA:");
        for (int i = 0; i < data.size(); i++) {
            Log.d(TAG, data.get(i).toString());
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < data.size(); i++) {
            item = inflater.inflate(R.layout.chanel_item, null);
            Button selectChannel = (Button) item.findViewById(R.id.selected_channel);
            ImageButton deleteChannel = (ImageButton) item.findViewById(R.id.delete_channel);
            if (data.get(i).channel.length() == 0) continue;
            selectChannel.setText(data.get(i).channel.substring(1));
            selectChannel.setOnClickListener(new OnSelectChannelListener(data.get(i).id));
            deleteChannel.setOnClickListener(new OnDeleteChannelListener(data.get(i).id));
            ll.addView(item);
        }
    }

    private class OnSelectChannelListener implements View.OnClickListener {

        private int id;

        OnSelectChannelListener(int id) {
            this.id = id;
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "On select channel click");
            for (LoginData d : data) {
                if (d.id == id) {
                    Intent intent = new Intent(ChannelsListActivity.this, ChatActivity.class);
                    intent.putExtra("Server", d.server);
                    intent.putExtra("Nick", d.nick);
                    intent.putExtra("Password", d.password);
                    intent.putExtra("Channel", d.channel);
                    intent.putExtra("Saved", true);
                    startActivity(intent);
                }
            }
        }
    }

    private class OnDeleteChannelListener implements View.OnClickListener {

        private int id;

        OnDeleteChannelListener(int id) {
            this.id = id;
        }

        @Override
        public void onClick(View v) {
            for (LoginData d : data) {
                if (d.id == id) {
                    // Получаем предка  предка кнопки - линеар лайаута, хранящий линеар лайауты, которые хранят 2 кнопки
                    // и удаляем у него  предка кнопки
                    ((ViewGroup) (v.getParent()).getParent()).removeView((View) v.getParent());
                    Bundle bundle = new Bundle();
                    bundle.putString("path", constant.LOGIN_DATA);
                    bundle.putString("package_name", constant.LOGIN_PACKAGE);
                    bundle.putString("id", String.valueOf(id));
                    getSupportLoaderManager().initLoader(1, bundle, new LoaderManager.LoaderCallbacks<LoadResult<Void>>() {


                        @Override
                        public Loader<LoadResult<Void>> onCreateLoader(int id, Bundle args) {
                            return new DeleteDataTask(ChannelsListActivity.this,
                                    args.getString("path"), args.getString("package_name"), args.getString("id"));
                        }

                        // Note: этото лоаедар ничего не возвращает, хоть и меняет файл.
                        // На деле это ничего не меняет, так как мы используем data либо сразу при считывание,
                        // либо при проверки нажатия кнопки удаления канала (т.е. чуть дольше искать будем)
                        @Override
                        public void onLoadFinished(Loader<LoadResult<Void>> loader, LoadResult<Void> data) {
                            loader.reset();
                        }

                        @Override
                        public void onLoaderReset(Loader<LoadResult<Void>> loader) {
                            getSupportLoaderManager().destroyLoader(loader.getId());
                        }
                    });
                    Log.d(TAG, "Delete channel");
                }
            }
        }
    }


    public void onAddChannelClick(View v) {
        Log.d(TAG, "On add channel click");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void onSettingsClick(View v) {
        ChannelsListActivity.this.openContextMenu(v);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, "Clear login data").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "Clear login data");
                ll.removeAllViews();
                return (new File(constant.LOGIN_DATA).delete());
            }
        });
        menu.add(0, 2, 0, "Clear emoticons").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Log.d(TAG, "Clear emoticons from external storage");
                return deleteDirectory(new File(constant.EMOTICONS_PACKAGE));
            }
        });

    }

    private boolean deleteDirectory(File f) {
        if (!f.exists()) return false;
        if (f.isFile())
            return f.delete();
        if (f.isDirectory()) {
            File[] subfs = f.listFiles();
            for (File t : subfs) {
                deleteDirectory(t);
            }
            return f.delete();
        }
        return false;
    }

    private void readFromSaveInstance(Bundle savedInstanceState) {
        Log.d(TAG, "Read from save instance");
        String saveState = savedInstanceState.getString(constant.LOGIN_PACKAGE);
        data = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(saveState, "\n");
        final String DELIM = "↨";
        for (String s = tokenizer.nextToken(); tokenizer.hasMoreTokens(); s = tokenizer.nextToken()) {
            StringTokenizer current_data = new StringTokenizer(s, DELIM);
            data.add(new LoginData(current_data));
        }
        addChannels(data);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "Save state");
        super.onSaveInstanceState(outState);
        if (data == null) return;
        StringBuilder result = new StringBuilder();
        for (LoginData d : data) {
            result.append(d.toString());
        }
        outState.putString("Login_data", result.toString());

    }


}
