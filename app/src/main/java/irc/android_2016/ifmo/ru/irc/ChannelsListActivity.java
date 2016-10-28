package irc.android_2016.ifmo.ru.irc;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import irc.android_2016.ifmo.ru.irc.constant.FilePathConstant;
import irc.android_2016.ifmo.ru.irc.loader.DeleteDataTask;
import irc.android_2016.ifmo.ru.irc.loader.LoadResult;
import irc.android_2016.ifmo.ru.irc.loader.LoginReadTask;
import irc.android_2016.ifmo.ru.irc.loader.ResultType;
import irc.android_2016.ifmo.ru.irc.model.LoginData;

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
        registerForContextMenu(findViewById(R.id.settings));
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
