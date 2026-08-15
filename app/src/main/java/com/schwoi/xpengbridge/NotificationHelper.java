package com.schwoi.xpengbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;

public final class NotificationHelper {
    public static final String CHANNEL = "collection";
    public static final int ID = 4107;

    private NotificationHelper() {}

    public static Notification notification(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL, "XPENG collection", NotificationManager.IMPORTANCE_LOW));
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Collecting XPENG telemetry")
                .setContentText("Waiting for read-only accessibility data")
                .setOngoing(true)
                .setContentIntent(pending)
                .build();
    }

    public static ForegroundInfo foreground(Context context) {
        return new ForegroundInfo(ID, notification(context), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    }
}
