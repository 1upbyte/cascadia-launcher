# Project instructions

	- Keep the app offline: no network permissions, backend, runtime downloads, ads, analytics, or Google Play services.
- Validate code changes with `./gradlew testDebugUnitTest lintDebug verifyReleasePrivacy assembleDebug assembleRelease`.
- Do not start emulators or run emulator tests unless explicitly requested. Do not use a deployment watcher.
- Deploy manually and preserve app data. Ask before retrying ADB permission failures; report sandbox errors.
- Commit or push only when explicitly requested. Never commit local SDK paths, build outputs, or signing keys.
