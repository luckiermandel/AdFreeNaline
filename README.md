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

## Versioning

When you ship an update, bump the app version in `app/build.gradle.kts` (`versionCode` and `versionName`) and tag the same release in git:

```bash
# Example: releasing 1.1
# 1. Update versionCode / versionName in app/build.gradle.kts
# 2. Commit, then tag:
git tag v1.1
git push origin v1.1
```

Keep the git tag in sync with `versionName` (e.g. `versionName = "1.1"` → tag `v1.1`). If you update the app but forget the tag, add or move the tag before pushing.

## F-Droid distribution

F-Droid does **not** use Google Play–style console uploads. They clone your public git repo, build the APK on their servers, and publish it on [f-droid.org](https://f-droid.org). Getting listed requires **two separate metadata steps** — do not skip either.

### How the two pieces fit together

| Where | What it is | What F-Droid uses it for |
|-------|------------|--------------------------|
| **Your repo** (`fastlane/metadata/…`) | Store text, icon, screenshots, changelogs | The **listing** users see on the F-Droid website and in the F-Droid client (name blurb, description, images) |
| **[fdroiddata](https://gitlab.com/fdroid/fdroiddata)** (their GitLab repo) | One YAML file per app | The **build recipe** — where to clone, which git tag to check out, which Gradle command to run |

Your Kotlin source is not “metadata.” Metadata is everything *about* the app that is not code: descriptions, images, version notes, and build instructions.

**Yes, you need both:** fastlane files in your project for the store page, **and** a merge request to fdroiddata so their build farm knows how to compile your app.

### 1. Store listing — in this repo

Create these files (Fastlane folder layout; you do **not** need the Fastlane tool installed):

```
fastlane/metadata/android/en-US/
  short_description.txt      # one line, max 80 chars, no trailing period
  full_description.txt       # full store description (adapt from this README)
  images/icon.png              # 512×512 app icon
  images/phoneScreenshots/1.png
  images/phoneScreenshots/2.png  # at least 2 real phone screenshots
  changelogs/1.txt             # filename = versionCode; max 500 chars
```

Example `short_description.txt`:

```
Ad-free GPS run tracker with maps, stats, and route creator
```

Example `changelogs/1.txt` (for `versionCode = 1`):

```
Initial release: GPS run tracking, stats, route creator, CSV export.
```

On every release, add `changelogs/<versionCode>.txt` (e.g. `changelogs/2.txt` when `versionCode` becomes `2`).

### 2. Build recipe — fdroiddata merge request

This file lives in **F-Droid’s repo**, not yours. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata), add `metadata/com.luckierdev.adfreenaline.yml`, and open a merge request titled `New App: com.luckierdev.adfreenaline`.

Example YAML (replace placeholders with your real name, email, and repo URL):

```yaml
Categories:
  - Sports & Health
License: MIT
AuthorName: YOUR_NAME
AuthorEmail: your@email.com
SourceCode: YOUR_REPO_URL
IssueTracker: YOUR_REPO_URL/issues
WebSite: YOUR_REPO_URL

AutoUpdateMode: Version
UpdateCheckMode: Tags

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0
    subdir: .
    gradle:
      - :app:assembleRelease
```

- `commit` must match a **git tag** on your repo (`v1.0` for `versionName "1.0"`).
- `versionCode` / `versionName` must match `app/build.gradle.kts` at that tag.
- CI on the merge request must build green; then a human reviewer merges it. The app usually appears on f-droid.org within ~24–48 hours.

For later releases, add a new `Builds:` entry (or rely on `AutoUpdateMode: Version` + `UpdateCheckMode: Tags` after the first inclusion).

### F-Droid checklist (do not forget)

**Before first submission**

- [ ] Public git repo with full source pushed (no secrets, no keystores)
- [ ] `LICENSE` in repo root (MIT)
- [ ] `fastlane/metadata/android/en-US/` files created (listing text, icon, screenshots, changelog)
- [ ] Release signing configured; `./gradlew assembleRelease` succeeds locally
- [ ] Git tag pushed (e.g. `v1.0`) matching `versionName`
- [ ] fdroiddata merge request submitted with YAML above
- [ ] Manual device testing done (see `RELEASE_TODO.txt`)

**On every release**

- [ ] Bump `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Add `fastlane/.../changelogs/<versionCode>.txt`
- [ ] Commit, tag (`v` + `versionName`), push branch and tag
- [ ] Update fdroiddata `Builds:` entry if needed for the new version

More detail: `FDROID_SUBMIT_CHECKLIST.txt` (step-by-step gate before GitLab), `TODO_VER_2.txt` (F-Droid guide), and `RELEASE_TODO.txt` (signing and testing).

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
