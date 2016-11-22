package ru.ifmo.android_2016.irc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXTSIZE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXT_LARGE;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXT_MEDIUM;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXT_SMALL;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_DARK_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_LIGHT_KEY;

public abstract class BaseActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getDefaultSharedPreferences(getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(this);
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
                    switch (prefs.getString(TEXTSIZE_KEY, "")) {
                        case TEXT_SMALL:
                            themeID = R.style.AppTheme_Small;
                            break;
                        case TEXT_MEDIUM:
                            themeID = R.style.AppTheme_Mediuim;
                            break;
                        case TEXT_LARGE:
                            themeID = R.style.AppTheme_Large;
                    }
                    break;
                case THEME_DARK_KEY:
                    switch (prefs.getString(TEXTSIZE_KEY, "")) {
                        case TEXT_SMALL:
                            themeID = R.style.AppTheme_Dark_Small;
                            break;
                        case TEXT_MEDIUM:
                            themeID = R.style.AppTheme_Dark_Mediuim;
                            break;
                        case TEXT_LARGE:
                            themeID = R.style.AppTheme_Dark_Large;
                    }
            }
            setTheme(themeID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case THEME_KEY:
            case TEXTSIZE_KEY:
                recreate();
        }
    }
}
