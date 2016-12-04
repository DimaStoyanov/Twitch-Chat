package ru.ifmo.android_2016.irc.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.stetho.urlconnection.StethoURLConnectionManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Методы для скачивания файлов.
 */
public final class DownloadUtils {

    private static String TAG = DownloadUtils.class.getSimpleName();

    public static void downloadFile(@NonNull HttpURLConnection conn,
                                    @NonNull File destFile) throws IOException {
        if (!destFile.setLastModified(conn.getLastModified())) {
            Log.d(TAG, "Can't set last modified");
        }
        Log.d(TAG, "Saving to file: " + destFile);

        StethoURLConnectionManager stethoManager = new StethoURLConnectionManager("Download");

        // Выполняем запрос по указанному урлу. Поскольку мы используем только http:// или https://
        // урлы для скачивания, мы привести результат к HttpURLConnection. В случае урла с другой
        // схемой, будет ошибка.


        stethoManager.preConnect(conn, null);

        InputStream in = null;
        OutputStream out = null;

        try {

            // Проверяем HTTP код ответа. Ожидаем только ответ 200 (ОК).
            // Остальные коды считаем ошибкой.
            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Received HTTP response code: " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Unexpected HTTP response: " + responseCode
                        + ", " + conn.getResponseMessage());
            }

            // Узнаем размер файла, который мы собираемся скачать
            // (приходит в ответе в HTTP заголовке Content-Length)
            int contentLength = conn.getContentLength();
            Log.d(TAG, "Content Length: " + contentLength);

            stethoManager.postConnect();

            // Создаем временный буффер для I/O операций размером 8кб
            byte[] buffer = new byte[1 << 13];

            // Размер полученной порции в байтах
            int receivedBytes;
            // Сколько байт всего получили (и записали).
            int receivedLength = 0;

            // Начинаем читать ответ
            in = conn.getInputStream();
            in = stethoManager.interpretResponseStream(in);
            // И открываем файл для записи
            out = new FileOutputStream(destFile);
            // В цикле читаем данные порциями в буффер, и из буффера пишем в файл.
            // Заканчиваем по признаку конца файла -- in.read(buffer) возвращает -1
            while ((receivedBytes = in.read(buffer)) >= 0) {
                out.write(buffer, 0, receivedBytes);

            }

            if (receivedLength != contentLength) {
                Log.w(TAG, "Received " + receivedLength + " bytes, but expected " + contentLength);
            } else {
                Log.d(TAG, "Received " + receivedLength + " bytes");
            }

        } catch (IOException e) {
            // Ловим ошибку только для отладки, кидаем ее дальше
            stethoManager.httpExchangeFailed(e);
            throw e;

        } finally {
            // Закрываем все потоки и соедиениние
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close HTTP input stream: " + e, e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close file: " + e, e);
                }
            }
            conn.disconnect();
        }

    }

    /**
     * Выполняет сетевой запрос для скачивания файла, и сохраняет ответ в указанный файл.
     *
     * @param downloadUrl      URL - откуда скачивать (http:// или https://)
     * @param destFile         файл, в который сохранять.
     * @throws IOException В случае ошибки выполнения сетевого запроса или записи файла.
     */
    public static void downloadFile(@Nullable String downloadUrl,
                                    File destFile) throws IOException {
        if (downloadUrl == null) {
            throw new IOException("Null url");
        }
        Log.d(TAG, "Start downloading url: " + downloadUrl);
        try {
            downloadFile((HttpURLConnection) new URL(downloadUrl).openConnection(), destFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}