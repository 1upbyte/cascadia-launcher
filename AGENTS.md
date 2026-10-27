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
- Do not start emulators or run emulator tests unless the user explicitly requests them.
- Do not claim physical-device performance based on debug builds/emulators.
- Do not commit machine-specific SDK paths, generated build outputs, or signing keys.
- Update the implementation status in `plan.md` after meaningful milestones.

## Phone development loop
- Deploy to the phone manually; do not start or use a watcher.
- If ADB permissions are missing, ask the user directly before retrying.
