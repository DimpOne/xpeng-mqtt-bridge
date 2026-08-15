package com.schwoi.xpengbridge;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

public final class MqttConnectionSpec {
    public final String serverUri;
    public final MqttConnectOptions options;

    public MqttConnectionSpec(String serverUri, MqttConnectOptions options) {
        this.serverUri = serverUri;
        this.options = options;
    }
}
