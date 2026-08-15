package com.schwoi.xpengbridge;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public final class CollectionScheduler {
    private CollectionScheduler() {}

    private static Constraints network() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    public static void schedule(Context context, int minutes) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                CollectionWorker.class, Math.max(15, minutes), TimeUnit.MINUTES)
                .setConstraints(network())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "xpeng-telemetry", ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void scheduleTest(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CollectionWorker.class)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(network())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "xpeng-automation-test", ExistingWorkPolicy.REPLACE, request);
    }

    public static void requestRefresh(Context context) {
        Data data = new Data.Builder().putBoolean("retry_on_failure", false).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(CollectionWorker.class)
                .setInputData(data)
                .setConstraints(network())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "xpeng-mqtt-refresh", ExistingWorkPolicy.REPLACE, request);
    }
}
