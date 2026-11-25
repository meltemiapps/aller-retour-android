# Aller-Retour

**A free, open-source travel companion for Android that never tracks you.**

Aller-Retour helps you prepare for a trip, find your way around while you're there and get home safely. It has no account, no ads, no analytics and **no internet permission**: the app cannot send your data anywhere, because Android does not let it connect to the network.

## Reminders (1.1)

Optional local reminders: passport expiry (9 and 6 months before), online check-in, leaving for the airport, packing, moving on to the next stop, Schengen days, travel insurance, GHIC expiry, daily medication and a daily "I'm safe" nudge. They use Android's AlarmManager and notifications on the phone itself (permissions: POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED). Still no internet permission.

## Version 1 features

| Tab | What it does |
|---|---|
| **Today** | Daily brief (where you are, next stop, moving-on days), sunrise and sunset calculated on the phone, local and UK clocks with a "good time to call home?" check, and a Schengen 90/180 summary |
| **Plan** | Route of up to 10 stops in any mix of countries, with dates, reordering and **extend my stay** (later stops move with it). Overlap and gap warnings, Schengen 90/180 counter, passport validity checks against the route, driving-permit and entry-rule summaries, and a pre-departure readiness checklist |
| **Money** | Offline currency converter using your own rate, and an expense log in each stop's currency with a running total in pounds |
| **Safety** | Tap-to-dial emergency numbers for the current country, Call Home with the correct international dialling format, a one-off "I am here" location you can share or show to a driver, and **Drop a pin** with a compass that guides you back |
| **Guide** | Plugs and voltage, driving rules, tipping and etiquette, water and common scams, medicine rules, full-screen phrase cards, a menu decoder and a medical card in large text |
| **Kit** | Packing templates (beach, city, winter, business) that adapt to your route, plus backup export and restore |

Country packs in Version 1 cover Greece, Spain, Italy, France, the United States, Japan and Thailand, for travellers whose home is the UK.

## Privacy by design

- **No `INTERNET` permission.** It is removed in the manifest and CI fails the build if it ever comes back.
- **No Google Play Services**, so the app suits de-Googled phones and F-Droid. Location uses Android's own `LocationManager`.
- **Location only on demand**: when you tap *Get my location*, *Save pin here* or *Guide me back*. Never in the background.
- **Colours follow your wallpaper** on Android 12 and later, with Aller-Retour blue as the fallback on older phones and as an option in Kit.
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

1. Create a signing key once: `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias allerretour`
2. Add repository secrets: `KEYSTORE_BASE64` (output of `base64 -w0 release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
3. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, then push a tag: `git tag v1.0.0 && git push --tags`.

The release workflow builds a signed APK and attaches it to a GitHub Release. Keep `release.jks` safe and backed up: you need the same key for every update.

## Project layout

```
app/src/main/assets/packs/       Country packs (one JSON file per country)
app/src/main/java/.../data/      Models, pack loading, on-device storage
app/src/main/java/.../logic/     Trip rules (dates, Schengen, dialling, distance, sun times), no Android code
app/src/main/java/.../ui/        Jetpack Compose UI, one file per tab under screens/
fastlane/metadata/               F-Droid / store listing text
```

## Contributing

Country packs are the easiest place to help. See [CONTRIBUTING.md](CONTRIBUTING.md). The plan for later versions is in [ROADMAP.md](ROADMAP.md).

## Disclaimer

Country information is a summary for guidance only. Entry rules, medicine rules, driving laws and emergency numbers change. Always check official sources such as GOV.UK foreign travel advice before you travel.

## Licence

GNU General Public License v3.0. See [LICENSE](LICENSE). You may use, study, share and modify Aller-Retour. If you distribute a modified version, it must also be free and open source under the same licence.
