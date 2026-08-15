package com.schwoi.xpengbridge;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ManualCollectionService extends Service {
    public static final String ACTION = "com.schwoi.xpengbridge.COLLECTION_RESULT";
    public static final String EXTRA_OK = "ok";
    public static final String EXTRA_MESSAGE = "message";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NotificationHelper.ID, NotificationHelper.notification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else
            startForeground(NotificationHelper.ID, NotificationHelper.notification(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executor.execute(() -> {
            try {
                Telemetry t = CollectionRunner.collectAndPublish(this);
                send(true, "Published telemetry collected at " + t.collectorTimestamp);
            } catch (Exception e) {
                send(false, safe(e));
            } finally {
                stopSelf(startId);
            }
        });
        return START_NOT_STICKY;
    }

    private String safe(Exception e) {
        String message = e.getMessage();
        return message == null ? "Collection failed" : message;
    }

    private void send(boolean ok, String message) {
        Intent intent = new Intent(ACTION)
                .setPackage(getPackageName())
                .putExtra(EXTRA_OK, ok)
                .putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
