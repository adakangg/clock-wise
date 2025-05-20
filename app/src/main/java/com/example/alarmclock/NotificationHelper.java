package com.example.alarmclock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

/**
 * This utility class creates the notification channel used to handle alarm triggers.
 */
public class NotificationHelper {
    public static String CHANNEL_ID  = "alarms";
    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Alarm Notifications", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notification Channel for Alarm Clock App");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }
}