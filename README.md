# Ad-Free-naline

A local-first Android run tracker built with Kotlin and Jetpack Compose. GPS tracks your runs, history stays on device, and maps use free tile sources (no API keys, no ads).

**Package:** `com.luckierdev.adfreenaline`  
**Min SDK:** 26 (Android 8.0)

## Features

### Run tracking
- Start, pause, resume, and finish runs
- Live distance, duration, average pace/speed, and estimated calories
- Red live route polyline on the map
- Foreground service with an ongoing notification while a run is active (distance, speed, calories)
- GPS noise filter (segments below ~0.5 km/h are ignored)
- Runs shorter than **50 m** are discarded on finish
- Active run auto-resumes after app restart (within 24 hours), including pause state

### Maps
- **MapLibre** vector maps via [OpenFreeMap](https://openfreemap.org) (OpenStreetMap-based data)
- Optional **dark map style**
- Optional **Esri World Imagery** satellite overlay
- No Google Maps SDK and no map API keys required
- Map attributions and license links in **Settings → Data & licenses**

### Route creator
- Tap the map to place waypoints and build custom routes
- Save, edit, categorize, apply, and delete routes
- Configurable creator route color

### Stats & history
- **Stats** tab: summary cards, time-window graphs (day/week/month/year/all), run streak counter, and unlockable achievements
- Grouped run history (today, last week, older)
- Country code recorded per finished run (via reverse geocoding when available)

### Goals & alerts
- Per-run and weekly distance goals (km or mi)
- In-app progress while tracking
- Optional sound/vibration alerts when a goal is reached (custom notification sound supported)

### Settings
- Dark mode, km/mi, battery-saver GPS mode
- Health profile: sex, age, height, weight (used for calorie estimates)
- Calorie goal per run
- Optional daily streak reminders (three humorous notifications per day)
- Export run history to CSV
- Delete all app data (full reset)

### Onboarding
- First-launch profile setup (sex, age, height) with explanation of why it is collected; can be skipped

## Calorie estimates

Calories burned are **estimates only**. The app uses a MET-based formula adjusted by your profile (sex, age, weight), pace, and duration. They are not medical or dietary advice and may differ from actual energy expenditure. The app shows this disclaimer during onboarding and in Settings.

## Tech stack

| Area | Libraries |
|------|-----------|
| UI | Jetpack Compose, Material 3 |
| Architecture | ViewModel, Kotlin coroutines, StateFlow |
| Location | Android `LocationManager` via `LocationManagerCompat` (no Google Play Services) |
| Maps | MapLibre GL (`android-sdk-opengl`), OpenFreeMap styles, optional Esri raster tiles |
| Persistence | Room (runs, routes, settings, active session) |
| Utilities | osmdroid `GeoPoint` only (not used for map rendering) |

Legacy `SharedPreferences` data is migrated into Room on first launch after upgrade.

## Permissions

- **Location** (fine + coarse) — GPS tracking and map centering
- **Notifications** (Android 13+) — live run updates and goal alerts
- **Foreground service (location)** — keep tracking while the screen is off or another app is in front

The app does **not** request background location access; tracking relies on a foreground service while a run is active.

## Build & run

### Android Studio
1. Open the `RunTrackerApp` folder in Android Studio.
2. Sync Gradle.
3. Run on a device or emulator with GPS (or mocked location).
4. Grant location and notification permissions when prompted.

### Command line
```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (device/emulator required)
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

Release builds require a signing config (not set up in the repo yet). See `RELEASE_TODO.txt`.

## Tests

Unit tests cover calorie math, CSV export, entity mapping, and map tile URLs. Instrumented tests cover Room DAOs, legacy prefs migration, and a basic launch smoke test.

## Data & privacy

- All run history, routes, and settings are stored locally in SQLite via Room (`adfreenaline.db`).
- No user accounts, cloud sync, or analytics in the current app.
- Android backup is disabled (`allowBackup=false`).

## Roadmap (not implemented yet)

- Release signing and R8/ProGuard hardening
- Custom challenges UI (schema exists in Room; no screen yet)
- Social feed, segments, accounts, and cloud sync
- Map matching and advanced pace smoothing
- Dedicated notification icons (currently system drawables)
