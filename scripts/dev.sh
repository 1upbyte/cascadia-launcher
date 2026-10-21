#!/usr/bin/env bash
# Incremental build -> reinstall (preserve data) -> open on one explicit device.
set -euo pipefail

if [[ "${1:-}" == "--help" || $# -gt 1 ]]; then
    printf 'Usage: %s [DEVICE_SERIAL]\n' "$0"
    printf 'Uses ANDROID_SERIAL, or the only authorized ADB device, if omitted.\n'
    printf 'Optimized build by default. Set CASCADIA_BUILD_TYPE=debug for debugger/tooling.\n'
    exit 0
fi

cd "$(dirname "$0")/.."
command -v adb >/dev/null || { printf 'adb is not on PATH. Install Android platform-tools.\n' >&2; exit 1; }
serial="${1:-${ANDROID_SERIAL:-}}"
if [[ -z "$serial" ]]; then
    devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
    count="$(printf '%s\n' "$devices" | awk 'NF { count++ } END { print count + 0 }')"
    if [[ "$count" != 1 ]]; then
        printf 'Expected one authorized device; found %s. Connect/authorize your phone or pass its serial.\n' "$count" >&2
        adb devices -l
        exit 1
    fi
    serial="$devices"
fi

# Validate before building, and never silently target another connected device.
[[ "$(adb -s "$serial" get-state)" == device ]] || { printf 'Device is not ready.\n' >&2; exit 1; }
variant="${CASCADIA_BUILD_TYPE:-benchmark}"
case "$variant" in
    benchmark) task=assembleBenchmark ;;
    debug) task=assembleDebug ;;
    *) printf 'CASCADIA_BUILD_TYPE must be benchmark or debug.\n' >&2; exit 1 ;;
esac
printf 'Building %s APK for %s…\n' "$variant" "$serial"
./gradlew ":app:$task"
adb -s "$serial" install -r "app/build/outputs/apk/$variant/app-$variant.apk"
adb -s "$serial" shell am start -W \
    -a android.intent.action.MAIN -c android.intent.category.HOME \
    -n app.cascadia.launcher/.LauncherActivity
printf '\nUpdated. Favorites/settings are preserved; default Home selection is unchanged.\n'
printf 'Logs: adb -s %s logcat --pid="$(adb -s %s shell pidof -s app.cascadia.launcher)"\n' "$serial" "$serial"
