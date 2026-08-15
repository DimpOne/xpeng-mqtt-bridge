package com.schwoi.xpengbridge;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public final class PahoMqttTransport implements MqttTransport {
    private MqttClient client;

    public static MqttConnectionSpec spec(MqttSettings s) {
        MqttConnectOptions o = new MqttConnectOptions();
        o.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        o.setCleanSession(true);
        o.setAutomaticReconnect(true);
        o.setConnectionTimeout(15);
        o.setKeepAliveInterval(30);
        o.setWill(s.baseTopic + "/availability", "offline".getBytes(StandardCharsets.UTF_8), 1, true);
        if (!s.username.isEmpty()) o.setUserName(s.username);
        if (!s.password.isEmpty()) o.setPassword(s.password.toCharArray());
        return new MqttConnectionSpec((s.tls ? "ssl" : "tcp") + "://" + s.host + ":" + s.port, o);
    }

    @Override
    public void connect(MqttSettings s) throws Exception {
        s.validate();
        MqttConnectionSpec spec = spec(s);
        client = new MqttClient(spec.serverUri,
                "xpeng-bridge-" + UUID.randomUUID().toString().substring(0, 8),
                new MemoryPersistence());
        client.connect(spec.options);
    }

    @Override
    public void publish(String topic, String payload, int qos, boolean retained) throws Exception {
        MqttMessage m = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        m.setQos(qos);
        m.setRetained(retained);
        client.publish(topic, m);
    }

    @Override
    public void close() throws Exception {
        if (client != null && client.isConnected()) client.disconnect();
        if (client != null) client.close();
    }
}
