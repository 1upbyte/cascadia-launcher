# Project reference

Read `plan.md` before changing product behavior. It records the user's approved
requirements. Read `README.md` for build/install/testing instructions.

## Constraints
- Kotlin + Jetpack Compose; do not introduce Rust without a measured need.
- No INTERNET/ACCESS_NETWORK_STATE permissions, Firebase, analytics, ads,
  Google Play services, runtime downloads, or backend.
- Favorites first; all apps only after rail activation (or accessibility action).
- Rail release keeps apps open. Empty background scrolls, never dismisses.
- Back closes browsing; Home resets favorites. Clock/pins are not sticky.
- Manual favorites ordering with drag handles and accessible move buttons.
- Android 10+; primary physical target is Pixel 8 running Android 17.

## Validation
- `./gradlew testDebugUnitTest lintDebug verifyReleasePrivacy assembleDebug assembleRelease`
- `./gradlew connectedDebugAndroidTest` on a **dedicated emulator**; tests reset pins.
- Do not claim physical-device performance based on debug builds/emulators.
- Do not commit machine-specific SDK paths, generated build outputs, or signing keys.
- Update the implementation status in `plan.md` after meaningful milestones.

## Phone development loop
- Use `python3 scripts/dev-watch.py DEVICE_SERIAL` for automatic optimized builds, reinstall, and restart on source/config edits. Check for an existing watcher before starting another; it enforces one instance per checkout.
- While the watcher is running, let it deploy edits instead of manually reinstalling. It preserves favorites/settings and keeps the selected device fixed. See README for usage.
