# Aller-Retour

**A free, open-source travel companion for Android that never tracks you.**

Aller-Retour helps you prepare for a trip, find your way around while you're there and get home safely. It has no account, no ads, no analytics and **no internet permission**: the app cannot send your data anywhere, because Android does not let it connect to the network.

## Reminders (1.1)

Optional local reminders: passport expiry (9 and 6 months before), online check-in, leaving for the airport, packing, moving on to the next stop, Schengen days, travel insurance, GHIC expiry, daily medication and a daily "I'm safe" nudge. They use Android's AlarmManager and notifications on the phone itself (permissions: POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED). Still no internet permission.

## Features

Since 1.1 the Android app has the same screens, cards and wording as Aller-Retour on iPhone: glass cards over a background scene (sky, sunset, olive grove, night harbour, or one of your own photos), large titles, a floating five-tab bar and the ⓘ settings sheet.

| Tab | What it does |
|---|---|
| **Today** | Trip card (day and stop, sunset, time difference, next stop, currency, plugs, voltage), Schengen 90/180, quick convert, emergency call, coming-up reminders and a phrase of the day |
| **Plan** | Route of up to 10 stops in any mix of countries, with gap and overlap warnings, **extend my stay** (later stops move with it) and the Schengen 90/180 counter |
| **Money** | Offline converter using your own rate, your rate per currency, and an expense log with a running total in pounds |
| **Guide** | **Safety & emergencies** first (tap-to-dial numbers, Call Home with the right dialling format, "I am here" as text, **Drop a pin** with distance and direction), then essentials, entry rules, money and tipping, customs, driving, health, full-screen phrase cards and a menu decoder. Filter by section and switch country |
| **Kit** | Packing templates that adapt to your route, ready-to-go checklist, passport check against the route, optional reminders and a medical card |

Settings (ⓘ) holds the glass style, background, backup export and restore, erase, licence, source and privacy.

Country packs in Version 1 cover Greece, Spain, Italy, France, the United States, Japan and Thailand, for travellers whose home is the UK.

## Privacy by design

- **No `INTERNET` permission.** It is removed in the manifest and CI fails the build if it ever comes back.
- **No Google Play Services**, so the app suits de-Googled phones and F-Droid. Location uses Android's own `LocationManager`.
- **Location only on demand**: when you tap *Show where I am*, *Pin this spot* or *Update*. Never in the background.
- **Your background photo stays on the phone**: picked with Android's photo picker (no storage permission), copied into app-private storage and deleted when you erase your data.
- **Storage stays on the phone**: one JSON file in app-private storage, excluded from Google cloud backup and device transfer.
- **You control exports**: backups are plain JSON files that you save wherever you choose.
- Calls open your phone's dialler, and sharing uses Android's share sheet, so you see everything before it leaves the phone.

See [PRIVACY.md](PRIVACY.md).

## Building

Requirements: JDK 17 and the Android SDK (Android Studio Meerkat (2024.3) or newer is easiest).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # trip logic unit tests
```

Open the project in Android Studio to run it on a device or emulator. Minimum Android version: 8.0 (API 26).

### Continuous integration

Every push runs `.github/workflows/build.yml`. It runs the unit tests, builds a debug APK (download it from the run's *Artifacts*) and checks the APK for forbidden permissions.

### Releasing

CI never signs anything and needs no secrets. Build signed releases on your own machine:

1. Create a signing key once: `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias allerretour`. Keep it outside this repo, safe and backed up: you need the same key for every update.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Set `ALLERRETOUR_KEYSTORE` (path to `release.jks`), `ALLERRETOUR_KEYSTORE_PASSWORD`, `ALLERRETOUR_KEY_ALIAS` and `ALLERRETOUR_KEY_PASSWORD` in your shell, then run `./gradlew assembleRelease` (or `bundleRelease` for Google Play).

Without those variables the release build is unsigned.

## Project layout

```
app/src/main/assets/packs/       Country packs (one JSON file per country)
app/src/main/java/.../data/      Models, pack loading, on-device storage
app/src/main/java/.../logic/     Trip rules (dates, Schengen, dialling, distance, sun times), no Android code
app/src/main/java/.../ui/        Jetpack Compose UI: glass components and backgrounds, one file per tab under screens/
fastlane/metadata/               F-Droid / store listing text
```

## Contributing

Country packs are the easiest place to help. See [CONTRIBUTING.md](CONTRIBUTING.md). The plan for later versions is in [ROADMAP.md](ROADMAP.md).

## Disclaimer

Country information is a summary for guidance only. Entry rules, medicine rules, driving laws and emergency numbers change. Always check official sources such as GOV.UK foreign travel advice before you travel.

## Licence

GNU General Public License v3.0. See [LICENSE](LICENSE). You may use, study, share and modify Aller-Retour. If you distribute a modified version, it must also be free and open source under the same licence.
