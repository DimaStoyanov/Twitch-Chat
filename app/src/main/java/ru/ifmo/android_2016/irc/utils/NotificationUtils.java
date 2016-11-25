package ru.ifmo.android_2016.irc.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.UiThread;

import ru.ifmo.android_2016.irc.NewChannelListActivity;
import ru.ifmo.android_2016.irc.R;

/**
 * Created by ghost on 11/25/2016.
 */

public class NotificationUtils {
    public static final int FOREGROUND_NOTIFICATION = 1;
    public static final int MENTION_NOTIFICATION = 2;

    private NotificationUtils() {
    }

    @SuppressWarnings("deprecation")
    @UiThread
    public static Notification getNotification(Context context, String text) {
        return new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("IRC client")
                .setContentText(text)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        0,
                        new Intent(context, NewChannelListActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .getNotification();
    }

    public static void updateNotification(Context context, String text) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(1, getNotification(context, text));
    }
}
