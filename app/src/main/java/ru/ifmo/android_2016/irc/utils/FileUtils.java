package ru.ifmo.android_2016.irc.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * * Created by Dima Stoyanov on 23.10.2016.
 * Project Android-IRC
 * Start time : 22:45
 */

public final class FileUtils {
    /**
     * Класс, реализующий операции в файле. Данные в файле хранятся в следующем виде
     * Каждый набор данных для подключения (server, nick, password, channel)
     * хранится в отдельной строке. Разделяется с помощью DELIM.
     */

    // Символ который никогда не встречается в запросах
    private final static String TAG = FileUtils.class.getSimpleName();


    /**
     * Создает файл с полным путем path, если директории предка не существует, создает ее, а потом необходимый файл
     * @param path_p Путь до предка файла
     * @param path Путь до файла
     * @return File, созданный в path.
     * @throws IOException
     */
    public static File createFile(@NonNull String path_p, @NonNull String path) throws IOException {
        File dir = new File(path_p);
        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir);
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
        File result = new File(path);
        if (!result.createNewFile()) {
            throw new IOException("Can't create a file " + result.getPath());
        }
        Log.d(TAG, "File " + result.getPath() + " created");
        return result;
    }

    public static boolean deleteDirectory(File f) {
        if (!f.exists()) return false;
        if (f.isFile())
            return f.delete();
        if (f.isDirectory()) {
            File[] subfs = f.listFiles();
            for (File t : subfs) {
                deleteDirectory(t);
            }
            return f.delete();
        }
        return false;
    }

}
