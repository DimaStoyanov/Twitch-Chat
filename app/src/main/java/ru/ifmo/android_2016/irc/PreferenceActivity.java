package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import ru.ifmo.android_2016.irc.utils.Log;

import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CACHE_KEY;

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
            findPreference(CACHE_KEY).setOnPreferenceClickListener(this::onPreferenceClick);
        }

        public boolean onPreferenceClick(Preference preference) {
            Log.d("settings", preference.getKey());
            switch (preference.getKey()) {
                case "clear_cache":
//                     It doesn't work!!!
//                    FileUtils.deleteDirectory(new File(getActivity().getFilesDir().getPath() + "/data.obj"));
                    Toast.makeText(getActivity(), "Cache cleared", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }


}
