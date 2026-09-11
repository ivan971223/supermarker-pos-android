#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

echo "==> assembleDebug"
(cd "$ROOT" && ./gradlew assembleDebug)

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. APK: $APK"
  exit 0
fi
DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICES" ]; then
  echo "No device. APK: $APK"
  exit 0
fi
SERIAL="${ADB_SERIAL:-$(echo "$DEVICES" | head -1)}"
echo "==> install $SERIAL"
adb -s "$SERIAL" install -r "$APK"
echo "==> done"
