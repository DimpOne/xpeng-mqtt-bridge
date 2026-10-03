package com.schwoi.xpengbridge;

public final class MqttSettings {
    public final String host, username, password, baseTopic, discoveryPrefix;
    public final int port, intervalMinutes;
    public final boolean tls;

    public MqttSettings(String host, int port, boolean tls, String username, String password,
                        String baseTopic, String discoveryPrefix, int intervalMinutes) {
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.tls = tls;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.baseTopic = normalizeTopic(baseTopic);
        this.discoveryPrefix = normalizeTopic(discoveryPrefix);
        this.intervalMinutes = Math.max(15, intervalMinutes);
    }

    public static String normalizeTopic(String topic) {
        if (topic == null) return "";
        return topic.trim().replaceAll("^/+|/+$", "");
    }

    public boolean isConfigured() {
        return !host.isEmpty();
    }

    public void validate() {
        if (host.isEmpty()) throw new IllegalArgumentException("MQTT host is required");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1-65535");
        if (baseTopic.isEmpty() || discoveryPrefix.isEmpty())
            throw new IllegalArgumentException("MQTT topics are required");
    }
}
