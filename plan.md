# Cascadia Launcher — implementation plan

Status: MVP implemented and locally validated on a Google-free AOSP Android 16 emulator. Kotlin/Compose confirmed. Primary physical target: Pixel 8 running Android 17; physical-device testing is still pending.

Use this document as the project reference for subsequent implementation sessions. Follow the confirmed decisions below; update this plan when scope or architecture changes.

## Goal

Build an original Android home-screen launcher inspired by Niagara’s interaction model, without copying its branding, assets, or proprietary implementation.

Core requirements:
1. One vertically scrolling home surface, with a small alphabet rail on the right.
2. Pinned apps at the top. The alphabetical app list appears only when interacting with the alphabet rail.
3. Fast startup, smooth scrolling, and low resource usage.
4. Wallpaper support.
5. A visible clock.

## Product decisions

### Confirmed decisions

1. **Alphabet interaction:** Touching/dragging the rail opens the list; releasing keeps it visible. Empty space is part of the scrolling surface, not a dismiss target. Back returns to pins only.
2. **One-page behavior:** Clock and pins scroll offscreen above the alphabetical list; they are not sticky. Selecting a letter scrolls to that section.
3. **Pinned apps:** Manual order, with long-press pin/unpin. Confirmed ordering UI: a simple reorder screen with drag handles and accessible move actions.
4. **Clock:** Digital plus date, system 12/24-hour format, no seconds; tap opens the device clock when available.
5. **Wallpaper:** Display system wallpaper and provide a shortcut to the system wallpaper picker.
6. **Distribution and privacy:** Eventual public release. No network permissions and no Google Play services dependency. All core functionality must work offline on Android devices without Google services.
7. **Android support:** Relatively modern Android, without compromising performance. Recommended starting point: Android 10+ (`minSdk 29`), compile/target a current stable SDK satisfying distribution requirements at release. Raising minSdk alone does not make scrolling faster.

8. **Architecture:** Kotlin + Jetpack Compose confirmed. Rust is not required; consider it only for a demonstrated bottleneck.
9. **Primary test device:** Pixel 8 running Android 17.

### Validation follow-ups

- Add an older/modest device or representative emulator configuration for compatibility coverage; Pixel 8 alone is not a low-end performance baseline.
- Verify Android 17 SDK/toolchain availability and compatibility when configuring the project.

### Can be decided later

- Icon style: system app icons, icon packs, or text-only?
- Rail contents: A–Z only, or locale-aware letters and a `#` section?
- Search: excluded from MVP, or needed immediately?
- Hidden apps, notification badges/previews, widgets, work profiles, and gestures?
- Theme controls: font size, spacing, colors, and automatic light/dark mode?

## Proposed MVP behavior

### Home state

- A transparent home window shows the system wallpaper.
- Clock/date and pinned apps form a single vertical scrolling surface.
- A compact alphabet rail remains at the right edge, outside mandatory system gesture insets.
- The all-apps list is absent from the visible layout until the rail is activated.
- Tapping an app launches it; failures produce a non-disruptive message.

### App-browsing state

- Touch or drag along the alphabet rail reveals an alphabetically grouped app list below the pinned section and jumps to the selected letter.
- Show the selected letter prominently while dragging; provide optional, throttled haptic feedback when it changes.
- Releasing the rail leaves the list open. Tapping a row launches that app.
- Back hides the list and restores the prior home scroll position. Empty background accepts scrolling gestures and never dismisses the list.
- A new Home intent resets to the pinned-only state and scrolls to the top. Returning with Back from a launched app preserves the browsing state, so the user can continue choosing apps.
- Letters without apps are visually muted; moving through them selects a documented nearest nonempty section.
- Treat rail dragging as one continuous gesture; do not launch an app on release from that gesture.

Clock and pins scroll offscreen when navigating the alphabetical list. Background drags must scroll the list even when the gesture starts between rows or beside labels.

### App management

