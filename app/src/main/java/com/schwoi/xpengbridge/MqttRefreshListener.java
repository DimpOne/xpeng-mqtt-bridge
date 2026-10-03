package com.schwoi.xpengbridge;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
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
    private final List<MqttClient> clients = new ArrayList<>();
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
        if (closed) return;

        SettingsRepository repo = new SettingsRepository(context);
        List<MqttSettings> brokers = repo.loadAll();
        synchronized (clients) {
            if (clients.size() == brokers.size()
                    && !clients.isEmpty()
                    && clients.stream().allMatch(MqttClient::isConnected)) {
                return;
            }
        }

        disconnect();
        int index = 0;
        for (MqttSettings mqtt : brokers) {
            MqttClient next = null;
            try {
                mqtt.validate();
                MqttConnectOptions options = new MqttConnectOptions();
                options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
                options.setCleanSession(true);
                options.setConnectionTimeout(10);
                options.setKeepAliveInterval(30);
                if (!mqtt.username.isEmpty()) options.setUserName(mqtt.username);
                if (!mqtt.password.isEmpty()) options.setPassword(mqtt.password.toCharArray());

                String uri = (mqtt.tls ? "ssl" : "tcp") + "://" + mqtt.host + ":" + mqtt.port;
                final MqttSettings broker = mqtt;
                next = new MqttClient(uri,
                        "xpeng-refresh-" + repo.clientSuffix() + "-" + index,
                        new MemoryPersistence());
                next.setCallback(new MqttCallback() {
                    @Override public void connectionLost(Throwable cause) { }

                    @Override public void messageArrived(String topic, MqttMessage message) {
                        if (RefreshCommand.matches(broker, topic, message.getPayload())) {
                            CollectionScheduler.requestRefresh(context);
                        }
                    }

                    @Override public void deliveryComplete(IMqttDeliveryToken token) { }
                });
                next.connect(options);
                next.subscribe(RefreshCommand.topic(mqtt), 1);
                synchronized (clients) {
                    clients.add(next);
                }
            } catch (Exception e) {
                Log.w(TAG, "Refresh listener connect failed for broker "
                        + (index + 1) + ": " + e.getClass().getSimpleName());
                if (next != null) {
                    try { next.close(); } catch (Exception ignored) { }
                }
            }
            index++;
        }
    }

    private void disconnect() {
        List<MqttClient> current;
        synchronized (clients) {
            current = new ArrayList<>(clients);
            clients.clear();
        }
        for (MqttClient client : current) {
            try { if (client.isConnected()) client.disconnect(); } catch (Exception ignored) { }
            try { client.close(); } catch (Exception ignored) { }
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdownNow();
        disconnect();
    }
}
