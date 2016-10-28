package ru.ifmo.android_2016.irc.constant;

import android.content.Context;

import java.io.File;

/**
 * Created by Dima Stoyanov on 25.10.2016.
 * Project Android-IRC
 * Start time : 22:59
 */

public class FilePathConstant {

    /**
     * Путь до файла, хранящего данные для входа на каналы.
     */
    public final String LOGIN_DATA;

    /**
     * Название директории внутреннего накопителя, в которой хранятся данные.
     */
    public final String LOGIN_PACKAGE;

    /**
     * Путь до директории, хранящей данные о Twitch emoticons
     */
    public final String EMOTICONS_PACKAGE;


    /**
     * Название файла, хранящего информацию о эмоциях канала
     */
    public final String EMOTICON_INFO_NAME;


    /**
     *  Расширение файла, хранящего информацию о эмоциях канала
     */
    public final String EMOTICON_INFO_EXTENSION;


    /**
     * Название директории на карточке, где храняться данные эмоций
     */
    public final String EMOTION_PACKAGE_NAME;

    public FilePathConstant(Context context) {
        LOGIN_DATA = context.getExternalFilesDir(null) + File.separator + "data" + File.separator + "login.bin";
        EMOTICONS_PACKAGE = context.getExternalFilesDir(null) + File.separator + "emoticons";
        EMOTICON_INFO_NAME = "info";
        LOGIN_PACKAGE = "data";
        EMOTICON_INFO_EXTENSION = "bin";
        EMOTION_PACKAGE_NAME = "emoticons";
    }

}
