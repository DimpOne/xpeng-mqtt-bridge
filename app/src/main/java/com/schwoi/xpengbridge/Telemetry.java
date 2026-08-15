package com.schwoi.xpengbridge;

import org.json.JSONObject;

public final class Telemetry {
    public final String source = "xpeng_android_accessibility";
    public final String collectorTimestamp;
    public final Integer batterySoc, estimatedRange, chargeLimit, interiorTemperatureC, sourceAgeMinutes;
    public final String rangeUnit, rangeStandard, sourceUpdateText, chargeStatus, chargeDetail, freshness;
    public final Boolean isCharging, isFullyCharged, isLocked;

    public Telemetry(String timestamp, Integer soc, Integer range, String unit, String standard,
                     Boolean charging, Boolean full, Boolean locked, Integer limit, Integer temp,
                     Integer age, String update, String status, String detail, String freshness) {
        collectorTimestamp = timestamp;
        batterySoc = soc;
        estimatedRange = range;
        rangeUnit = unit;
        rangeStandard = standard;
        isCharging = charging;
        isFullyCharged = full;
        isLocked = locked;
        chargeLimit = limit;
        interiorTemperatureC = temp;
        sourceAgeMinutes = age;
        sourceUpdateText = update;
        chargeStatus = status;
        chargeDetail = detail;
        this.freshness = freshness;
    }

    private static Object v(Object x) {
        return x == null ? JSONObject.NULL : x;
    }

    public String toJson() {
        try {
            JSONObject j = new JSONObject();
            j.put("source", source);
            j.put("collector_timestamp", collectorTimestamp);
            j.put("collector_last_success", collectorTimestamp);
            j.put("battery_soc", v(batterySoc));
            j.put("estimated_range", v(estimatedRange));
            j.put("range_unit", v(rangeUnit));
            j.put("range_standard", v(rangeStandard));
            j.put("is_charging", v(isCharging));
            j.put("is_fully_charged", v(isFullyCharged));
            j.put("is_locked", v(isLocked));
            j.put("charge_limit", v(chargeLimit));
            j.put("interior_temperature_c", v(interiorTemperatureC));
            j.put("source_age_minutes", v(sourceAgeMinutes));
            j.put("source_update_text", v(sourceUpdateText));
            j.put("charge_status", v(chargeStatus));
            j.put("charge_detail", v(chargeDetail));
            j.put("freshness", freshness);
            return j.toString();
        } catch (org.json.JSONException e) {
            throw new IllegalStateException(e);
        }
    }
}
