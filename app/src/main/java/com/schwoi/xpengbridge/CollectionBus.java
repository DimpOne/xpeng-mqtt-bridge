package com.schwoi.xpengbridge;

import java.util.ArrayList;
import java.util.List;

public final class CollectionBus {
    private static final long DASHBOARD_SETTLE_MS = 3000;
    private static long seq;
    private static List<UiNode> latest;

    private CollectionBus() {}

    public static synchronized long sequence() {
        return seq;
    }

    public static synchronized void submit(List<UiNode> nodes) {
        latest = new ArrayList<>(nodes);
        seq++;
        CollectionBus.class.notifyAll();
    }

    public static synchronized List<UiNode> awaitAfter(long after, long timeoutMs)
            throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (seq <= after) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) return null;
            CollectionBus.class.wait(left);
        }
        return new ArrayList<>(latest);
    }

    public static synchronized List<UiNode> awaitReadyAfter(long after, long timeoutMs)
            throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        long observed = after;
        List<UiNode> lastReady = null;
        String lastFingerprint = null;
        long settledAt = Long.MAX_VALUE;
        while (true) {
            while (seq <= observed) {
                long now = System.currentTimeMillis();
                if (lastReady != null && now >= settledAt) return lastReady;
                long left = end - now;
                if (left <= 0) return lastReady;
                long waitFor = lastReady == null ? left : Math.min(left, settledAt - now);
                CollectionBus.class.wait(waitFor);
            }
            observed = seq;
            if (isDashboardReady(latest)) {
                String fingerprint = fingerprint(latest);
                if (!fingerprint.equals(lastFingerprint)) {
                    lastReady = new ArrayList<>(latest);
                    lastFingerprint = fingerprint;
                    settledAt = System.currentTimeMillis() + DASHBOARD_SETTLE_MS;
                }
            }
            if (System.currentTimeMillis() >= end) return lastReady;
        }
    }

    private static String fingerprint(List<UiNode> nodes) {
        StringBuilder value = new StringBuilder();
        for (UiNode node : nodes) value.append(node.resourceId).append('\u0000').append(node.text).append('\u0001');
        return value.toString();
    }

    public static boolean isDashboardReady(List<UiNode> nodes) {
        return hasValue(nodes, "title_charge_left_tv") && hasValue(nodes, "tv_car_miles");
    }

    private static boolean hasValue(List<UiNode> nodes, String suffix) {
        if (nodes == null) return false;
        for (UiNode node : nodes) {
            if (node.resourceId.endsWith("/" + suffix) && !node.text.trim().isEmpty()) return true;
        }
        return false;
    }
}
