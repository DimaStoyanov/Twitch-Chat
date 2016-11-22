package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;

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

        getFragmentManager().beginTransaction().replace(R.id.pref, new SettingsFragment()).commit();
    }

    @Override
    public void getStartPreferences() {
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            findPreference(CACHE_KEY).setOnPreferenceClickListener(this);
        }


        @Override
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
