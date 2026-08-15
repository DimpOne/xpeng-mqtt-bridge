package com.schwoi.xpengbridge;

import java.nio.charset.StandardCharsets;

public final class RefreshCommand {
    public static final String PAYLOAD = "PRESS";

    private RefreshCommand() {}

    public static String topic(MqttSettings settings) {
        return settings.baseTopic + "/refresh/set";
    }

    public static boolean matches(MqttSettings settings, String topic, byte[] payload) {
        return topic(settings).equals(topic)
                && PAYLOAD.equals(new String(payload, StandardCharsets.UTF_8));
    }
}
