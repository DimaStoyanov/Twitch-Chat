package ru.ifmo.android_2016.irc.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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


    /**
     * Callback интерфейс для получения уведомления о прогрессе.
     */
    public interface ProgressCallback {

        /**
         * Вызывается при изменении значения прогресса.
         *
         * @param progress новое значение прогресса от 0 до 100.
         */
        void onProgressChanged(int progress);
    }

    /**
     * Выполняет сетевой запрос для скачивания файла, и сохраняет ответ в указанный файл.
     *
     * @param context          Контекст активити.
     * @param downloadUrl      URL - откуда скачивать (http:// или https://)
     * @param package_name     В какую папку сохранить файл.
     * @param path             Путь создаваемого файла
     * @param extension        Расширение файла.
     * @param progressCallback опциональный callback для уведомления о прогрессе скачивания
     *                         файлы. Его метод onProgressChanged вызывается синхронно
     *                         в текущем потоке.
     * @throws IOException В случае ошибки выполнения сетевого запроса или записи файла.
     */
    public static String downloadFile(@NonNull Context context,
                                      @NonNull String downloadUrl,
                                      @NonNull String package_name,
                                      @NonNull String path,
                                      @NonNull String extension,
                                      @Nullable ProgressCallback progressCallback) throws IOException {
        Log.d(TAG, "Start downloading url: " + downloadUrl);
        Log.d(TAG, "Saving to package: " + package_name);
        File destFile = FileUtils.createFile(package_name, path);
        StethoURLConnectionManager stethoManager = new StethoURLConnectionManager("Download");

        // Выполняем запрос по указанному урлу. Поскольку мы используем только http:// или https://
        // урлы для скачивания, мы привести результат к HttpURLConnection. В случае урла с другой
        // схемой, будет ошибка.
        Uri uri = Uri.parse(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
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
            byte[] buffer = new byte[1024 * 8];

            // Размер полученной порции в байтах
            int receivedBytes;
            // Сколько байт всего получили (и записали).
            int receivedLength = 0;
            // прогресс скачивания от 0 до 100
            int progress = 0;

            // Начинаем читать ответ
            in = conn.getInputStream();
            in = stethoManager.interpretResponseStream(in);
            // И открываем файл для записи
            out = new FileOutputStream(destFile);

            // В цикле читаем данные порциями в буффер, и из буффера пишем в файл.
            // Заканчиваем по признаку конца файла -- in.read(buffer) возвращает -1
            while ((receivedBytes = in.read(buffer)) >= 0) {
                out.write(buffer, 0, receivedBytes);
                receivedLength += receivedBytes;

                if (contentLength > 0) {
                    int newProgress = 100 * receivedLength / contentLength;
                    if (newProgress > progress && progressCallback != null) {
                        Log.d(TAG, "Downloaded " + newProgress + "% of " + contentLength + " bytes");
                        progressCallback.onProgressChanged(newProgress);
                    }
                    progress = newProgress;
                }
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
        return destFile.getPath();
    }

    /**
     * Скачивает данные по указнному url и сохраняет в новый файл, находящийся в External Storage в директории package_name
     * Допускается name == null, тогда имя файла генерируется автоматически.
     *
     * @param context      Контекст активти
     * @param downloadUrl  Ссылка на данные
     * @param package_name Адрес директории для сохранения файла
     * @param name         Имя файла
     * @return Возвращает путь к сохранненому на внешней карточке файлу
     * @throws IOException
     */
    public static String downloadFile(@NonNull Context context,
                                      @NonNull String downloadUrl,
                                      @NonNull String package_name,
                                      @Nullable String name,
                                      @NonNull String extension) throws IOException {
        return downloadFile(context, downloadUrl, package_name, name, extension, null);
    }

    private static final String TAG = "Download";


    private DownloadUtils() {
    }

    /**
     * Декодирует изображение.
     *
     * @param path Путь до файла, который нужно декодировать
     * @return Объект декодированного изображения
     */
    @Nullable
    public static Bitmap decodeImage(@NonNull String path) {
        return BitmapFactory.decodeFile(path);
    }


}