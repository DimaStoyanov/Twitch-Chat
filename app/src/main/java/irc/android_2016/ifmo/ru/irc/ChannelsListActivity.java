package irc.android_2016.ifmo.ru.irc;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import irc.android_2016.ifmo.ru.irc.model.LoginData;
import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

public class ChannelsListActivity extends AppCompatActivity {

    private LinearLayout ll;
    private List<LoginData> data;
    private boolean updateDataFromCache = false;
    private AsyncTask<Void, Void, String> loader = new FiePathLoader();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels_list);
        Log.d("IRC", "On create");
        ll = (LinearLayout) findViewById(R.id.channels_ll);
        registerForContextMenu(findViewById(R.id.settings));
        // Пока не вижу смысла, добавлять иф с чтением из savedInstanceState
        // Потому что, если мы, например, добавили новый канал, а потом вернулись на экран списка каналов,
        // То в savedInstanceState будут лежать не актуальные данные, и надо как-то это проверять.
        // В случае чего, лезть в кэш. Пока что проще сразу лезть в кэш.
        readFromCache();
        updateDataFromCache = true;
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d("IRC", "On pause");
        updateDataFromCache = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("IRC", "On start");
        // Способ не самый эффективный - возможно нужно добавить одну вьюшку, а мы все стираем, а потом добавляем.
        // Но тут данных мало и пока сойдет.
        if (!updateDataFromCache) {
            ll.removeAllViews();
            readFromCache();
            updateDataFromCache = true;
        }

    }

    public class FiePathLoader extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            return getSharedPreferences("Logind_data", MODE_PRIVATE).getString("file path", "");
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }


    private void readFromCache() {
        Log.d("IRC Chanel list", "Read from cache");
        loader.execute();
        String path = "";
        try {
            path = loader.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Если файла нет или нет доступа к нему, ничего не делаем
        if (!(new File(path)).exists()) return;
        AsyncTask<String, Void, List<LoginData>> asyncTask = new AsyncTask<String, Void, List<LoginData>>() {
            @Override
            protected List<LoginData> doInBackground(String... params) {
                return FileUtils.getData(params[0]);
            }

            @Override
            protected void onPostExecute(List<LoginData> data) {
                super.onPostExecute(data);
                Log.d("IRC Channel list", "Data read from cache");
                addChannels(data);

            }
        };
        try {
            BufferedReader temp = new BufferedReader(new FileReader(path));
            temp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO в случае неверного логина, тут может кинуться RuntimeException
        // TODO Нужно что-то предпринять, лишние проверки и прочее
        asyncTask.execute(path);


    }


    private void addChannels(List<LoginData> data) {
        this.data = data;
        View item;
        System.out.println("Login data");
        for (int i = 0; i < data.size(); i++) {
            System.out.println(data.get(i).toString());
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
            Log.d("IRC Channel list", "On select channel click");
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
            Log.d("IRC Channel list", "On delete channel click");
            for (LoginData d : data) {
                if (d.id == id) {
                    // Получаем предка  предка кнопки - линеар лайаута, хранящий линеар лайауты, которые хранят 2 кнопки
                    // и удаляем у него  предка кнопки
                    ((ViewGroup) (v.getParent()).getParent()).removeView((View) v.getParent());
                    AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            SharedPreferences pref = getSharedPreferences("Login_data", MODE_PRIVATE);
                            FileUtils.deleteData(ChannelsListActivity.this, pref.getString("file_path", ""), String.valueOf(id));
                            return null;
                        }
                    };
                    deleteTask.execute();
                    Log.d("IRC Channel list", "Delete channel");

                }
            }
        }
    }


    public void onAddChannelClick(View v) {
        Log.d("IRC Channel list", "On add channel click");
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void onSettingsClick(View v) {
        ChannelsListActivity.this.openContextMenu(v);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, "Clear cache").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d("IRC Channel list", "Clear cache");
                loader.execute();
                String path = "";
                try {
                    ll.removeAllViews();
                    return new File(loader.get()).delete();
                } catch (RuntimeException | ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });

    }

    private void readFromSaveInstance(Bundle savedInstanceState) {
        Log.d("IRC Chanel List", "Read from save instance");
        String saveState = savedInstanceState.getString("Login_data");
        data = new ArrayList<>();
        StringTokenizer datas = new StringTokenizer(saveState, "\n");
        final String DELIM = "↨";
        for (String s = datas.nextToken(); datas.hasMoreTokens(); s = datas.nextToken()) {
            StringTokenizer current_data = new StringTokenizer(s, DELIM);
            data.add(new LoginData(current_data));
        }
        addChannels(data);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("IRC Channel list", "Save state");
        super.onSaveInstanceState(outState);
        if (data == null) return;
        StringBuilder result = new StringBuilder();
        for (LoginData d : data) {
            result.append(d.toString());
        }
        outState.putString("Login_data", result.toString());

    }


}
