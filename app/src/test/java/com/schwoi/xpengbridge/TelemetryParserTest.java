package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class TelemetryParserTest {
    private UiNode n(String id, String text) { return new UiNode("com.xiaopeng.globalcarinfo:id/" + id, text); }

    @Test public void parsesCompleteDashboardIntoNormalizedTelemetry() {
        List<UiNode> nodes = Arrays.asList(
            n("tv_car_miles", "251"), n("tv_car_miles_unit", "km"), n("tv_car_miles_type", "WLTP"),
            n("title_charge_left_tv", "67%"), n("data_update_time_tv", "3 minutes ago"),
            n("tips_tv", "Vehicle locked"), n("title_status", "Charging"),
            n("charge_finish_desc", "Finished at 06:20"), n("temp_tv", "22°C"),
            n("tv_title", "Charging"), n("tv_sub_title", "Limit 80%")
        );
        Telemetry t = new TelemetryParser().parse(nodes, Instant.parse("2026-08-15T12:00:00Z"));
        assertEquals(Integer.valueOf(67), t.batterySoc);
        assertEquals(Integer.valueOf(251), t.estimatedRange);
        assertEquals("km", t.rangeUnit); assertEquals("WLTP", t.rangeStandard);
        assertEquals(Boolean.TRUE, t.isCharging); assertEquals(Boolean.FALSE, t.isFullyCharged);
        assertEquals(Boolean.TRUE, t.isLocked); assertEquals(Integer.valueOf(80), t.chargeLimit);
        assertEquals(Integer.valueOf(22), t.interiorTemperatureC);
        assertEquals(Integer.valueOf(3), t.sourceAgeMinutes); assertEquals("fresh", t.freshness);
        assertEquals("2026-08-15T12:00:00Z", t.collectorTimestamp);
    }

    // Real strings captured from the XPENG app (see evidence/): the unlocked state
    // is rendered as "Door(s) not locked", which contains the substring "locked".
    @Test public void doorsNotLockedMeansUnlocked() {
        List<UiNode> nodes = Arrays.asList(n("tv_car_miles", "501"), n("title_charge_left_tv", "88%"),
            n("tips_tv", "Door(s) not locked"));
        assertEquals(Boolean.FALSE, new TelemetryParser().parse(nodes, Instant.EPOCH).isLocked);
    }

    @Test public void doorLockedMeansLocked() {
        List<UiNode> nodes = Arrays.asList(n("tv_car_miles", "511"), n("title_charge_left_tv", "90%"),
            n("tips_tv", "Door Locked"));
        assertEquals(Boolean.TRUE, new TelemetryParser().parse(nodes, Instant.EPOCH).isLocked);
    }

    @Test public void unknownLockTextLeavesLockStateNull() {
        List<UiNode> nodes = Arrays.asList(n("tv_car_miles", "100"), n("title_charge_left_tv", "50%"),
            n("tips_tv", "Charging time: 2h51min"));
        assertNull(new TelemetryParser().parse(nodes, Instant.EPOCH).isLocked);
    }

    @Test public void pairsChargingTitleWithNearestFollowingSubtitle() {
        List<UiNode> nodes = Arrays.asList(n("tv_car_miles", "200"), n("title_charge_left_tv", "50%"),
            n("tv_title", "Other"), n("tv_sub_title", "99%"), n("tv_title", "Charging"),
            n("temp_tv", "20"), n("tv_sub_title", "85%"));
        assertEquals(Integer.valueOf(85), new TelemetryParser().parse(nodes, Instant.EPOCH).chargeLimit);
    }

    @Test public void recognizesCompleteUnlockedAndStale() {
        List<UiNode> nodes = Arrays.asList(n("tv_car_miles", "300"), n("title_charge_left_tv", "100%"),
            n("tips_tv", "Vehicle unlocked"), n("title_status", "Charging complete"),
            n("data_update_time_tv", "18 minutes ago"));
        Telemetry t = new TelemetryParser().parse(nodes, Instant.EPOCH);
        assertEquals(Boolean.FALSE, t.isLocked); assertEquals(Boolean.FALSE, t.isCharging);
        assertEquals(Boolean.TRUE, t.isFullyCharged); assertEquals("stale", t.freshness);
    }

    @Test public void requiresSocAndRange() {
        try { new TelemetryParser().parse(Collections.singletonList(n("tv_car_miles", "200")), Instant.EPOCH); fail(); }
        catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("dashboard")); }
    }
}
