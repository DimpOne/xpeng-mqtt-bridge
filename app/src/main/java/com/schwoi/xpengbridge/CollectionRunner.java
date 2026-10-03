package com.schwoi.xpengbridge;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import java.time.Instant;
import java.util.List;

public final class CollectionRunner {
    private static final String TAG = "CollectionRunner";

    private CollectionRunner() {}

    @SuppressWarnings("deprecation")
    public static Telemetry collectAndPublish(Context context) throws Exception {
        PowerManager pm = context.getSystemService(PowerManager.class);
        PowerManager.WakeLock wake = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "xpengbridge:collection");
        wake.acquire(45000);
        try {
            long seq = CollectionBus.sequence();
            if (!XpengAccessibilityService.requestXpengLaunch())
                throw new IllegalStateException(
                        "Enable XPENG MQTT Bridge in Accessibility settings before collecting");
            List<UiNode> nodes = CollectionBus.awaitReadyAfter(seq, 30000);
            if (nodes == null)
                throw new IllegalStateException(
                        "Timed out waiting for the XPENG dashboard. Keep the phone unlocked.");
            Telemetry telemetry = new TelemetryParser().parse(nodes, Instant.now());
            publishConfigured(context, telemetry);
            return telemetry;
        } finally {
            if (wake.isHeld()) wake.release();
        }
    }

    public static void publishTest(Context context) throws Exception {
        Telemetry telemetry = new Telemetry(Instant.now().toString(), 73, 321, "km", "WLTP",
                false, false, true, 80, 21, 0, "local test", "Not charging",
                "Generated locally", "fresh");
        publishConfigured(context, telemetry);
    }

    static void publishConfigured(Context context, Telemetry telemetry) throws Exception {
        List<MqttSettings> brokers = new SettingsRepository(context).loadAll();
        Exception firstFailure = null;
        int published = 0;

        for (MqttSettings broker : brokers) {
            try {
                new MqttPublisher(new PahoMqttTransport()).publishTelemetry(broker, telemetry);
                published++;
            } catch (Exception e) {
                if (firstFailure == null) firstFailure = e;
            }
        }

        if (published == 0 && firstFailure != null) throw firstFailure;
        if (published < brokers.size()) {
            Log.w(TAG, "Telemetry published to " + published + " of " + brokers.size()
                    + " configured MQTT brokers");
        }
    }
}
