#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="dev.gitfudge.debbie.debug"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb was not found on PATH." >&2
  exit 1
fi

device_count="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"

if [ "$device_count" -eq 0 ]; then
  echo "No adb device is connected and ready." >&2
  adb devices >&2
  exit 1
fi

if [ "$device_count" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then
  echo "Multiple adb devices are connected. Set ANDROID_SERIAL to choose one." >&2
  adb devices >&2
  exit 1
fi

./gradlew :app:installDebug

adb shell monkey \
  -p "$PACKAGE_NAME" \
  -c android.intent.category.LAUNCHER \
  1 >/dev/null

echo "Installed and opened $PACKAGE_NAME."
