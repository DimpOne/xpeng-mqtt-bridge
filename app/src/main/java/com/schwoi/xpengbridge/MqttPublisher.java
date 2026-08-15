package com.schwoi.xpengbridge;

import java.util.Map;

public final class MqttPublisher {
    private final MqttTransport transport;

    public MqttPublisher(MqttTransport transport) {
        this.transport = transport;
    }

    public void publishTelemetry(MqttSettings s, Telemetry t) throws Exception {
        transport.connect(s);
        try {
            for (Map.Entry<String, String> e : HomeAssistantDiscovery.payloads(s, t.rangeUnit).entrySet())
                transport.publish(e.getKey(), e.getValue(), 1, true);
            transport.publish(s.baseTopic + "/state", t.toJson(), 1, true);
            transport.publish(s.baseTopic + "/availability", "online", 1, true);
        } finally {
            transport.close();
        }
    }
}
