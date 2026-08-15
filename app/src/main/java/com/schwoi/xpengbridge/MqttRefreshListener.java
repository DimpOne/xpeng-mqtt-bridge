package com.schwoi.xpengbridge;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public final class MqttRefreshListener implements AutoCloseable {
    private static final String TAG = "MqttRefreshListener";
    private final Context context;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile MqttClient client;
    private volatile boolean closed;

    public MqttRefreshListener(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start() {
        executor.scheduleWithFixedDelay(this::ensureConnected, 0, 30, TimeUnit.SECONDS);
    }

    public void restart() {
        executor.execute(() -> {
            disconnect();
            ensureConnected();
        });
    }

    private void ensureConnected() {
        if (closed || (client != null && client.isConnected())) return;
        disconnect();
        MqttClient next = null;
        try {
            SettingsRepository repo = new SettingsRepository(context);
            MqttSettings mqtt = repo.load();
            mqtt.validate();
            MqttConnectOptions options = new MqttConnectOptions();
            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            if (!mqtt.username.isEmpty()) options.setUserName(mqtt.username);
            if (!mqtt.password.isEmpty()) options.setPassword(mqtt.password.toCharArray());
            String uri = (mqtt.tls ? "ssl" : "tcp") + "://" + mqtt.host + ":" + mqtt.port;
            next = new MqttClient(uri, "xpeng-refresh-" + repo.clientSuffix(), new MemoryPersistence());
            next.setCallback(new MqttCallback() {
                @Override public void connectionLost(Throwable cause) { }

                @Override public void messageArrived(String topic, MqttMessage message) {
                    if (RefreshCommand.matches(mqtt, topic, message.getPayload())) {
                        CollectionScheduler.requestRefresh(context);
                    }
                }

                @Override public void deliveryComplete(IMqttDeliveryToken token) { }
            });
            next.connect(options);
            next.subscribe(RefreshCommand.topic(mqtt), 1);
            client = next;
        } catch (Exception e) {
            // Never log settings or credentials; the class name is enough for triage.
            Log.w(TAG, "Refresh listener connect failed: " + e.getClass().getSimpleName());
            if (next != null) {
                try { next.close(); } catch (Exception ignored) { }
            }
        }
    }

    private void disconnect() {
        MqttClient current = client;
        client = null;
        if (current == null) return;
        try { if (current.isConnected()) current.disconnect(); } catch (Exception ignored) { }
        try { current.close(); } catch (Exception ignored) { }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        disconnect();
    }
}
