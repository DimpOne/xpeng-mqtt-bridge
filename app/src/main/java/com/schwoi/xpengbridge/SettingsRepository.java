package com.schwoi.xpengbridge;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SettingsRepository {
    private static final String FILE = "secure_mqtt_settings";
    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        try {
            MasterKey key = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(context, FILE, key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to open encrypted settings", e);
        }
    }

    public static MqttSettings defaults() {
        return new MqttSettings("", 1883, false, "", "", "xpeng/vehicle", "homeassistant", 15);
    }

    public MqttSettings load() {
        MqttSettings d = defaults();
        return new MqttSettings(
                prefs.getString("host", d.host),
                prefs.getInt("port", d.port),
                prefs.getBoolean("tls", d.tls),
                prefs.getString("username", d.username),
                prefs.getString("password", d.password),
                prefs.getString("base_topic", d.baseTopic),
                prefs.getString("discovery_prefix", d.discoveryPrefix),
                prefs.getInt("interval_minutes", d.intervalMinutes));
    }

    public MqttSettings loadSecondary() {
        MqttSettings primary = load();
        return new MqttSettings(
                prefs.getString("secondary_host", ""),
                prefs.getInt("secondary_port", 1883),
                prefs.getBoolean("secondary_tls", false),
                prefs.getString("secondary_username", ""),
                prefs.getString("secondary_password", ""),
                primary.baseTopic,
                primary.discoveryPrefix,
                primary.intervalMinutes);
    }

    public List<MqttSettings> loadAll() {
        ArrayList<MqttSettings> result = new ArrayList<>();
        MqttSettings primary = load();
        result.add(primary);
        MqttSettings secondary = loadSecondary();
        if (secondary.isConfigured()) result.add(secondary);
        return result;
    }

    public void save(MqttSettings s) {
        save(s, loadSecondary());
    }

    public void save(MqttSettings primary, MqttSettings secondary) {
        primary.validate();
        if (secondary != null && secondary.isConfigured()) secondary.validate();

        SharedPreferences.Editor editor = prefs.edit()
                .putString("host", primary.host)
                .putInt("port", primary.port)
                .putBoolean("tls", primary.tls)
                .putString("username", primary.username)
                .putString("password", primary.password)
                .putString("base_topic", primary.baseTopic)
                .putString("discovery_prefix", primary.discoveryPrefix)
                .putInt("interval_minutes", primary.intervalMinutes);

        if (secondary == null || !secondary.isConfigured()) {
            editor.remove("secondary_host")
                    .remove("secondary_port")
                    .remove("secondary_tls")
                    .remove("secondary_username")
                    .remove("secondary_password");
        } else {
            editor.putString("secondary_host", secondary.host)
                    .putInt("secondary_port", secondary.port)
                    .putBoolean("secondary_tls", secondary.tls)
                    .putString("secondary_username", secondary.username)
                    .putString("secondary_password", secondary.password);
        }
        editor.apply();
    }

    /** Stable random suffix for MQTT client IDs; avoids exposing hardware identifiers. */
    public String clientSuffix() {
        String existing = prefs.getString("client_suffix", "");
        if (!existing.isEmpty()) return existing;
        String generated = UUID.randomUUID().toString().substring(0, 8);
        prefs.edit().putString("client_suffix", generated).apply();
        return generated;
    }
}
