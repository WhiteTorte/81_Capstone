package com.mtsahakis.mediaprojectiondemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;

public class NotificationUtils {

    public static final int NOTIFICATION_ID = 1337;
    private static final String NOTIFICATION_CHANNEL_ID = "com.mtsahakis.mediaprojectiondemo.app";
    private static final String NOTIFICATION_CHANNEL_NAME = "Screen Capture Notification";

    private Notification notification = new Notification();
    public static Pair<Integer, Notification> getNotification(@NonNull Context context, ScreenCaptureService service) {
        createNotificationChannel(context);
        Notification notification = createNotification(context, service);
        NotificationManager notificationManager
                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
        return new Pair<>(NOTIFICATION_ID, notification);
    }

    private static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private static Notification createNotification(@NonNull Context context, ScreenCaptureService service) {

        Intent intent = new Intent(context, service.getClass());
        intent.putExtra("from_notification", true);
        intent.putExtra("ACTION", "START");
        intent.putExtra("RESULT_CODE", -1);
        intent.putExtra("DATA", new Intent());

        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            pendingIntent = PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_camera)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Tap to capture screen")
                .setContentIntent(pendingIntent)
                .setAutoCancel(false) // 변경된 부분
                .build();
    }
}