- Enumerate launchable activities, not every installed package.
- Identify entries by component (and user/profile if later supported), not display name or package alone.
- Sort labels using locale-aware collation with stable tie-breaking.
- Refresh when apps are installed, removed, updated, or renamed; remove stale pins gracefully.
- Long-press exposes pin/unpin and system app-info actions.
- Persist pin order and basic display preferences locally. No account or backend.

## Technical approach

### Confirmed architecture: Kotlin with native Android UI

Use Kotlin, Jetpack Compose, coroutines, and DataStore, with a single launcher activity and a small repository/state layer.

Why:
- Launcher integration, application icons, wallpaper, accessibility, and lifecycle handling are Android-native concerns.
- Native UI plus lazy lists is a practical starting point for responsive rendering.
- Rust does not replace Android’s UI/rendering pipeline; JNI crossings and a second build toolchain add complexity.
- This app’s initial workload is small. Icon loading, recomposition, and main-thread work are more likely bottlenecks than sorting.
- Compose is a Kotlin UI toolkit, not a Rust renderer. A Rust core feeding Compose still uses the same Compose layout/rendering path. Rust can outperform managed code for some CPU-heavy tasks, but that does not imply faster startup or smoother UI for this app.
- Kotlin runs on Android Runtime (ART), with compilation/profile optimizations; it is not interpreted UI scripting. Traditional Android Views are another native option if measurements justify them, not an automatic performance improvement.

Keep app indexing and section mapping isolated behind interfaces. If profiling reveals CPU-heavy work, evaluate a Rust library through JNI for that specific work. Do not introduce Rust or JNI in the initial implementation.

### Suggested components

- `LauncherActivity`: HOME intent handling, lifecycle, system-bar/window behavior.
- `AppRepository`: launchable activity discovery, change listeners, icon loading/cache.
- `LauncherViewModel`: home/browsing state, selected letter, pins, scroll restoration.
- `AppIndex`: locale-aware ordering, sections, letter-to-row mapping.
- `LauncherScreen`: clock, pins, lazy app list, alphabet rail, gesture handling.
- `PreferencesRepository`: persisted pins and visual settings.

Use stable row keys and immutable state snapshots. Discover apps and decode icons off the main thread. Load icons at their display size, bound cache memory, and avoid repeatedly loading icons or rebuilding the index during drag gestures.

### Android integration

- Declare the appropriate MAIN/HOME/DEFAULT intent filter and guide the user through Android’s default-home selection flow; do not attempt to silently replace their default launcher.
- Use platform launcher/package APIs for discovery and launch. Account for package visibility on modern Android and add only the manifest declarations needed; do not default to broad installed-package permissions.
- Implement system wallpaper display using the platform wallpaper window behavior. Do not request image/media access just to show the existing wallpaper.
- Use system time/date formatting, updating on minute boundaries and time/time-zone changes rather than polling every second.
- Handle insets, display cutouts, system gestures, font scaling, and accessibility semantics explicitly.
- The alphabet rail needs accessible alternatives: TalkBack users must be able to open and navigate all apps without precise dragging.
- Work/private profiles are outside the first MVP unless requested; document that limitation.

### Offline and public-release constraints

- Do not declare `INTERNET` or `ACCESS_NETWORK_STATE`; check the merged release manifest in CI so transitive libraries cannot silently add them.
- No Google Play services/Firebase, ads, analytics, remote crash reporting, accounts, or runtime downloads. AndroidX/Compose do not require Google Play services on the device.
- Build-time dependency downloads and Android SDK tooling are distinct from runtime network use; a normal development build may access artifact repositories.
- Keep user preferences local. Decide explicitly whether Android OS backup is allowed, since system-managed backup is separate from the app's network permissions.
- Use a minimal dependency set, audit licenses, and publish accurate privacy/distribution disclosures. Support a signed APK install path independent of Google Play.

## Performance and validation

Provisional targets, to be measured on the Pixel 8 running Android 17 using a release build, with additional older-device coverage when available:

