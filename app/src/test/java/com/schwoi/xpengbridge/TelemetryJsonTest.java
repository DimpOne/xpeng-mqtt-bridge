package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.time.Instant;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;

public class TelemetryJsonTest {
    @Test public void emitsStableNormalizedJsonIncludingNulls() throws Exception {
        Telemetry t = new TelemetryParser().parse(Arrays.asList(
                new UiNode("x/tv_car_miles", "42"), new UiNode("x/title_charge_left_tv", "9%")),
                Instant.EPOCH);
        JSONObject j = new JSONObject(t.toJson());
        assertEquals("xpeng_android_accessibility", j.getString("source"));
        assertEquals(9, j.getInt("battery_soc"));
        assertEquals(42, j.getInt("estimated_range"));
        assertTrue(j.isNull("is_locked"));
        assertEquals("unknown", j.getString("freshness"));
    }
}
