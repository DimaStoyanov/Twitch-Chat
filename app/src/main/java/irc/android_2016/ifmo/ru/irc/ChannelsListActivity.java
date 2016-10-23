package irc.android_2016.ifmo.ru.irc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class ChannelsListActivity extends AppCompatActivity implements View.OnClickListener {

    private Button add;
    private ImageButton settings;
    private LinearLayout channels;
    private ScrollView scroll_v;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channels_list);
        init();
        if (savedInstanceState != null) {
            Log.d("IRC Chanel List", "Read from save instance");
            readFromSaveInstance(savedInstanceState);
        } else {
            Log.d("IRC Chanel list", "Read from cache");
            readFromCache();
        }
    }

    private void init() {
        add = (Button) findViewById(R.id.add_channel);
        settings = (ImageButton) findViewById(R.id.settings);
        channels = (LinearLayout) findViewById(R.id.channels_ll);
        scroll_v = (ScrollView) findViewById(R.id.channels_sv);
        Button kek = new Button(this);
        kek.setText("Button 1");
        kek.setOnClickListener(this);
        channels.addView(kek);
        kek = new Button(this);
        kek.setText("Button 2");
        kek.setOnClickListener(this);
        channels.addView(kek);
    }

    private void readFromSaveInstance(Bundle savedInstanceState) {
    }

    private void readFromCache() {

    }


    public void onAddChannelClick(View v) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void onSettingsClick(View v) {

    }


    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        Toast.makeText(this, b.getText().toString(), Toast.LENGTH_SHORT).show();
    }
}