- Smooth scrolling and rail dragging at the device refresh rate, with no sustained jank.
- Visible cached home content within approximately 500 ms on warm entry; cold-start goals set after the first benchmark baseline.
- Test indexing and navigation with hundreds of apps and long/non-Latin labels.
- No package queries, disk reads, or icon decoding in the gesture path.
- No ongoing background polling; clock updates should not cause whole-list redraws.

Measure startup and scroll performance using Macrobenchmark, frame timing, and system traces. Add a baseline profile once the main flows stabilize. Track memory and cache behavior rather than claiming an arbitrary universal memory budget.

## Implementation milestones

### 1. Project and launcher shell

- Follow the confirmed product decisions and Kotlin/Compose architecture in this plan.
- Create Gradle/Kotlin Android project; document JDK/SDK requirements and build commands.
- Verify installed SDK platforms/build-tools and configure SDK location.
- Implement home activity and default-launcher onboarding.
- Show system wallpaper and a clock on a physical device.

Acceptance: app builds, installs, can become the default launcher, and survives being reopened or recreated.

### 2. App inventory and pins

- Discover launchable activities and display labels/icons.
- Persist pin selection and order; implement launching and app-info actions.
- Handle installation/removal/update events.

Acceptance: pins survive restart, launch correctly, and do not break when an app is removed.

### 3. Alphabet navigation

- Implement the home/browsing state machine and lazy sectioned list.
- Implement rail taps/drags, selection feedback, empty sections, and scroll restoration.
- Add accessible app-browsing entry and navigation.

Acceptance: the app list appears only after rail activation or its accessibility equivalent; scrolling and dismissal follow the agreed behavior.

### 4. Polish and settings

- Add clock formatting, wallpaper-picker entry, contrast controls, and basic appearance settings.
- Test font scaling, long labels, gesture navigation, light/dark wallpapers, and process recreation.

Acceptance: content remains legible and all apps remain reachable on supported phone configurations.

### 5. Performance and release readiness

- Benchmark startup and scrolling on the target device; fix measured bottlenecks.
- Add tests, baseline profile, release build configuration, and installation documentation.
- Review public-distribution requirements, signing/key management, dependency licenses, and privacy disclosures.
- Verify no network permissions in the merged release manifest and no Google Play services dependencies; test core flows on an AOSP emulator without Google services.

Acceptance: measured performance meets the agreed targets and the launcher is reliable enough for daily use.

## Test plan

- Unit: sorting/grouping, empty letters, duplicate labels, stable identities, stale pins, state transitions.
- UI: pin/unpin/reorder, letter taps and drags, app launch, Back/Home behavior, restored scroll position.
- Integration: default-home role, package changes, permission/visibility behavior, clock time-zone changes, wallpaper changes, and process death.
- Accessibility: TalkBack, larger text, contrast, touch target sizes, and non-drag alternatives.
- Device: gesture and three-button navigation, supported Android versions, portrait/landscape, and large app inventories.

## Out of scope for the initial MVP

Notification access/previews, widgets, icon packs, folders, weather/network services, custom gesture shortcuts, cloud sync, and a replacement for Android’s recent-apps UI. Third-party launcher/system gesture animation limitations are platform constraints, not something Rust can solve.

## Environment notes

Already observed locally:
- Homebrew OpenJDK 21 is installed and `java` works.
- `sdkmanager`, `avdmanager`, and `adb` are on PATH.
- `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` are unset.
- SDK configured through ignored `local.properties` at `~/Library/Android/sdk`.
- SDK platform 36, build-tools 36.0.0, platform-tools, and emulator installed. AGP also provisioned build-tools 35.0.0 during initial setup.
- A Google-free AOSP Android 16 / ARM64 emulator is available as `Cascadia_AOSP_36`.
- No physical phone was connected during implementation.

## Implementation status

### Completed MVP

