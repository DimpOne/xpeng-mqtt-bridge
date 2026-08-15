package com.schwoi.xpengbridge;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class CollectionWorker extends Worker {
    private static final String TAG = "CollectionWorker";

    public CollectionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setForegroundAsync(NotificationHelper.foreground(getApplicationContext())).get();
        } catch (Exception e) {
            // Android can deny foreground promotion from the background; collect anyway.
            Log.w(TAG, "Foreground promotion failed: " + e.getClass().getSimpleName());
        }
        try {
            CollectionRunner.collectAndPublish(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            return getInputData().getBoolean("retry_on_failure", true) ? Result.retry() : Result.failure();
        }
    }
}
