package ru.ifmo.android_2016.irc.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import ru.ifmo.android_2016.irc.model.LoginData;
import ru.ifmo.android_2016.irc.model.TwitchEmoticon;

/**
 * Created by Dima Stoyanov on 23.10.2016.
 * Project Android-IRC
 * Start time : 22:45
 */

public class FileUtils {
    /**
     * Класс, реализующий операции в файле. Данные в файле хранятся в следующем виде
     * Каждый набор данных для подключения (server, nick, password, channel)
     * хранится в отдельной строке. Разделяется с помощью DELIM.
     */

    // Символ который никогда не встречается в запросах
    private final static String DELIM = "↨";


    /**
     * Создает пустой файл в папке приложения в External Storage
     * Дтректория: /sdcard/Android/data/<application_package_name>/files/<package_name>
     * <p>
     * Имя файла задается входящим параметром или генерируется автоматически, если name == null. Файл никак
     * автоматически не удаляется -- получатель сам должен позаботиться об удалении после
     * использования.
     *
     * @param context      Контекст приложения
     * @param package_name Имя папки, в которой нужно создать файл
     * @param name         Имя файла
     * @param extension    Разширение файла
     * @return новый пустой файл
     * @throws IOException в случае ошибки создания файла.
     */
    @NonNull
    public static File createExternalFile(@NonNull Context context,
                                          @NonNull String package_name,
                                          @Nullable String name,
                                          @Nullable String extension) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), package_name);

        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir);
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
        if (name == null)
            return File.createTempFile("tmp", extension, dir);
        File result = new File(dir, name + "." + extension);
        if (!result.createNewFile()) {
            Log.d("WTF", result.getPath() + "\n" + result.exists() + " " + result.isFile());
            throw new IOException("Can't create file " + name + extension + " at package " + package_name);
        }
        Log.d("IRC", "Created file " + result.getPath());
        return result;
    }


    /**
     * Добавляет в конец файла данные о логине. Разделяет данные с помощью DELIM.
     *
     * @param path Путь до файла
     * @param data Данные, которые нужно записать
     * @throws IOException В случае отсустсвтия возможности записи в файл.
     */
    public static void addLoginData(@NonNull String path,
                                    @NonNull LoginData data) throws IOException {
        File f = new File(path);
        FileWriter out = new FileWriter(f, true);
        int id = Integer.parseInt(getLastId(path)) + 1;
        out.write(getDataString(String.valueOf(id), data.server, data.nick, data.password, data.channel));
        out.close();
    }

    public static void addEmoticonData(@NonNull String path, @NonNull List<TwitchEmoticon> data) throws IOException {
        File f = new File(path);
        FileWriter out = new FileWriter(f);
        for (int i = 0; i < data.size(); i++) {
            out.write(data.get(i).toString());
        }
        out.close();
        Log.d("Emotion data file utils", "Data saved in file " + f.getPath());
    }

    /**
     * Генерирует строку из данных лоигина. Разделяет поля с помощью DELIM.
     *
     * @param data Данные, которые нужно записать в файл
     * @return Сгенерированная строка для записи в файл
     */
    @NonNull
    public static String getDataString(@NonNull String... data) {
        StringBuilder result = new StringBuilder();
        for (String s : data) {
            result.append(s).append(DELIM);
        }
        result.deleteCharAt(result.length() - 1);
        result.append("\n");
        return result.toString();
    }


    /**
     * Функция, которая считывает весь файл, и вычисляет последний записанный id.
     * Если файл пустой, возвращает 0.
     *
     * @param path Путь файла, в котором ищется
     * @return id строки данных в файле.
     * @throws IOException
     */
    @NonNull
    private static String getLastId(@NonNull String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String temp, result = null;
        while ((temp = reader.readLine()) != null) {
            result = temp;
        }
        return result == null ? "0" : result.substring(0, result.indexOf(DELIM));
    }

    /**
     * Считывает файл и возвращает список данных о логине.
     *
     * @param path Путь до файла
     * @return список данных о логине
     * @throws IOException Если нет доступа к чтению файла
     */
    @NonNull
    public static List<LoginData> getData(@NonNull String path) throws IOException {
        List<LoginData> result = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(path));
        String line;
        StringTokenizer tokenizer;
        while ((line = in.readLine()) != null) {
            tokenizer = new StringTokenizer(line, DELIM);
            result.add(new LoginData(tokenizer));
        }
        in.close();
        return result;
    }

    /**
     * Удаляет данные в строке с заданным id.
     * Создает временный файл, считывает все строчки из исходного
     * И записывает все, кроме строчки с заданным id в временный файл
     * Переименовывает временный файл в исходный.
     *
     * @param context      Контекст активти.
     * @param path         Путь до файла.
     * @param package_name Название директории в external storage.
     * @param extension    Расширение файла
     * @param id           id строки, которую нужно удалить
     * @throws IOException В случае отсутствия доступа на запись или чтения файла
     */
    public static void deleteData(@NonNull Context context,
                                  @NonNull String path,
                                  @NonNull String package_name,
                                  @NonNull String extension,
                                  @NonNull String id) throws IOException {
        try {
            File source_file = new File(path);
            File result_file = createExternalFile(context, package_name, null, extension);
            Log.d("FileUtils", "delete from " + source_file.isFile() + " temp " + result_file.isFile());
            BufferedReader reader = new BufferedReader(new FileReader(source_file));
            BufferedWriter writer = new BufferedWriter(new FileWriter(result_file));
            String line, current_id;
            while ((line = reader.readLine()) != null) {
                current_id = line.substring(0, line.indexOf(DELIM));
                if (!id.equals(current_id)) {
                    writer.write(line);
                }
            }
            writer.close();
            reader.close();
            if (!source_file.delete() && !result_file.renameTo(source_file)) {
                throw new RuntimeException("Can't rename file");
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't open file");
        }
    }

    private byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] result;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            result = bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                Log.d(TAG, "Can't close baos " + ex.getMessage());
            }
        }
        return result;
    }

    private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        Object result;
        try {
            in = new ObjectInputStream(bis);
            result = in.readObject();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Log.d(TAG, "Can't close input stream" + ex.getMessage());
            }
        }
        return result;
    }


    private final String TAG = FileUtils.class.getSimpleName();
}
