package com.schwoi.xpengbridge;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TelemetryParser {

    private static String first(List<UiNode> nodes, String id) {
        for (UiNode n : nodes) {
            if (n.resourceId.endsWith("/" + id) && !n.text.isEmpty()) return n.text;
        }
        return null;
    }

    private static Integer integer(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("-?\\d+").matcher(s.replace(",", ""));
        return m.find() ? Integer.valueOf(m.group()) : null;
    }

    private static Integer percent(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("(\\d{1,3})\\s*%").matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    // XPENG renders "Door Locked" but also "Door(s) not locked", so the negated
    // forms must be checked before the bare "locked" substring.
    private static Boolean lockState(String text) {
        if (text == null) return null;
        String l = text.toLowerCase(Locale.ROOT);
        if (l.contains("not locked") || l.contains("unlocked")) return false;
        if (l.contains("locked")) return true;
        return null;
    }

    public Telemetry parse(List<UiNode> nodes, Instant now) {
        Integer range = integer(first(nodes, "tv_car_miles"));
        Integer soc = percent(first(nodes, "title_charge_left_tv"));
        if (range == null || soc == null)
            throw new IllegalArgumentException("XPENG dashboard SOC and range were not found");

        String update = first(nodes, "data_update_time_tv");
        String status = first(nodes, "title_status");

        Integer age = null;
        if (update != null) {
            Matcher m = Pattern.compile("(\\d+)\\s+minute", Pattern.CASE_INSENSITIVE).matcher(update);
            if (m.find()) age = Integer.valueOf(m.group(1));
            else if (Pattern.compile("just now|seconds? ago", Pattern.CASE_INSENSITIVE).matcher(update).find()) age = 0;
        }

        Boolean locked = lockState(first(nodes, "tips_tv"));

        Boolean charging = null, full = null;
        if (status != null) {
            String s = status.toLowerCase(Locale.ROOT);
            full = s.contains("charging complete") || s.contains("fully charged");
            if (full) charging = false;
            else if (s.contains("charging")) charging = true;
        }

        Integer limit = null;
        for (int i = 0; i < nodes.size(); i++) {
            UiNode n = nodes.get(i);
            if (n.resourceId.endsWith("/tv_title") && n.text.equals("Charging")) {
                for (int k = i + 1; k < Math.min(nodes.size(), i + 12); k++) {
                    if (nodes.get(k).resourceId.endsWith("/tv_sub_title")) {
                        limit = percent(nodes.get(k).text);
                        break;
                    }
                }
                break;
            }
        }

        return new Telemetry(now.toString(), soc, range,
                first(nodes, "tv_car_miles_unit"), first(nodes, "tv_car_miles_type"),
                charging, full, locked, limit, integer(first(nodes, "temp_tv")), age,
                update, status, first(nodes, "charge_finish_desc"),
                age == null ? "unknown" : age <= 10 ? "fresh" : "stale");
    }
}
