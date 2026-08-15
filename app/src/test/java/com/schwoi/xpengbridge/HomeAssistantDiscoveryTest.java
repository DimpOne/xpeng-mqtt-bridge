package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.util.*;
import org.json.*;
import org.junit.Test;

public class HomeAssistantDiscoveryTest {
    private MqttSettings settings(String base, int interval) {
        return new MqttSettings("h", 1883, false, "", "", base, "homeassistant", interval);
    }

    @Test public void createsReadOnlyEntitiesWithAvailability() throws Exception {
        Map<String, String> m = HomeAssistantDiscovery.payloads(settings("xpeng", 15));
        assertTrue(m.size() >= 8);
        JSONObject soc = new JSONObject(m.get("homeassistant/sensor/xpeng_battery_soc/config"));
        assertEquals("xpeng/state", soc.getString("state_topic"));
        assertEquals("xpeng/availability", soc.getString("availability_topic"));
        assertEquals("{{ value_json.battery_soc }}", soc.getString("value_template"));
        assertEquals(2700, soc.getInt("expire_after"));
        assertFalse(soc.has("command_topic"));
    }

    @Test public void binarySensorsUseExplicitOnOffAndSafeDeviceClasses() throws Exception {
        Map<String, String> m = HomeAssistantDiscovery.payloads(settings("xpeng", 20));
        JSONObject charging = new JSONObject(m.get("homeassistant/binary_sensor/xpeng_is_charging/config"));
        assertEquals("{{ 'ON' if value_json.is_charging else 'OFF' }}", charging.getString("value_template"));
        assertEquals("ON", charging.getString("payload_on"));
        assertEquals("OFF", charging.getString("payload_off"));
        assertEquals(3600, charging.getInt("expire_after"));
        JSONObject locked = new JSONObject(m.get("homeassistant/binary_sensor/xpeng_is_locked/config"));
        assertFalse(locked.has("device_class"));
        JSONObject full = new JSONObject(m.get("homeassistant/binary_sensor/xpeng_is_fully_charged/config"));
        assertFalse(full.has("device_class"));
    }

    @Test public void replacesFreshnessAndSourceAgeWithLastCheckedTimestamp() throws Exception {
        Map<String, String> m = HomeAssistantDiscovery.payloads(settings("xpeng", 15));
        JSONObject checked = new JSONObject(m.get("homeassistant/sensor/xpeng_last_checked/config"));
        assertEquals("timestamp", checked.getString("device_class"));
        assertEquals("{{ value_json.collector_last_success }}", checked.getString("value_template"));
        assertEquals("", m.get("homeassistant/sensor/xpeng_source_age_minutes/config"));
        assertEquals("", m.get("homeassistant/sensor/xpeng_freshness/config"));
    }

    @Test public void createsRefreshButtonOnDedicatedCommandTopic() throws Exception {
        Map<String, String> m = HomeAssistantDiscovery.payloads(settings("cars/xpeng", 15));
        JSONObject button = new JSONObject(m.get("homeassistant/button/xpeng_refresh/config"));
        assertEquals("cars/xpeng/refresh/set", button.getString("command_topic"));
        assertEquals("PRESS", button.getString("payload_press"));
        assertEquals("cars/xpeng/availability", button.getString("availability_topic"));
        assertFalse(button.has("state_topic"));
    }

    @Test public void rangeUnitFollowsVehicleDisplayUnit() throws Exception {
        JSONObject km = new JSONObject(HomeAssistantDiscovery.payloads(settings("xpeng", 15), "km")
                .get("homeassistant/sensor/xpeng_estimated_range/config"));
        assertEquals("km", km.getString("unit_of_measurement"));
        JSONObject mi = new JSONObject(HomeAssistantDiscovery.payloads(settings("xpeng", 15), "miles")
                .get("homeassistant/sensor/xpeng_estimated_range/config"));
        assertEquals("mi", mi.getString("unit_of_measurement"));
        JSONObject fallback = new JSONObject(HomeAssistantDiscovery.payloads(settings("xpeng", 15), null)
                .get("homeassistant/sensor/xpeng_estimated_range/config"));
        assertEquals("km", fallback.getString("unit_of_measurement"));
    }
}
