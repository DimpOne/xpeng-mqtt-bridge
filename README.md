# XPENG MQTT Bridge

Get your XPENG's battery, range, charging, and lock state into [Home Assistant](https://www.home-assistant.io/) — without any cloud account, API key, or reverse-engineered protocol.

XPENG offers no official local API. This app takes a different route: it runs on a spare Android phone, opens the official XPENG app, reads the numbers *you* can already see on its dashboard through an Android Accessibility Service, and publishes them to your own MQTT broker with Home Assistant auto-discovery.

> **Unofficial community project — not affiliated with or endorsed by XPENG.**
> Read-only by design: the app contains **no vehicle-control actions** of any kind. It only reads text that the XPENG app renders on screen.

## How it works

```
XPENG app (on the phone)
   │  rendered dashboard text (read-only accessibility events,
   │  restricted to the XPENG app's package)
   ▼
XPENG MQTT Bridge ──► your MQTT broker ──► Home Assistant (auto-discovery)
```

Every collection cycle (default: every 15 minutes, or on demand) the bridge wakes the phone, launches the XPENG app, waits for the dashboard to settle, parses the visible values, publishes retained MQTT state, and disconnects.

**Published entities:** battery %, estimated range (km/mi), charging + fully-charged state, door lock state, charge limit, interior temperature, charge status text, a "Last checked" timestamp, and a **Refresh** button that triggers an immediate collection from Home Assistant.

## What you need

- A dedicated Android 11+ (API 30) phone that stays home on Wi-Fi, with the official XPENG app (`com.xiaopeng.globalcarinfo`) installed, logged in, and set to **English**
- An MQTT 3.1.1 broker (e.g. Mosquitto) reachable from that phone
- Home Assistant with the MQTT integration (optional but recommended)

The phone must be able to render the XPENG app on screen: use no secure lock screen (or a trusted/extended unlock) and exempt the bridge from battery optimization. A phone that is securely locked cannot render XPENG, so scheduled collections will time out and retry.

## Install

**Obtainium (recommended for auto-updates):** add `https://github.com/schwoi/xpeng-mqtt-bridge` in [Obtainium](https://github.com/ImranR98/Obtainium) and it will track GitHub releases.

**GitHub releases:** download the latest APK from the [Releases page](https://github.com/schwoi/xpeng-mqtt-bridge/releases) and install it (you may need to allow installs from your browser/file manager).

**Build it yourself:** see [Build and test](#build-and-test) below.

## Set up (5 minutes)

1. **Open the app** and enter your broker host, port, and (optional) username/password. Set the base topic (default `xpeng/vehicle`), Home Assistant discovery prefix (default `homeassistant`), and interval. Tap **Save & Schedule**.
2. Tap **Publish Local Test (No XPENG)**. If your broker settings are right, a synthetic vehicle appears in Home Assistant within seconds — this verifies MQTT and discovery without touching the XPENG app.
3. Tap **Open Accessibility Settings** and enable **XPENG MQTT Bridge**. The service declaration and a runtime check restrict it to reading the XPENG app only.
4. Tap **Collect Now**. The bridge launches the XPENG app, waits up to 30 seconds for the dashboard, and publishes real telemetry.
5. Optionally tap **Test Automation in 1 Minute** and turn the screen off (don't securely lock it) to verify the scheduled background path works on your phone.

In Home Assistant you'll find an **XPENG Vehicle** device with all sensors plus a **Refresh** button.

## Privacy & security

This app reads another app's screen, so it deserves scrutiny. Here is exactly what it does and doesn't do — all verifiable in this repository:

- **Read-only.** There is no code path that taps, clicks, or otherwise controls anything — in the XPENG app or the vehicle. The Refresh button only schedules another read.
- **Scoped access.** The accessibility service is declared for the XPENG package only (`accessibility_service_config.xml`) and re-checks the source package at runtime before reading anything.
- **Your data stays yours.** Telemetry goes only to the MQTT broker *you* configure. There are no analytics, no third-party servers, no telemetry about you.
- **Credentials are protected.** All settings, including the broker password, are stored in AndroidX Security `EncryptedSharedPreferences`. Backup and device-transfer are disabled, credentials are never logged and never appear in status or error messages.
- **TLS done properly.** Optional TLS uses Android's system trust store; there is no insecure-certificate bypass.
- **No hardware identifiers.** MQTT client IDs use a random, locally generated suffix.

Things to know before relying on it:

- Screen scraping is inherently brittle: an XPENG app update or a non-English locale can break parsing. State entities use `expire_after` (3× your interval), so stale data shows as *unavailable* in Home Assistant instead of silently lingering.
- Using an automated accessibility reader on the XPENG app may not be covered by XPENG's terms of service. You install and run this at your own risk.
- One bridge per broker: entity IDs are fixed (`xpeng_*`), so two phones/vehicles on the same broker would collide.

## MQTT reference

| Topic | Content |
|---|---|
| `<base>/state` | Retained normalized JSON, QoS 1 |
| `<base>/availability` | Retained `online`; connection Last Will publishes retained `offline` |
| `<base>/refresh/set` | Send exactly `PRESS` to trigger one read-only collection |
| `<prefix>/{sensor\|binary_sensor\|button}/xpeng_<field>/config` | Retained Home Assistant discovery |

Telemetry publishing uses short-lived connections. A separate listener — active only while the accessibility service is enabled — subscribes to the exact refresh topic/payload and nothing else.

The state JSON includes: `battery_soc`, `estimated_range`, `range_unit`, `range_standard`, `is_charging`, `is_fully_charged`, `is_locked`, `charge_limit`, `interior_temperature_c`, `charge_status`, `charge_detail`, `source_age_minutes`, `source_update_text`, `collector_last_success`, and `freshness`. Fields the dashboard didn't show are `null`.

## Troubleshooting

- **"Collection failed" / timeout** — the phone must be awake and able to show the XPENG app. Check: accessibility service enabled, no secure lock screen, battery optimization disabled for the bridge, XPENG app logged in.
- **Sensors exist but no values** — check `is_locked` etc. are non-null in `<base>/state`; if the XPENG app isn't in English, parsing of lock/charging text fails (SOC and range usually still work).
- **Nothing in Home Assistant** — use **Publish Local Test** first; if the synthetic vehicle doesn't appear, the problem is broker settings or the HA MQTT integration, not the XPENG side.
- **Works manually, fails on schedule** — Android is throttling background work. Re-check battery optimization, and note Android enforces a minimum 15-minute interval.

## Build and test

Requires JDK 17 and the Android SDK (API 35). Set `sdk.dir` in `local.properties` or `ANDROID_HOME`, then:

```sh
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK → app/build/outputs/apk/debug/
```

For a signed release build, create `keystore.properties` in the project root (gitignored):

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

then run `./gradlew assembleRelease`.

## Contributing

Issues and PRs are welcome. Especially useful:

- **Locale reports:** if your XPENG app runs in another language, an accessibility dump of the dashboard (like the files in `evidence/`) makes it possible to support your locale.
- **XPENG app updates** that change view IDs or texts — the parser's expected IDs live in `XpengAccessibilityService.IDS` and `TelemetryParser`.

Run `./gradlew testDebugUnitTest` before submitting; parser changes should come with a test using real captured strings.

## License

[MIT](LICENSE). Not affiliated with XPENG Motors; "XPENG" is a trademark of its respective owner and is used here only to describe compatibility.
