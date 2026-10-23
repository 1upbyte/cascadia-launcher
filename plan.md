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
- Clock/date and pinned apps form a single vertical scrolling surface. Use a shared 28 dp left inset, 48 sp clock, 16 sp weekday/date, 48 dp app icons, 20 sp single-line labels, 20 dp icon-to-label gap, and at least 64 dp rows. Position the group responsively with an upper bias, moving it upward as favorites grow; overflow remains scrollable. Omit the brand banner, Favorites heading, and recurring browsing hint; access Settings through the Cascadia Settings shortcut in the app-list footer.
- The all-apps list ends with app-style Cascadia Settings and GitHub shortcuts; GitHub opens `https://github.com/1upbyte/cascadia-launcher` through an installed browser/app. Footer shortcuts scroll with the list.
- A compact alphabet rail remains at the right edge within safe drawing insets. Its gesture-exclusion region requests priority over system Back only on the rail (subject to Android's exclusion limits).
- The all-apps list is absent from the visible layout until the rail is activated.
- Tapping an app launches it; failures produce a non-disruptive message.

### App-browsing state

- Touch or drag along the alphabet rail reveals the selected letter's section below the pinned area; while holding, other letters' app sections are absent. Anchor every held category at the same height as the first rail letter. Near the end, clamp to the natural list bottom rather than adding blank space below the final apps. Clock/pins stay offscreen; oversized sections remain scrollable after release. Releasing restores the full alphabetical list with the heading and app rows at exactly their held positions, ready for free scrolling. The rail's letters remain available for continuous scrubbing.
- While dragging, bend the visible letters left into a broad Gaussian bell curve centered continuously on the finger's vertical position. Adjacent letters taper back toward the right edge; dragging left deepens and broadens the curve. Keep glyph size and vertical positions stable; animate press/release over 150 ms. Show the selected letter in a large circular preview beside the finger during rail gestures; hide it on release. Allow pulling the rail farther left while keeping the preview on screen. Provide a system CLOCK_TICK haptic when the letter changes, respecting Android settings.
- Releasing the rail leaves the list open. Tapping a row launches that app.
- Back hides the list and restores the prior home scroll position. Empty background accepts scrolling gestures and never dismisses the list.
- A new Home intent resets to the pinned-only state and scrolls to the top. Returning with Back from a launched app preserves the browsing state, so the user can continue choosing apps.
- Hide letters without apps. Show only occupied sections in a compact rail anchored at the lower right with a 192 dp margin above the safe-area bottom and 19 sp slots (shrinking to fit on short displays). Letters use bundled OFL-licensed Nunito Sans regular at 16 sp, centered 14 dp from the right edge, with a subtle shadow instead of a pill backdrop. The invisible touch area extends 68 dp leftward to meet the app rows. Drag hit-testing uses exactly the visible letter slots. Nunito Sans also supplies the launcher typography. See `docs/typography-and-rail.md` for licensing/provenance and intentional differences from the reference.
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

### Reference-driven visual refinement

- Bundled unmodified OFL-licensed Nunito Sans 3.101 with its matching copyright/license inside the APK; no proprietary code or artwork included.
- Tuned rail text/spacing, 150 ms transition, broader continuous finger-centered bend, and system clock-tick haptics; preserved hidden unused letters, wide hitbox, and the then-current no-popup preference (superseded below).
- Applied Nunito Sans throughout typography and tuned app-label spacing. See `docs/typography-and-rail.md`; visual/haptic parity still needs hands-on comparison.
- Refinement validation: 18 unit tests and all 12 connected UI tests passed on the dedicated AOSP API 36 emulator, including all three rail tests. Debug lint, release privacy audit, Android-test compilation, and unsigned release build passed. Verified the bundled font and matching OFL notice inside the release APK.
- Updated debug build deployed to the connected Pixel 8 with `scripts/dev.sh`, preserving favorites/settings. No instrumentation ran on the phone. The dedicated emulator was shut down after validation.

### Overall layout refinement

- Shared geometry aligns the clock, favorites, and all-apps icons at 28 dp; app labels start at 96 dp. Increased icons to 48 dp, reduced labels to 20 sp, and kept 64 dp minimum rows. Long labels ellipsize while launch semantics retain their full names.
- Reduced clock to 48 sp and date to 16 sp with a compact locale-aware weekday/date. Responsive upper-biased home placement adapts to favorite count and viewport; short screens still scroll.
- Removed the brand banner, Favorites heading, and recurring browsing hint. Settings is available in the app-list footer, with empty-state help and default-home onboarding retained when needed. Browsing section headings are smaller and aligned to the icon column.
- Reference resource dimensions inform the layout; exact customized Niagara placement was not visually verified because the connected phone was locked during reference capture. No lock-screen bypass attempted. These are independent approximations, not recovered proprietary layouts.
- Validation: 23 unit tests, 13 connected UI tests on the dedicated AOSP emulator (including clock/label alignment and Settings reachability), debug lint, Android-test compilation, and privacy audit passed. Updated debug installed on Pixel 8 without clearing data; no phone instrumentation.

### Held-letter isolation and smoothness pass

- Only the held letter's app section is materialized during rail gestures; release restores all sections at the same letter. Accessible browse entry still opens the full list. Back/Home and recreation continue to preserve their existing behavior.
- Replaced coroutine-delayed scrolling with `requestScrollToItem` in the input callback, co-timed with section remeasurement. Rail callbacks occur only on slot changes; continuous finger movement stays draw-layer-only. Cached icons are supplied immediately and bypass dispatcher hops on cache hits.
- The activity requests the highest refresh rate supported at the current native resolution, leaving Android in charge of power/thermal/user constraints. The Pixel window reports a 120 Hz preference; this is not evidence of achieved frame rate.
- `scripts/dev.sh` now defaults to the optimized, development-signed benchmark variant to avoid evaluating debug Compose overhead. `CASCADIA_BUILD_TYPE=debug` explicitly selects debugger/tooling builds. No app-data clearing or default-Home changes.
- Existing cumulative Pixel debug counters showed 128/725 janky frames (17.66%), UI 90th-percentile 40 ms vs GPU 90th-percentile 3 ms. Uncontrolled history, not an isolated rail benchmark; no valid before/after speed claim. Phone was screen-off/locked during this pass, so hands-on optimized-build frame timing remains pending.
- Validation: 29 unit tests and all 14 connected UI tests passed on dedicated AOSP API 36, including held-section isolation/release positioning, recreation, and Back/Home. Lint, privacy checks, Android-test compilation, optimized build, shell syntax and diff checks passed. Optimized build deployed to Pixel with settings preserved; no phone instrumentation.

### Held-section vertical positioning

- Held results now follow the selected letter's normal vertical rail slot; lower letters appear lower when space allows. Larger sections shift upward to keep their rows visible; oversized sections start at the top. Heading spacing lives inside the selected section item so the clock/pins cannot peek into the blank area above it.
- Full-list positioning after release remains unchanged. No per-frame list work was added; placement changes only when selecting a different letter or changing layout constraints.
- Validation: 33 unit tests, all 14 connected UI tests on dedicated AOSP API 36 (including held-position/hidden-clock assertions), lint, test compilation, privacy audit, and optimized build passed. Initial full UI attempt timed out; the targeted case and complete rerun passed. Optimized APK installed successfully on Pixel 8 before it was disconnected; settings preserved.

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

### Rail-release position correction

- Retain the selected section’s vertical spacing when restoring the full list on release, preserving heading/app positions instead of jumping them to the top. Preserve this anchor across recreation.
- Added UI regression assertions comparing heading and first-app positions before and after release.
- Validation: unit tests, lint, release privacy audit, debug/release builds, Android-test compilation, and optimized benchmark build passed. Optimized APK installed on the connected Pixel 8 with app data preserved. No emulator or phone instrumentation tests ran; on-screen release-position verification remains pending because the phone screen was asleep.

### Automatic phone development loop

- Added `scripts/dev-watch.py`: dependency-free source/config watcher invoking the existing optimized build/install/restart flow, with debounce, fixed device selection, one watcher per checkout, and app data preserved. Documented start/stop and failure retry behavior in README.
- Verified on Pixel 8: initial automatic deployment and a subsequent source timestamp change both built, installed successfully, and cold-started the activity. Watcher left running for subsequent edits. Shell syntax, watcher help, and diff checks passed.

### Fixed category start height

- All rail categories now use the first-letter start height, independent of the chosen letter and category size. Final categories clamp to the natural bottom. Removed artificial trailing scroll space; held and released layouts reserve identical remaining content height. Supersedes the earlier per-letter vertical positioning.
- Validation: unit tests, debug lint, release privacy audit, debug/release builds and Android-test compilation passed. On-device visual verification is pending while Pixel 8 is asleep; no instrumentation ran on the phone.
- Watcher successfully installed the corrected optimized build on Pixel 8 and cold-started Cascadia, preserving app data.

### Launcher footer shortcuts

- Added app-style Cascadia Settings and GitHub rows after the alphabetical apps. Settings opens the existing local settings dialog; GitHub delegates the repository URL to an installed app, with a fallback message if none is available. No network permission added. Footer height participates in held/released positioning to avoid a release jump.
- Validation: unit tests, lint, release privacy verification, debug/release builds and Android-test compilation passed. Added a footer/settings navigation UI regression test; instrumentation was not run on the daily-use phone.
- Watcher built and installed the optimized footer update on Pixel 8 and opened the launcher successfully, preserving favorites/settings.

### Footer icon refinement

- Footer shortcuts use rounded-square (squircle) backgrounds and bundled vector icons. GitHub uses the official [Octicons mark](https://github.com/primer/octicons/blob/main/icons/mark-github-16.svg), with its MIT license bundled in APK assets; Settings uses an original gear vector. No runtime asset downloads.
- Validation: unit tests, debug lint, release privacy verification, and debug/release builds passed.
- Watcher installed the optimized icon update and cold-started Cascadia on Pixel 8 successfully.

### Configurable icon shape

- Added persisted Square, Squircle, and Circle choices in Settings, defaulting to Squircle for new and existing installs. Applies to favorites, alphabetical apps, placeholders and footer shortcuts. Adaptive icon layers are rendered before the launcher applies its chosen mask; legacy transparent icons receive a white backing. Shapes update without re-querying packages or decoding icons again.
- Validation: unit tests, lint, privacy audit, debug/release builds, and Android-test compilation passed. Added regression coverage for adaptive icon corner pixels and shape selection/persistence across recreation; connected tests were not run on the daily-use phone.
- Watcher installed the optimized shape-setting build on Pixel 8 and cold-started the launcher successfully; favorites/settings preserved.

### Reference squircle geometry

- Inspected the locally available Niagara APK reference and replaced the rounded-square approximation with a symmetric four-cubic squircle matching its mask geometry. Shared outline applies to app icons and footer backgrounds without changing saved shape preferences.
- Validation: unit tests, lint, privacy audit, and debug/release builds passed; diff checks clean.
- Watcher installed the optimized reference-mask update on Pixel 8 and opened Cascadia successfully, preserving app data.

### Lower rail and cleaner favorites page

- Moved the rail center 24 dp lower using asymmetric safe-area padding; its touch bounds move with the letters and shrink to fit short displays. Removed the Settings action and its empty spacing from the favorites page; settings stays accessible through the app-list footer. Updated UI navigation checks accordingly.
- Validation: unit tests, lint, privacy audit, debug/release builds, Android-test compilation, and diff checks passed. Updated tests enter settings through the footer; connected tests were not run on the daily-use phone.
- Watcher installed the optimized update and cold-started Cascadia on Pixel 8, preserving app data.

### Bottom-biased alphabet rail

- Anchored the rail to the lower-right safe area with a 96 dp bottom margin, replacing the small downward offset. The full rail remains available and shrinks within the viewport on short screens; hit bounds follow the glyphs.
- Validation: unit tests, lint, privacy audit, debug/release builds, and diff checks passed.
- Watcher installed the optimized bottom-rail update and cold-started Cascadia on Pixel 8, preserving app data.

### Rail bottom margin adjustment

- Doubled the alphabet rail bottom margin from 48 dp to 96 dp, as requested.
- Validation: unit tests, lint, privacy audit, debug/release builds, and diff checks passed.
- Watcher installed the optimized 96 dp margin update on Pixel 8 and cold-started Cascadia successfully, preserving app data.

### Rail bottom margin doubled again

- Increased the alphabet rail bottom margin from 96 dp to 192 dp at the user’s request.
- Validation: unit tests, lint, privacy audit, debug/release builds, and diff checks passed.
- Watcher installed the optimized 192 dp margin update on Pixel 8 and cold-started Cascadia successfully, preserving app data.

### Finger-following selected-letter preview

- Added a 64 dp circular preview with a 36 sp selected letter beside the active finger. Preview follows movement, stays within the left edge, and disappears on release. Extended leftward rail pull from a 140 dp cap to up to 280 dp, limited by screen space for the preview. Gesture selection, haptics, and post-release list position remain unchanged. This supersedes the earlier no-popup preference.
- Validation: unit tests, lint, privacy audit, debug/release builds, Android-test compilation, and diff checks passed. Added a preview-follow/release UI regression case; connected tests were not run. Pixel 8 disconnected from ADB during deployment, so installation is pending reconnection.
- Optimized development-signed benchmark APK also built successfully and is ready for installation once the phone reconnects.
- Pixel 8 reconnected; watcher installed the optimized circular-preview/extended-pull update and cold-started Cascadia successfully, preserving app data.

### Smaller letter preview

- Reduced the circular preview from 64 dp to 48 dp and its letter from 36 sp to 28 sp, retaining finger-following placement and rail pull behavior.
- Validation: unit tests, lint, privacy audit, debug/release builds, and diff checks passed.
- Watcher installed the optimized smaller-preview update and opened Cascadia on Pixel 8 successfully, preserving app data.

### App scale preference

- Added a live-preview App scale slider in Settings (75–150%, 5% steps, default 100%) with a reset action. Scales favorites, app rows, section labels and footer shortcuts together; keeps rail and clock at their existing sizes. Saves on slider release; updates list navigation geometry to match. Touch rows remain at least 48 dp. Icons use a stable cache resolution suitable for the largest setting to avoid redecoding throughout a drag.
- Validation: unit tests (including invalid saved-scale handling), lint, privacy audit, debug/release builds, Android-test compilation, and diff checks passed. Added a UI regression for scaled favorites, slider persistence across recreation, and reset; connected tests were not run on the daily-use phone.
- Watcher installed the optimized app-scale update and cold-started Cascadia on Pixel 8 successfully, preserving app data.

### Selected-letter section spacing

- Matched the held section's vertical anchor to the alphabet rail's actual position 192 dp above the safe-area bottom, removing the oversized gap above selected app sections. Preserved the in-progress release-position adjustment in `LauncherScreen.kt`.
- The optimized benchmark build succeeded and installed on Pixel 8 over ADB; Cascadia cold-started successfully. Automated tests were not run for this change.
