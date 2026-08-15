package com.schwoi.xpengbridge;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
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

    public void save(MqttSettings s) {
        s.validate();
        prefs.edit()
                .putString("host", s.host)
                .putInt("port", s.port)
                .putBoolean("tls", s.tls)
                .putString("username", s.username)
                .putString("password", s.password)
                .putString("base_topic", s.baseTopic)
                .putString("discovery_prefix", s.discoveryPrefix)
                .putInt("interval_minutes", s.intervalMinutes)
                .apply();
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
