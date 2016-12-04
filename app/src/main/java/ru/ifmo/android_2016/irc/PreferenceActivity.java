package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;

import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CLEAR_EMOTES_CACHE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CLEAR_LOGIN_KEY;

/**
 * Created by Dima Stoyanov on 21.11.2016.
 * Project Android-IRC
 * Start time : 4:28
 */

public class PreferenceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getSupportFragmentManager()
                .beginTransaction().replace(R.id.pref, new SettingsFragment()).commit();
    }


    @Override
    public void getStartPreferences() {
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.pref_main);
            findPreference(CLEAR_LOGIN_KEY).setOnPreferenceClickListener(this::onPreferenceClick);
        }


        public boolean onPreferenceClick(Preference preference) {
            Log.d("settings", preference.getKey());
            switch (preference.getKey()) {
                case CLEAR_LOGIN_KEY:
                    try {
                        if (ServerList.getInstance() != null) ServerList.getInstance().clear();
                        Toast.makeText(getActivity(), "Cache cleared", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Can't clear cahce now", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CLEAR_EMOTES_CACHE_KEY:
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    imagePipeline.clearMemoryCaches();
                    imagePipeline.clearDiskCaches();
                    imagePipeline.clearCaches();
            }
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
