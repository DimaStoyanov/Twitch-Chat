package ru.ifmo.android_2016.irc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CACHE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_KEY;
import static ru.ifmo.android_2016.irc.utils.ThemeUtils.changeTheme;
import static ru.ifmo.android_2016.irc.utils.ThemeUtils.changeThemeAndRecreate;
import static ru.ifmo.android_2016.irc.utils.ThemeUtils.onActivityCreateSetTheme;

/**
 * Created by Dima Stoyanov on 21.11.2016.
 * Project Android-IRC
 * Start time : 4:28
 */

public class PreferenceActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        changeTheme(prefs.getString(THEME_KEY, ""));
        onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getFragmentManager().beginTransaction().replace(R.id.pref, new SettingsFragment()).commit();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case THEME_KEY:
                changeThemeAndRecreate(this, sharedPreferences.getString(s, ""));
        }
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
                    // TODO
                    Toast.makeText(getActivity(), "Cache cleared", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        }
        return false;
    }
}
