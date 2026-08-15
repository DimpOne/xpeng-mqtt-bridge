package com.schwoi.xpengbridge;

public interface MqttTransport extends AutoCloseable {
    void connect(MqttSettings settings) throws Exception;

    void publish(String topic, String payload, int qos, boolean retained) throws Exception;

    @Override
    void close() throws Exception;
}
