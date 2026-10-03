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
    private EditText host, port, user, password;
    private EditText secondaryHost, secondaryPort, secondaryUser, secondaryPassword;
    private EditText base, prefix, interval;
    private CheckBox tls, secondaryTls;
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

        secondaryHost = findViewById(R.id.secondary_host);
        secondaryPort = findViewById(R.id.secondary_port);
        secondaryTls = findViewById(R.id.secondary_tls);
        secondaryUser = findViewById(R.id.secondary_username);
        secondaryPassword = findViewById(R.id.secondary_password);

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
                ContextCompat.startForegroundService(this,
                        new Intent(this, ManualCollectionService.class));
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
                status.setText("Publishing local test telemetry to configured brokers…");
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
                new IntentFilter(ManualCollectionService.ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { })
                    .launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void load() {
        MqttSettings primary = repo.load();
        MqttSettings secondary = repo.loadSecondary();

        host.setText(primary.host);
        port.setText(String.valueOf(primary.port));
        tls.setChecked(primary.tls);
        user.setText(primary.username);
        password.setText(primary.password);

        secondaryHost.setText(secondary.host);
        secondaryPort.setText(String.valueOf(secondary.port));
        secondaryTls.setChecked(secondary.tls);
        secondaryUser.setText(secondary.username);
        secondaryPassword.setText(secondary.password);

        base.setText(primary.baseTopic);
        prefix.setText(primary.discoveryPrefix);
        interval.setText(String.valueOf(primary.intervalMinutes));
    }

    private boolean save() {
        try {
            int intervalMinutes = Integer.parseInt(interval.getText().toString());
            String baseTopic = base.getText().toString();
            String discoveryPrefix = prefix.getText().toString();

            MqttSettings primary = new MqttSettings(
                    host.getText().toString(),
                    Integer.parseInt(port.getText().toString()),
                    tls.isChecked(),
                    user.getText().toString(),
                    password.getText().toString(),
                    baseTopic,
                    discoveryPrefix,
                    intervalMinutes);

            String secondaryPortText = secondaryPort.getText().toString().trim();
            MqttSettings secondary = new MqttSettings(
                    secondaryHost.getText().toString(),
                    secondaryPortText.isEmpty() ? 1883 : Integer.parseInt(secondaryPortText),
                    secondaryTls.isChecked(),
                    secondaryUser.getText().toString(),
                    secondaryPassword.getText().toString(),
                    baseTopic,
                    discoveryPrefix,
                    intervalMinutes);

            repo.save(primary, secondary);
            CollectionScheduler.schedule(this, primary.intervalMinutes);
            XpengAccessibilityService.refreshMqttListener();
            interval.setText(String.valueOf(primary.intervalMinutes));

            int brokerCount = secondary.isConfigured() ? 2 : 1;
            status.setText("Settings saved for " + brokerCount
                    + " MQTT broker" + (brokerCount == 1 ? "" : "s")
                    + "; periodic collection and refresh listeners scheduled.");
            return true;
        } catch (Exception e) {
            status.setText("Invalid settings. Primary host, ports, topics, and interval are required.");
            return false;
        }
    }

    @Override protected void onDestroy() {
        unregisterReceiver(receiver);
        executor.shutdownNow();
        super.onDestroy();
    }
}
