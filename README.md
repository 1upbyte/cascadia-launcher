# Cascadia Launcher

An offline, one-page Android home screen built with Kotlin and Jetpack Compose.
Inspired by alphabet-driven launchers; original code and artwork, with bundled
OFL-licensed Nunito Sans typography. Font license/provenance and rail tuning:
[docs/typography-and-rail.md](docs/typography-and-rail.md).

**Status:** first MVP, not yet a public-production release. See [plan.md](plan.md) for
the agreed behavior, architecture, and remaining release work.

## Features

- System wallpaper behind a single vertical scrolling surface.
- Digital clock/date using the device's time format; tap to open the clock.
- Manually ordered favorites; long-press any app to pin/unpin or open app info.
- Right-edge A–Z / # rail: touch or drag to reveal and navigate all apps.
- The list stays open after release. Background drags scroll; **Back** closes
  browsing. Pressing **Home** resets to favorites.
- Clock and favorites scroll offscreen when jumping to a letter.
- Settings for wallpaper selection, dimming, and favorite ordering. Drag a reorder
  handle or use accessible Up/Down buttons.
- No network permissions, accounts, analytics, ads, or Google Play services.

Only launchable activities in the current user are shown. Latin accents fold to
A–Z; other scripts/numbers use #. Labels sort using the current locale. Disabled
or uninstalled favorites disappear from the UI; saved component references are
retained so a reinstalled app can recover its position.

## Build

Requirements:
- JDK **21** recommended (sources/bytecode target Java 17).
- Android SDK platform **36**, build-tools **36.0.0**, and platform-tools.
- Android **10+** phone/emulator (`minSdk 29`).
- Network access **on the development machine** for initial Gradle/Maven/SDK
  downloads. The installed launcher itself has no network access.

Set `ANDROID_HOME` or create an ignored `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

Then:

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest lintDebug verifyReleasePrivacy
./gradlew assembleRelease
```

The wrapper pins Gradle 8.13 with a distribution SHA-256 checksum. The current
toolchain uses compile/target SDK 36; this does not prevent running on Android 17.
A target-SDK/toolchain update and validation against distribution requirements
are required before public release.

Artifacts:
- Installable development APK: `app/build/outputs/apk/debug/app-debug.apk`
- Optimized, development-signed test APK (build with `./gradlew :app:assembleBenchmark`):
  `app/build/outputs/apk/benchmark/app-benchmark.apk`. Suitable for manual performance
  testing; same package ID/signing key as debug. **Not a production release.**
- Optimized **unsigned** release APK:
  `app/build/outputs/apk/release/app-release-unsigned.apk`

Release signing keys are deliberately not generated or stored in the repository.
Do not publish a debug-signed build.

## Install and use

1. Enable USB debugging on your phone and approve this computer.
2. Connect the phone. If an emulator is also connected, pass `-s SERIAL` to adb.
3. Install and open:

```sh
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n app.cascadia.launcher/.LauncherActivity
```

4. Tap **Set as default home** and approve Android's Home selection dialog.
5. Touch the alphabet. Long-press an app and choose **Pin to favorites**.
6. Open **Settings → Reorder favorites** to set your preferred order.

No runtime permission prompts should be necessary. Wallpaper selection is
delegated to the system. Missing clock/wallpaper handlers show a message rather
than crashing.

To switch back: **Android Settings → Apps → Default apps → Home app**. Keep your
existing launcher installed while testing. If Cascadia cannot open, use Android
Settings (from the notification shade) to change the Home app or uninstall it.

## CLI development loop

With one authorized USB/wireless ADB device connected:

```sh
./scripts/dev.sh
```

This incrementally builds debug, reinstalls with `-r` (preserving favorites and
settings), and opens Cascadia. Re-run after edits; this is a restart, not hot reload.
It does not change your default Home selection or run instrumentation tests.

For multiple devices, select one explicitly:

```sh
./scripts/dev.sh DEVICE_SERIAL
# Or: ANDROID_SERIAL=DEVICE_SERIAL ./scripts/dev.sh
```

The command prints a process-specific Logcat command after deployment. Re-run that
Logcat command after each reinstall because the process ID changes. Installation
stops on a signing-key mismatch; the script never uninstalls or clears your data.
Keep the stock launcher installed as a fallback during development.

## Tests

```sh
./gradlew testDebugUnitTest                 # indexing, identities, pin ordering
./gradlew lintDebug verifyReleasePrivacy   # lint + merged manifest/dependency audit
./gradlew connectedDebugAndroidTest        # requires an unlocked test device
```

**Use a dedicated emulator for instrumentation:** tests clear Cascadia favorites
in their setup. Do not run them against your daily-use installation without
backing up your desired pin order.

`verifyReleasePrivacy` checks the merged release manifest for INTERNET and
ACCESS_NETWORK_STATE and checks runtime dependencies for Google Play services
and Firebase. AndroidX's local signature permission for private broadcasts is
expected; it is not a user-granted runtime permission. The downloadable emoji
font initializer is removed; the launcher uses system emoji.

CI runs unit tests, lint, privacy checks, and debug/release builds. Connected UI
tests and real-device performance validation are separate. The `benchmark` module
contains startup/frame-timing tests and a baseline-profile generator; see
[docs/performance.md](docs/performance.md) for the commands and safety notes.

## Privacy and limitations

See [PRIVACY.md](PRIVACY.md).

Work/private profiles, widgets, notification previews, icon packs, search, and
custom gestures are not part of this MVP. No attempt is made to replace Android's
Recents implementation or bypass third-party-launcher gesture animation limits.
The UI is English-only for now. Large-text, TalkBack, landscape, live wallpaper,
and Android 10/17 need hands-on validation before a public release.

Primary intended physical device: **Pixel 8 / Android 17**. Emulator results are
functional evidence, not proof of Pixel 8 performance. Startup/frame timing must
be measured in an optimized build on actual hardware.

Before publishing: finalize application ID/branding/license, production signing,
current target SDK, privacy/store disclosures, dependency notices, and the
remaining validation checklist in `plan.md`.
