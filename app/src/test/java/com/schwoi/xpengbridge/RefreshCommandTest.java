package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class RefreshCommandTest {
    @Test public void acceptsOnlyExactRefreshTopicAndPressPayload() {
        MqttSettings s = new MqttSettings("h", 1883, false, "", "", "cars/xpeng", "homeassistant", 15);
        assertTrue(RefreshCommand.matches(s, "cars/xpeng/refresh/set", "PRESS".getBytes(StandardCharsets.UTF_8)));
        assertFalse(RefreshCommand.matches(s, "cars/xpeng/refresh/set", "press".getBytes(StandardCharsets.UTF_8)));
        assertFalse(RefreshCommand.matches(s, "cars/xpeng/other", "PRESS".getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void exposesNormalizedCommandTopic() {
        MqttSettings s = new MqttSettings("h", 1883, false, "", "", "/cars/xpeng/", "homeassistant", 15);
        assertEquals("cars/xpeng/refresh/set", RefreshCommand.topic(s));
    }
}
