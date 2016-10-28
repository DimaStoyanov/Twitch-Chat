package irc.android_2016.ifmo.ru.irc.model;

import android.graphics.Bitmap;
import android.media.Image;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;

import irc.android_2016.ifmo.ru.irc.utils.FileUtils;

/**
 * Created by Dima Stoyanov on 24.10.2016.
 * Project Android-IRC
 * Start time : 22:55
 */

public class TwitchEmoticon {
    /**
     * Регулярное выражение, по которому находится emoticon
     */
    public final
    @NonNull
    String regex;


    /**
     * Является ли регулярное выражение состоящим только из латинских букв => может подойти только 1 слово
     */
    public boolean isAlphabetic;


    /**
     * Путь картинки emoticon
     */
    public final
    @NonNull
    String emoticon_url;


    public TwitchEmoticon(@NonNull String regex, @NonNull String emoticon_url) {
        this.regex = regex;
        this.emoticon_url = emoticon_url;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if ((c < 'A' || c > 'Z' && c < 'a' || c > 'z')) {
                this.isAlphabetic = false;
            }
        }
        this.isAlphabetic = true;
    }

    @Override
    public String toString() {
        return FileUtils.getDataString(regex, String.valueOf(isAlphabetic), emoticon_url);
    }
}


