package irc.android_2016.ifmo.ru.irc.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import irc.android_2016.ifmo.ru.irc.model.LoginData;

/**
 * Created by Dima Stoyanov on 23.10.2016.
 * Project Android-IRC
 * Start time : 22:45
 */

public class FileUtils {
    /**
     * Класс, реализующий операции в файле. Данные в файле хранятся в следующем виде
     * Каждый набор данных для подключения (server, nick, password, channel)
     * Хранится в отдельной строке. Разделяется с помощью DELIM.
     */

    // Символ который никогда не встречается в запросах
    private final static String DELIM = "↨";

    /**
     * Создает временный пустой файл в папке приложения в External Storage
     * Дтректория: /sdcard/Android/data/<application_package_name>/files/login_data
     * <p>
     * Имя файла генерируется случайным образом, к нему можно добавить расширение. Файл никак
     * автоматически не удаляется -- получатель сам должен позаботиться об удалении после
     * использования.
     *
     * @param context   контекст приложения
     * @param extension расширение, которое будет добавлено в конце имени файла.
     * @return новый пустой файл
     * @throws IOException в случае ошибки создания файла.
     */
    private static File createExternalFile(Context context, String extension) throws IOException {
        File dir = new File(context.getExternalFilesDir(null), "login_data");
        if (dir.exists() && !dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir);
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + dir);
        }
        return File.createTempFile("login_data", extension, dir);
    }

    /**
     * @param context Контекст
     * @return Возвращает путь созданного файла
     * @throws IOException
     */
    public static String getPathOfExternalFile(Context context) throws IOException {
        return createExternalFile(context, "txt").getPath();
    }

    /**
     * Добавляет в конец файла данные о логине. Разделяет данные с помощью DELIM.
     *
     * @param path Путь до файла
     * @param data Данные, которые нужно записать
     */
    public static void addData(String path, LoginData data) {
        File f;
        try {
            f = new File(path);
            FileWriter out = new FileWriter(f, true);
            int id = Integer.parseInt(getLastId(path)) + 1;
            out.write(getDataString(String.valueOf(id), data.server, data.nick, data.password, data.channel));
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Can't save data to storage");
        }
    }

    /**
     * Генерирует строку из данных лоигина. Разделяет поля с помощью DELIM.
     *
     * @param data Данные, которые нужно записать в файл
     * @return Сгенерированная строка для записи в файл
     */
    public static String getDataString(String... data) {
        StringBuilder result = new StringBuilder();
        for (String s : data) {
            result.append(s).append(DELIM);
        }
        result.deleteCharAt(result.length() - 1);
        result.append("\n");
        return result.toString();
    }


    /**
     * Функция, которая считывает последнюю строку файла, и вычисляет последний записанный id.
     * Реализована на основе RandomAccessReader.
     * Пытается считать 2 строки с конца, иначе уменьшает индекс.
     *
     * @param f Файл, в котором ищется
     * @return id строки данных в файле.
     * @throws IOException
     */
    private static String getLastId(File f) throws IOException {
        RandomAccessFile reader = new RandomAccessFile(f, "r");
        String result = null;
        long startIndex = f.length();
        while (startIndex >= 0 && (result == null || result.length() == 0)) {
            reader.seek(startIndex);
            if (startIndex > 0)
                reader.readLine();
            result = reader.readLine();
            startIndex--;
        }
        reader.close();
        return result == null ? "0" : result.substring(0, result.indexOf(DELIM));
    }

    private static String getLastId(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String temp, result = null;
        while ((temp = reader.readLine()) != null) {
            result = temp;
        }
        return result == null ? "0" : result.substring(0, result.indexOf(DELIM));
    }

    /**
     * Считывает файл и возвращает список данных.
     *
     * @param path Путь до файла
     * @return список данных о логине
     */
    public static List<LoginData> getData(String path) {
        List<LoginData> result = new ArrayList<>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line;
            StringTokenizer tokenizer;
            while ((line = in.readLine()) != null) {
                tokenizer = new StringTokenizer(line, DELIM);
                result.add(new LoginData(tokenizer));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Удаляет данные в строке с заданным id.
     * Создает временный файл, считывает все строчки из исходного
     * И записывает все, кроме строчки с заданным id в временный файл
     * Переименовывает временный файл в исходный.
     *
     * @param context Контекст
     * @param path    Путь до файла
     * @param id      id строки, которую нужно удалить
     */
    public static void deleteData(Context context, String path, String id) {
        try {
            File source_file = new File(path);
            File result_file = createExternalFile(context, "txt");
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
            if (!result_file.renameTo(source_file)) {
                throw new RuntimeException("Can't rename file");
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't open file");
        }
    }
}
