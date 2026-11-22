# Contributing

Thank you for helping. Contributions are accepted under the GPL-3.0 licence.

## Ground rules
1. **No network access.** Pull requests that add the `INTERNET` permission, analytics, ads or tracking SDKs will not be merged. CI rejects them automatically.
2. **No Google Play Services.** Use Android platform APIs so the app works on every phone and on F-Droid.
3. **Keep data on the device.** New data belongs in `AppState` (stored locally), never in a remote service.

## Adding or fixing a country pack
Packs live in `app/src/main/assets/packs/<ISO code>.json`. Copy an existing pack and edit it. Every fact must be checkable: put your sources in the pull request description, preferring official ones such as GOV.UK travel advice, embassies and transport authorities.

Key fields:
- `emergency`: put the general emergency number first, with `"primary": true`
- `plugs`, `voltage`, `frequency`
- `exitCode`: the international prefix dialled from that country (00, 011, 010…)
- `schengen`: true for Schengen-area members
- `passportMonthsOnArrival`: set it when the country requires a minimum passport validity on arrival
- `indicativeRatePerGbp`: a rough rate only. Users set their own
- `phrases` should include `"I need a doctor"` with exactly that English text, because the medical card uses it

## Code
Kotlin and Jetpack Compose. Trip rules belong in `logic/TripLogic.kt` with a unit test in `app/src/test`. Run `./gradlew testDebugUnitTest assembleDebug` before opening a pull request.