- Gradle wrapper with distribution checksum, Kotlin/Compose project, adaptive icon, HOME activity and default-home role flow.
- System wallpaper display/picker shortcut, digital clock/date/clock-app action, and local dimming control. Clock launch resolves the clock's public launcher activity rather than requesting SET_ALARM, which some clocks require even to handle SHOW_ALARMS.
- Current-user launchable app discovery, package/locale change refresh, asynchronous bounded icon cache, locale-aware label sorting, stable component identities, and app launch/app-info actions.
- Persistent manual favorites, long-press pin/unpin, reorder screen with drag handles and accessible Up/Down actions.
- Alphabet taps/drags, selected-letter feedback/haptics, nearest occupied section selection, one lazy scrolling surface, non-sticky clock/pins, and Back/Home handling.
- TalkBack semantic action to open all apps without precise rail dragging; app rows expose launch/long-press actions.
- Local DataStore persistence with corruption recovery. Cloud backup/device transfer excluded. Downloadable emoji initialization disabled.
- Release manifest/dependency privacy audit and CI for unit tests, lint, privacy checks, and debug/release builds.
- Macrobenchmark module for cold/warm activity startup and rail frame timing, plus a separate unminified profile-generation variant. An app-specific baseline profile was generated successfully on AOSP API 36 and is included at `app/src/main/baseline-prof.txt` (startup and alphabet navigation paths, including application-specific rules). See `docs/performance.md`.
- `README.md`, `PRIVACY.md`, and project `AGENTS.md` document operation, safety constraints, and future-session context.

### Validation achieved

- `:app:assembleDebug`, `:app:assembleRelease`, `:app:lintDebug`, and `:app:verifyReleasePrivacy` passed.
- 13 unit tests passed, covering indexing, section fallback, duplicate identities/labels, manual pin order, stale pins, reorder boundaries, and a 2,002-entry inventory.
- 9 connected UI tests passed on the AOSP Android 16 emulator (browse/Back, background scrolling, rail release, pin/unpin, root Back, Home-intent handling, reorder persistence, free-scroll restoration across activity recreation, and opening the clock without alarm permission).
- Baseline-profile generation passed on AOSP API 36. Three macrobenchmark smoke tests (cold/warm activity startup and alphabet dragging) passed on the emulator with the EMULATOR warning explicitly suppressed. Their timings are not physical-device performance evidence.
- Optimized unsigned release and development-signed benchmark APKs are approximately 1.9 MB including the app-specific baseline profile; debug APK is approximately 40 MB with tooling.
- Release APK passed 16 KB ZIP alignment checking; all packaged 64-bit native libraries also have at least 16 KB ELF load-segment alignment. Full device compatibility still requires hardware/16 KB-system testing.
- Merged debug/release manifests contain neither network permission; release runtime dependency audit found no Google Play services/Firebase.
- Manual optimized-build checks with Wi-Fi/mobile data disabled confirmed system wallpaper, letter navigation with clock/pins offscreen, pinning, app launch, system Home returning to favorites, clock-widget launch, and the system wallpaper chooser. Default Home was set via ADB on the emulator; the user-facing role approval flow still needs physical-device validation. No physical-device performance claims are made.

### Remaining before public release

- Regenerate the app-specific baseline profile when startup or navigation paths change, and measure its impact on physical hardware.
- Pixel 8 / Android 17 testing, 60/120 Hz measurements, cold/warm startup and memory profiling, realistic hundreds-of-app inventory, lower-end hardware, and Android 10 compatibility.
- Hands-on TalkBack, large-font, contrast, landscape, live-wallpaper, process-death, app install/update/remove, and system gesture/default-Home testing.
- Review work/private-profile support requirements; current MVP intentionally shows only the current user.
- Current toolchain compiles/targets SDK 36. Upgrade/validate target SDK and tooling against current distribution requirements before publishing; running on Android 17 is not the same as targeting its API.
- Final application ID, branding, source license, dependency notices, production signing/key custody, privacy/store disclosures, and release channel.
- UI is currently English-only; localize user-facing strings before a broad multilingual release.

No production signing key has been created, no public release has been uploaded, and no changes have been committed automatically.
