package ru.ifmo.android_2016.irc.utils;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
     *
     * @param path_p Путь до предка файла
     * @param path   Путь до файла
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

    @SuppressWarnings("unchecked")
    public static <T> T readObject(String filename) {
        ObjectInputStream is = null;
        try {
            is = new ObjectInputStream(new FileInputStream(filename));
            return (T) is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static <T> void writeObject(String filename, T object) {
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new FileOutputStream(filename));
            os.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
