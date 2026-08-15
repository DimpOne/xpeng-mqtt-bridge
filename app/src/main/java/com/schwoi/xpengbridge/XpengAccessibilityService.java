package com.schwoi.xpengbridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class XpengAccessibilityService extends AccessibilityService {
    public static final String XPENG_PACKAGE = "com.xiaopeng.globalcarinfo";
    private static final Set<String> IDS = new HashSet<>(Arrays.asList(
            "tv_car_miles", "tv_car_miles_unit", "tv_car_miles_type",
            "title_charge_left_tv", "data_update_time_tv", "tips_tv",
            "title_status", "charge_finish_desc", "temp_tv", "tv_title", "tv_sub_title"));
    private static volatile XpengAccessibilityService active;
    private final Handler snapshotHandler = new Handler(Looper.getMainLooper());
    private MqttRefreshListener refreshListener;

    public static boolean isAllowedPackage(String packageName) {
        return XPENG_PACKAGE.equals(packageName);
    }

    public static long[] snapshotDelaysMs() {
        return new long[]{0, 500, 1500, 3000, 5000};
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        active = this;
        refreshListener = new MqttRefreshListener(this);
        refreshListener.start();
    }

    public static void refreshMqttListener() {
        XpengAccessibilityService service = active;
        if (service != null && service.refreshListener != null) service.refreshListener.restart();
    }

    public static boolean requestXpengLaunch() {
        XpengAccessibilityService service = active;
        if (service == null) return false;
        Intent launch = service.getPackageManager().getLaunchIntentForPackage(XPENG_PACKAGE);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        service.startActivity(launch);
        service.scheduleSnapshotScans();
        return true;
    }

    private void scheduleSnapshotScans() {
        for (long delay : snapshotDelaysMs()) snapshotHandler.postDelayed(this::captureSnapshot, delay);
    }

    private void captureSnapshot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            CharSequence packageName = root.getPackageName();
            if (packageName == null || !isAllowedPackage(packageName.toString())) return;
            ArrayList<UiNode> nodes = new ArrayList<>();
            walk(root, nodes);
            if (!nodes.isEmpty()) CollectionBus.submit(nodes);
        } finally {
            root.recycle();
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null
                || !isAllowedPackage(event.getPackageName().toString())) return;
        captureSnapshot();
    }

    private static void walk(AccessibilityNodeInfo node, List<UiNode> out) {
        String id = node.getViewIdResourceName();
        CharSequence text = node.getText();
        if (id != null) {
            int slash = id.lastIndexOf('/');
            String suffix = slash >= 0 ? id.substring(slash + 1) : id;
            if (IDS.contains(suffix)) out.add(new UiNode(id, text == null ? "" : text.toString()));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                walk(child, out);
                child.recycle();
            }
        }
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        snapshotHandler.removeCallbacksAndMessages(null);
        if (refreshListener != null) refreshListener.close();
        if (active == this) active = null;
        super.onDestroy();
    }
}
