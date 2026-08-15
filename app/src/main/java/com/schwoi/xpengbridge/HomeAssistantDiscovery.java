package com.schwoi.xpengbridge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class HomeAssistantDiscovery {
    private HomeAssistantDiscovery() {}

    private static JSONObject device() throws JSONException {
        JSONObject d = new JSONObject();
        d.put("identifiers", new JSONArray().put("xpeng_android_bridge"));
        d.put("name", "XPENG Vehicle");
        d.put("manufacturer", "XPENG");
        d.put("model", "Android accessibility bridge");
        return d;
    }

    private static String sensor(MqttSettings s, String name, String key, String unit,
                                 String deviceClass, String stateClass, boolean binary)
            throws JSONException {
        JSONObject j = new JSONObject();
        j.put("name", name);
        j.put("unique_id", "xpeng_" + key);
        j.put("state_topic", s.baseTopic + "/state");
        j.put("availability_topic", s.baseTopic + "/availability");
        j.put("expire_after", s.intervalMinutes * 60 * 3);
        j.put("value_template", binary
                ? "{{ 'ON' if value_json." + key + " else 'OFF' }}"
                : "{{ value_json." + key + " }}");
        if (binary) {
            j.put("payload_on", "ON");
            j.put("payload_off", "OFF");
        }
        if (unit != null) j.put("unit_of_measurement", unit);
        if (deviceClass != null) j.put("device_class", deviceClass);
        if (stateClass != null) j.put("state_class", stateClass);
        j.put("device", device());
        return j.toString();
    }

    private static String refreshButton(MqttSettings s) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("name", "Refresh");
        j.put("unique_id", "xpeng_refresh");
        j.put("command_topic", RefreshCommand.topic(s));
        j.put("payload_press", RefreshCommand.PAYLOAD);
        j.put("availability_topic", s.baseTopic + "/availability");
        j.put("device_class", "restart");
        j.put("device", device());
        return j.toString();
    }

    public static Map<String, String> payloads(MqttSettings s) {
        return payloads(s, null);
    }

    public static Map<String, String> payloads(MqttSettings s, String rangeUnit) {
        String distanceUnit = rangeUnit != null && rangeUnit.toLowerCase(Locale.ROOT).contains("mi")
                ? "mi" : "km";
        try {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            String[][] entities = {
                    {"sensor", "battery_soc", "Battery", "%", "battery", "measurement"},
                    {"sensor", "estimated_range", "Estimated range", distanceUnit, "distance", "measurement"},
                    {"sensor", "charge_limit", "Charge limit", "%", "battery", "measurement"},
                    {"sensor", "interior_temperature_c", "Interior temperature", "°C", "temperature", "measurement"},
                    {"sensor", "collector_last_success", "Last checked", null, "timestamp", null},
                    {"sensor", "charge_status", "Charge status", null, null, null},
                    {"binary_sensor", "is_charging", "Charging", null, "battery_charging", null},
                    {"binary_sensor", "is_fully_charged", "Fully charged", null, null, null},
                    {"binary_sensor", "is_locked", "Locked", null, null, null}
            };
            for (String[] e : entities) {
                boolean binary = e[0].equals("binary_sensor");
                String topicKey = e[1].equals("collector_last_success") ? "last_checked" : e[1];
                m.put(s.discoveryPrefix + "/" + e[0] + "/xpeng_" + topicKey + "/config",
                        sensor(s, e[2], e[1], e[3], e[4], e[5], binary));
            }
            m.put(s.discoveryPrefix + "/button/xpeng_refresh/config", refreshButton(s));
            // Empty retained discovery payloads delete the two superseded entities from Home Assistant.
            m.put(s.discoveryPrefix + "/sensor/xpeng_source_age_minutes/config", "");
            m.put(s.discoveryPrefix + "/sensor/xpeng_freshness/config", "");
            return m;
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }
}
