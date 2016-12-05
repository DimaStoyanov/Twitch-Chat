package ru.ifmo.android_2016.irc.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import ru.ifmo.android_2016.irc.R;

/**
 * Created by ghost on 11/25/2016.
 */

public class NotificationUtils {
    public static final int FOREGROUND_NOTIFICATION = 1;
    public static final int HIGHLIGHT_NOTIFICATION = 2;

    private static NotificationManagerCompat notificationManager = null;
    private static long[] vibrationPattern = new long[]{50, 50, 50, 50, 50, 50, 50, 50, 50, 50};

    private NotificationUtils() {
    }

    private static NotificationManagerCompat getNotificationManager(Context context) {
        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(context);
        }
        return notificationManager;
    }

    public static Notification getNotification(Context context, String title, String text) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(vibrationPattern)
                .build();
    }

    public static Notification getNotification(Context context,
                                               String title,
                                               String text,
                                               Intent onClick) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(vibrationPattern)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        0,
                        onClick,
                        PendingIntent.FLAG_UPDATE_CURRENT
                ))
                .build();
    }

    public static NotificationCompat.Builder getNotificationBuilder(Context context) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher);
    }

    public static void sendNotification(Context context,
                                        int notificationId,
                                        Notification notification) {
        getNotificationManager(context);

        notificationManager.notify(notificationId, notification);
    }
}
