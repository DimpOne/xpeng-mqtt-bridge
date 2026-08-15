package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class SettingsDefaultsTest {
    @Test public void defaultsHaveNoHostAndNoCredentials() {
        MqttSettings s = SettingsRepository.defaults();
        assertEquals("", s.host);
        assertEquals(1883, s.port);
        assertFalse(s.tls);
        assertEquals("", s.username);
        assertEquals("", s.password);
        assertEquals("xpeng/vehicle", s.baseTopic);
        assertEquals("homeassistant", s.discoveryPrefix);
        assertEquals(15, s.intervalMinutes);
    }

    @Test public void emptyHostFailsValidation() {
        try { SettingsRepository.defaults().validate(); fail(); }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("host")); }
    }

    @Test public void intervalIsClampedToAndroidMinimum() {
        assertEquals(15, new MqttSettings("h", 1, false, "", "", "b", "d", 2).intervalMinutes);
    }
}
