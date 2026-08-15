package com.schwoi.xpengbridge;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends AppCompatActivity {
    private EditText host, port, user, password, base, prefix, interval;
    private CheckBox tls;
    private TextView status;
    private SettingsRepository repo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            status.setText(intent.getBooleanExtra(ManualCollectionService.EXTRA_OK, false)
                    ? intent.getStringExtra(ManualCollectionService.EXTRA_MESSAGE)
                    : "Collection failed. Check accessibility, phone state, and MQTT settings.");
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        repo = new SettingsRepository(this);
        host = findViewById(R.id.host);
        port = findViewById(R.id.port);
        tls = findViewById(R.id.tls);
        user = findViewById(R.id.username);
        password = findViewById(R.id.password);
        base = findViewById(R.id.base_topic);
        prefix = findViewById(R.id.discovery_prefix);
        interval = findViewById(R.id.interval);
        status = findViewById(R.id.status);
        load();

        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        findViewById(R.id.collect).setOnClickListener(v -> {
            if (save()) {
                status.setText("Launching XPENG and waiting for accessibility events…");
                ContextCompat.startForegroundService(this, new Intent(this, ManualCollectionService.class));
            }
        });
        findViewById(R.id.test_schedule).setOnClickListener(v -> {
            if (save()) {
                CollectionScheduler.scheduleTest(this);
                status.setText("Automation test scheduled for one minute. "
                        + "Turn the screen off but do not securely lock the phone.");
            }
        });
        findViewById(R.id.test_publish).setOnClickListener(v -> {
            if (save()) {
                status.setText("Publishing local test telemetry…");
                executor.execute(() -> {
                    try {
                        CollectionRunner.publishTest(this);
                        runOnUiThread(() ->
                                status.setText("Local test telemetry published (no XPENG data used)."));
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                status.setText("Test publish failed. Check MQTT settings and connection."));
                    }
                });
            }
        });

        ContextCompat.registerReceiver(this, receiver,
                new IntentFilter(ManualCollectionService.ACTION), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { })
                    .launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void load() {
        MqttSettings s = repo.load();
        host.setText(s.host);
        port.setText(String.valueOf(s.port));
        tls.setChecked(s.tls);
        user.setText(s.username);
        password.setText(s.password);
        base.setText(s.baseTopic);
        prefix.setText(s.discoveryPrefix);
        interval.setText(String.valueOf(s.intervalMinutes));
    }

    private boolean save() {
        try {
            MqttSettings s = new MqttSettings(
                    host.getText().toString(),
                    Integer.parseInt(port.getText().toString()),
                    tls.isChecked(),
                    user.getText().toString(),
                    password.getText().toString(),
                    base.getText().toString(),
                    prefix.getText().toString(),
                    Integer.parseInt(interval.getText().toString()));
            repo.save(s);
            CollectionScheduler.schedule(this, s.intervalMinutes);
            XpengAccessibilityService.refreshMqttListener();
            interval.setText(String.valueOf(s.intervalMinutes));
            status.setText("Settings saved; periodic collection and MQTT refresh listener scheduled.");
            return true;
        } catch (Exception e) {
            status.setText("Invalid settings. Host, port, topics, and interval are required.");
            return false;
        }
    }

    @Override protected void onDestroy() {
        unregisterReceiver(receiver);
        executor.shutdownNow();
        super.onDestroy();
    }
}
