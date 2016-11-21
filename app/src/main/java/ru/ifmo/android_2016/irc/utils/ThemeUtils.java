package ru.ifmo.android_2016.irc.utils;

import android.app.Activity;

import ru.ifmo.android_2016.irc.R;

/**
 * Created by Dima Stoyanov on 21.11.2016.
 * Project Android-IRC
 * Start time : 14:01
 */

public class ThemeUtils {
    private static String currentTheme;
    public final static String THEME_LIGHT = "Light";
    public final static String THEME_DARK = "Dark";


    public static void changeThemeAndRecreate(Activity activity, String theme) {
        currentTheme = theme;
        activity.recreate();
    }

    public static int getCurrentTheme() {
        switch (currentTheme) {
            case THEME_LIGHT:
                return R.style.AppTheme;
            case THEME_DARK:
                return R.style.AppTheme_Dark;
            default:
                return 0;
        }
    }

    public static void changeTheme(String theme) {
        currentTheme = theme;
    }

    public static void onActivityCreateSetTheme(Activity activity) {
        activity.setTheme(getCurrentTheme());
    }

}
