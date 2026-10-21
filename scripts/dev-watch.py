#!/usr/bin/env python3
"""Rebuild and restart the launcher on one phone after source changes."""

import argparse
import fcntl
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import time


ROOT = Path(__file__).resolve().parent.parent


def snapshot():
    files = []
    for directory in (ROOT / "app/src", ROOT / "gradle"):
        files.extend(p for p in directory.rglob("*") if p.is_file())
    files.extend(ROOT.glob("*.gradle.kts"))
    files.extend((ROOT / "app").glob("*.gradle.kts"))
    files.extend((ROOT / "app").glob("*.pro"))
    files.extend([ROOT / "gradle.properties", ROOT / "scripts/dev.sh"])
    result = {}
    for path in files:
        try:
            stat = path.stat()
            result[str(path)] = (stat.st_mtime_ns, stat.st_size)
        except FileNotFoundError:
            pass
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("serial", nargs="?", default=os.environ.get("ANDROID_SERIAL"))
    args = parser.parse_args()
    serial = args.serial
    if not serial:
        output = subprocess.check_output(["adb", "devices"], text=True)
        devices = [line.split()[0] for line in output.splitlines()[1:]
                   if len(line.split()) >= 2 and line.split()[1] == "device"]
        if len(devices) != 1:
            parser.error("Connect exactly one authorized phone, or provide its serial.")
        serial = devices[0]

    # Keep one watcher per checkout; retain this file handle for the process lifetime.
    key = hashlib.sha256(str(ROOT).encode()).hexdigest()[:16]
    with open(Path(tempfile.gettempdir()) / f"cascadia-watch-{key}.lock", "w") as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            parser.error("A watcher is already running for this checkout.")
        print(f"Watching {ROOT} → {serial}. Ctrl-C to stop.", flush=True)
        previous = None
        try:
            while True:
                current = snapshot()
                if current != previous:
                    # Wait for edits to settle; changes during a build trigger another pass.
                    time.sleep(0.75)
                    if snapshot() != current:
                        continue
                    previous = current
                    result = subprocess.run(["bash", str(ROOT / "scripts/dev.sh"), serial], cwd=ROOT)
                    if result.returncode:
                        print("Update failed. Save a source file to retry after fixing the build or reconnecting the phone.", flush=True)
                time.sleep(0.5)
        except KeyboardInterrupt:
            print("\nWatcher stopped.", flush=True)


if __name__ == "__main__":
    main()
