package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.time.Instant;
import java.util.*;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.Test;

public class MqttPublisherTest {
    static class Fake implements MqttTransport {
        List<String> topics = new ArrayList<>(), payloads = new ArrayList<>();
        List<Boolean> retained = new ArrayList<>();

        public void connect(MqttSettings s) { }

        public void publish(String topic, String payload, int qos, boolean isRetained) {
            topics.add(topic);
            payloads.add(payload);
            retained.add(isRetained);
        }

        public void close() { }
    }

    @Test public void publishesRetainedStateAvailabilityAndDiscovery() throws Exception {
        Fake f = new Fake();
        MqttSettings s = new MqttSettings("broker", 1883, false, "", "", "cars/xpeng", "homeassistant", 15);
        Telemetry t = new TelemetryParser().parse(Arrays.asList(
                new UiNode("x/tv_car_miles", "100"), new UiNode("x/title_charge_left_tv", "55%")),
                Instant.EPOCH);
        new MqttPublisher(f).publishTelemetry(s, t);
        assertTrue(f.topics.contains("cars/xpeng/state"));
        assertTrue(f.topics.contains("cars/xpeng/availability"));
        assertTrue(f.topics.contains("homeassistant/sensor/xpeng_battery_soc/config"));
        assertFalse(f.retained.contains(false));
        assertEquals("online", f.payloads.get(f.topics.indexOf("cars/xpeng/availability")));
    }

    @Test public void baseTopicIsNormalized() {
        assertEquals("car/xpeng", MqttSettings.normalizeTopic("/car/xpeng/"));
    }

    @Test public void usesMqtt311AndLastWill() throws Exception {
        MqttSettings s = new MqttSettings("h", 8883, true, "u", "secret", "b", "ha", 15);
        MqttConnectionSpec spec = PahoMqttTransport.spec(s);
        assertEquals("ssl://h:8883", spec.serverUri);
        assertEquals(MqttConnectOptions.MQTT_VERSION_3_1_1, spec.options.getMqttVersion());
        assertEquals("b/availability", spec.options.getWillDestination());
        assertTrue(spec.options.isAutomaticReconnect());
    }
}
