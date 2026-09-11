#!/usr/bin/env bash
# Build signed release APK and optionally install with adb -r (update in place).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

if [ ! -f "$ROOT/keystore.properties" ]; then
  echo "Missing keystore.properties"
  echo "Run once: ./scripts/create-keystore.sh"
  echo "Or: cp keystore.properties.example keystore.properties  # then edit"
  exit 1
fi

echo "==> assembleRelease"
(cd "$ROOT" && ./gradlew assembleRelease)

if [ ! -f "$APK" ]; then
  echo "APK not found: $APK"
  exit 1
fi
echo "APK: $APK"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found — copy APK to tablet manually."
  exit 0
fi
DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "$DEVICES" ]; then
  echo "No device connected. Copy APK to tablet and install."
  exit 0
fi
SERIAL="${ADB_SERIAL:-$(echo "$DEVICES" | head -1)}"
echo "==> update install $SERIAL (adb install -r)"
adb -s "$SERIAL" install -r "$APK"
echo "==> done"
