# Cascadia privacy

Cascadia works entirely offline.

- The app does not request INTERNET or ACCESS_NETWORK_STATE.
- No accounts, analytics, advertisements, telemetry, remote crash reporting,
  weather requests, or Google Play services are included.
- Launchable app labels, component identifiers, and icons are read locally to
  display and launch apps. App inventory and icons are held in memory.
- Favorite component identifiers, their order, and wallpaper dimming are saved
  in app-private Android DataStore on the device.
- Android cloud backup is disabled. Android 12+ extraction rules also exclude
  application data from cloud backup and device transfer.
- Wallpaper rendering and selection use Android's system facilities; Cascadia
  does not read your image library or upload wallpaper images.
- Clock and app-info actions open other installed applications/system screens.
  Their independent privacy policies and permissions apply.
- No downloadable emoji font provider is initialized.

Clearing Cascadia's app data or uninstalling it removes its saved preferences.
Android/your device vendor may independently collect system diagnostics; Cascadia
does not control the operating system or applications you launch.

Development tooling may download build dependencies. This is separate from the
permissions and behavior of the installed launcher.
