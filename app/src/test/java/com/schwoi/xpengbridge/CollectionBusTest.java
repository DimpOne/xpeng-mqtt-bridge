package com.schwoi.xpengbridge;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;

public class CollectionBusTest {
    @Test public void waitsForSnapshotNewerThanSequence() throws Exception {
        long before = CollectionBus.sequence();
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<UiNode>> f = e.submit(() -> CollectionBus.awaitAfter(before, 1000));
        List<UiNode> expected = Collections.singletonList(new UiNode("x/tv_car_miles", "123"));
        CollectionBus.submit(expected);
        assertEquals("123", f.get(2, TimeUnit.SECONDS).get(0).text);
        e.shutdownNow();
    }

    @Test public void timesOutWithoutAccessibilityEvent() throws Exception {
        long start = System.nanoTime();
        assertNull(CollectionBus.awaitAfter(CollectionBus.sequence(), 30));
        assertTrue(System.nanoTime() - start >= 20_000_000L);
    }

    @Test public void onlyAcceptsXpengPackage() {
        assertTrue(XpengAccessibilityService.isAllowedPackage("com.xiaopeng.globalcarinfo"));
        assertFalse(XpengAccessibilityService.isAllowedPackage("other.app"));
    }

    @Test public void refreshScansAlreadyVisibleDashboardAtBoundedDelays() {
        assertArrayEquals(new long[]{0, 500, 1500, 3000, 5000}, XpengAccessibilityService.snapshotDelaysMs());
    }

    @Test public void ignoresPartialLoadingSnapshotsUntilDashboardIsReady() throws Exception {
        long before = CollectionBus.sequence();
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<UiNode>> f = e.submit(() -> CollectionBus.awaitReadyAfter(before, 5000));
        CollectionBus.submit(Collections.singletonList(new UiNode("x/title_charge_left_tv", "50%")));
        Thread.sleep(30);
        assertFalse(f.isDone());
        List<UiNode> ready = Arrays.asList(
                new UiNode("x/title_charge_left_tv", "50%"), new UiNode("x/tv_car_miles", "200"));
        CollectionBus.submit(ready);
        assertEquals(2, f.get(5, TimeUnit.SECONDS).size());
        e.shutdownNow();
    }

    @Test public void dashboardReadinessRequiresSocAndRangeResourceIds() {
        assertFalse(CollectionBus.isDashboardReady(
                Collections.singletonList(new UiNode("x/tv_car_miles", "1"))));
        assertTrue(CollectionBus.isDashboardReady(Arrays.asList(
                new UiNode("x/tv_car_miles", "1"), new UiNode("x/title_charge_left_tv", "2%"))));
    }

    @Test public void waitsForUpdatedDashboardInsteadOfReturningFirstCachedSnapshot() throws Exception {
        long before = CollectionBus.sequence();
        ExecutorService e = Executors.newSingleThreadExecutor();
        Future<List<UiNode>> f = e.submit(() -> CollectionBus.awaitReadyAfter(before, 5000));
        List<UiNode> cached = Arrays.asList(
                new UiNode("x/title_charge_left_tv", "90%"), new UiNode("x/tv_car_miles", "510"));
        CollectionBus.submit(cached);
        Thread.sleep(40);
        assertFalse("collector returned the first cached dashboard before XPENG could refresh", f.isDone());
        List<UiNode> refreshed = Arrays.asList(
                new UiNode("x/title_charge_left_tv", "88%"), new UiNode("x/tv_car_miles", "501"));
        CollectionBus.submit(refreshed);
        List<UiNode> result = f.get(5, TimeUnit.SECONDS);
        assertEquals("88%", result.get(0).text);
        assertEquals("501", result.get(1).text);
        e.shutdownNow();
    }
}
