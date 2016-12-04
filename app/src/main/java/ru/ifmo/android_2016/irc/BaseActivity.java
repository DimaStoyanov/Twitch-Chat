package ru.ifmo.android_2016.irc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import ru.ifmo.android_2016.irc.ui.span.ChangeableForegroundColorSpan;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_DARK_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_LIGHT_KEY;

public abstract class BaseActivity extends AppCompatActivity {

    SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener =
            this::onSharedPreferenceChanged;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(listener);
        setThemeFromPref();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getStartPreferences();
    }

    public void getStartPreferences() {
    }


    private void setThemeFromPref() {
        int themeID = 0;
        try {
            switch (prefs.getString(THEME_KEY, "")) {
                case THEME_LIGHT_KEY:
                    ChangeableForegroundColorSpan.setLightness(80.f / 256);
                    themeID = R.style.AppTheme;
                    break;
                case THEME_DARK_KEY:
                    ChangeableForegroundColorSpan.setLightness(180.f / 256);
                    themeID = R.style.AppTheme_Dark;
            }
            setTheme(themeID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case THEME_KEY:
                recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
