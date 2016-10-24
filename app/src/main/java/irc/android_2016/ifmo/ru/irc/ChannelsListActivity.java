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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import irc.android_2016.ifmo.ru.irc.model.LoginData;
import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

public class ChannelsListActivity extends AppCompatActivity {

    private LinearLayout ll;
    private List<LoginData> data;
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

    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("IRC", "On start");
        // TODO При добавлении новых каналов, они появляются на экране, только если
        // TODO прилоежние попало в onCreate. Нужно придумать способ как в ином случае проверять
        // TODO и добавлять новые элементы
    }


    private void readFromCache() {
        Log.d("IRC Chanel list", "Read from cache");
        SharedPreferences pref = getSharedPreferences("Login_data", MODE_PRIVATE);
        final String path = pref.getString("file_path", "");
        if ("".equals(path)) return;
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
        // TODO в случае неверного логина, тут может кинуться RuntimeException
        // TODO Нужно что-то предпринять, лишние проверки и прочее
        asyncTask.execute(path);



    }


    private void addChannels(List<LoginData> data) {
        this.data = data;
        View item;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < data.size(); i++) {
            item = inflater.inflate(R.layout.chanel_item, null);
            Button selectChannel = (Button) item.findViewById(R.id.selected_channel);
            ImageButton deleteChannel = (ImageButton) item.findViewById(R.id.delete_channel);
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
                    // TODO здесь падает если удалять не первый элемент (nullptrException), пока не знаю почему.
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
        menu.add(0, 1, 0, "Clear casche").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d("IRC", "Clear cache");
                SharedPreferences preferences = getSharedPreferences("Login_data", MODE_PRIVATE);
                SharedPreferences.Editor ed = preferences.edit();
                try {
                    ed.putString("file_path", FileUtils.getPathOfExternalFile(ChannelsListActivity.this));
                    ed.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
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
