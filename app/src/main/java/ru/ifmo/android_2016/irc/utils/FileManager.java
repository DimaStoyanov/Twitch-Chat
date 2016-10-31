package ru.ifmo.android_2016.irc.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ru.ifmo.android_2016.irc.constant.FilePathConstant;

import static ru.ifmo.android_2016.irc.utils.ObjectUtils.deserialize;
import static ru.ifmo.android_2016.irc.utils.ObjectUtils.serialize;

/**
 * Created by Dima Stoyanov on 31.10.2016.
 * Project Android-IRC
 * Start time : 12:37
 */

public class FileManager {
    private String path;
    private FilePathConstant constant;


    /**
     * Type of file manager. It depends on file path and some methods
     */
    public enum FileType {
        Login
    }


    /**
     * File manager can store data at external storage and modify it using serializable
     *
     * @param type     тип Файлового мэнэджера.
     * @param constant Константы - пути до внутреннего хранилища.
     */
    public FileManager(FileType type, FilePathConstant constant) {
        switch (type) {
            case Login:
                path = constant.LOGIN_DATA;
                break;
        }
        this.constant = constant;
    }

    /**
     * Adding new object that store in storage
     *
     * @param savingData Данные, которые нужно добавить
     * @return Все сохранненые в файлы данные
     * @throws IOException
     */
    @Nullable
    public Object addData(@NonNull Object savingData) throws IOException {
        ArrayList<Object> datas = (ArrayList<Object>) getData();
        File f = new File(path);
        if (!f.exists()) {
            FileUtils.createFile(constant.LOGIN_PACKAGE, path);
        }
        if (!f.isFile() && !f.createNewFile()) {

            throw new IOException("Can't create file" + f.getPath());
        }


        // If data like this is already stored, we didn't add it
        if (datas != null) {
            for (Object cur : datas)
                if (savingData.equals(cur))
                    return datas;
        } else {
            datas = new ArrayList<>();
        }
        datas.add(savingData);
        writeData(datas);
        return datas;
    }

    /**
     * Позволяет получить данные из файла и десериализовать их.
     *
     * @return Все хранящиеся в файле данные
     */
    @Nullable
    public Object getData() {
        Object result = null;
        try {
            File f = new File(path);
            if (!f.isFile())
                return null;
            FileInputStream inputStream = new FileInputStream(path);
            byte[] byteData = new byte[(int) f.length()];
            if (inputStream.read(byteData, 0, (int) f.length()) != f.length()) {
                throw new IOException("Can't read all bytes, read -" + inputStream.read(byteData, 0, (int) f.length()) + " bytes");
            }
            result = deserialize(byteData);
            inputStream.close();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Удаляет выбранные данные из файла.
     *
     * @param deletingData Объект данных, которые нужно удалить.
     * @return Все данные, хранящиеся в файле.
     * @throws IOException Если файла не сущесвует или он не хранит необходимых данных.
     */
    @NonNull
    public Object deleteData(@NonNull Object deletingData) throws IOException {
        ArrayList<Object> datas = (ArrayList<Object>) getData();
        if (datas == null)
            throw new IOException("Can't delete data from null");
        if (!datas.contains(deletingData))
            throw new IOException("No such data" + deletingData.toString());
        datas.remove(deletingData);
        writeData(datas);
        return datas;
    }

    /**
     * Позволяет изменить данные, хранящиеся во внутренней памяти
     *
     * @param editingData Данные, которые  нужно заменить.
     * @param eddiedData  Данные которыми нужно заменить.
     * @return Все данные хранящиеся в файле
     * @throws IOException Если файла не сущесвтует или данные, которые нужно изменить отсутсвуют
     */
    @NonNull
    public Object editData(@NonNull Object editingData, @NonNull Object eddiedData) throws IOException {
        ArrayList<Object> datas = (ArrayList<Object>) getData();
        if (datas == null)
            throw new IOException("Can't get data's from file");
        if (!datas.contains(editingData))
            throw new IOException("No such data" + editingData.toString());
        datas.set(datas.indexOf(editingData), eddiedData);
        writeData(datas);
        return datas;
    }

    private void writeData(Object data) throws IOException {
        File f = new File(path);
        if (!f.delete() && !f.createNewFile()) {
            throw new IOException("Can't add data");
        }
        FileOutputStream outputStream = new FileOutputStream(f);
        byte[] byteData = serialize(data);
        outputStream.write(byteData, 0, byteData.length);
        outputStream.close();
    }


    private final String TAG = FileManager.class.getSimpleName();

}
