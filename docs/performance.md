# Performance workflow

Functional emulator tests do not establish real-device frame rates or startup
latency. Use an unlocked Pixel 8 running Android 17 with battery saver off,
minimal background load, and a stable thermal state. Keep a copy of its pin
order before test runs. Do not start emulators or run emulator tests unless
explicitly requested by the user.

## Variants

- `debug`: debuggable, developer installation/UI tests. Not a performance baseline.
- `release`: R8-minified, resource-shrunk, unsigned. Intended production artifact.
- `benchmark`: same optimization as release, non-debuggable, shell-profileable,
  signed with the development key solely so local benchmarks can install it.
- `profile`: non-debuggable but unminified, development-signed. Generates
  unobfuscated baseline rules that R8 can rewrite when building release.

All app variants use the same package ID. Installing a different variant may
replace your existing development installation. Never publish benchmark/profile
APKs. Public release requires your production signing key.

An initial baseline profile generated on the AOSP API 36 emulator is checked in at
`app/src/main/baseline-prof.txt`. It covers startup and alphabet navigation; update
it as important journeys change. Its presence is not a real-device speed claim.

## App-specific baseline profile

Dependency baseline profiles are merged by AGP; the checked-in profile adds
Cascadia's own paths. Profile regeneration is a separate task that requires an
explicit user request and is not part of routine build/install validation.

## Measure startup and alphabet dragging

On a physical device, temporarily select the stock launcher as default Home.
Otherwise pressing Home can restart Cascadia during a cold-start test and distort
results. Select Cascadia again afterward for daily use and actual Home-return
checks. The startup tests measure explicit activity entry; validate system Home
and gesture-return behavior separately.


```sh
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=app.cascadia.launcher.benchmark.LauncherBenchmark
```

The suite measures cold startup, warm startup, and frame timing while dragging the
alphabet rail. It uses five iterations and partial compilation. Benchmark JSON
and Perfetto traces appear under the benchmark module's build outputs; inspect
the reported paths. Record device, OS/build, refresh rate, thermal state, app
count, variant, and commit alongside the results.

## Rail smoothness investigation

The CLI deploy helper now defaults to the optimized `benchmark` variant. Use
`CASCADIA_BUILD_TYPE=debug ./scripts/dev.sh` only when debugger/tooling support is
needed. Both builds use the development key and preserve existing app data.

The pre-change Pixel debug process had 725 accumulated frames, 128 janky
(17.66%), a 7 ms median/40 ms p90 UI time, and 2 ms median/3 ms p90 GPU time.
These counters include uncontrolled prior use and startup; they are evidence of
missed frames, **not** a controlled rail benchmark or a before/after comparison.

The follow-up removes coroutine-delayed section jumps, de-duplicates rail
callbacks per letter, materializes only the held section, and supplies cached
icons synchronously (cache misses still decode off-main). The window requests
the highest refresh rate supported at its current resolution; the Pixel window
reports a 120 Hz preference. Android still applies user/power/thermal policy.
A refresh-rate request is not an achieved frame-rate measurement.

The phone was screen-off/locked during deployment. For a manual check after
unlocking, keep power/thermal conditions consistent and collect a fresh sample:

```sh
adb -s DEVICE_SERIAL shell dumpsys gfxinfo app.cascadia.launcher reset
# Scrub the rail and scroll for a repeatable interval, then:
adb -s DEVICE_SERIAL shell dumpsys gfxinfo app.cascadia.launcher framestats
```

Separate cold icon-cache passes from warm repeats. Use the Macrobenchmark and
Perfetto workflow above before claiming sustained 60/120 Hz or quantified gains.
The existing baseline profile predates the held-section path and needs refreshing.

## Acceptance and follow-ups

- Confirm smooth 60/120 Hz rail and list navigation on the Pixel 8.
- Verify no icon decoding, disk I/O, or app discovery on the UI gesture path.
- Inspect frame-time percentiles, missed-frame bursts, startup timings, memory,
  and icon-cache behavior rather than only averages.
- Exercise hundreds of launchable entries, long labels, and non-Latin labels.
- Add lower-end hardware and Android 10 compatibility coverage.
- Re-run after generating/installing an app-specific baseline profile.
- Warm Home entry should show cached content in about 500 ms or less; cold-start
  targets must be based on real measurements, not debug emulator timings.
