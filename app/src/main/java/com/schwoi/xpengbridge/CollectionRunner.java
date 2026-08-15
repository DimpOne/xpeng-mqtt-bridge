package com.schwoi.xpengbridge;

import android.content.Context;
import android.os.PowerManager;
import java.time.Instant;
import java.util.List;

public final class CollectionRunner {
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
            new MqttPublisher(new PahoMqttTransport())
                    .publishTelemetry(new SettingsRepository(context).load(), telemetry);
            return telemetry;
        } finally {
            if (wake.isHeld()) wake.release();
        }
    }

    public static void publishTest(Context context) throws Exception {
        Telemetry telemetry = new Telemetry(Instant.now().toString(), 73, 321, "km", "WLTP",
                false, false, true, 80, 21, 0, "local test", "Not charging",
                "Generated locally", "fresh");
        new MqttPublisher(new PahoMqttTransport())
                .publishTelemetry(new SettingsRepository(context).load(), telemetry);
    }
}
