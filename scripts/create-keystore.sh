#!/usr/bin/env bash
# Create a one-time shop release keystore (keep passwords + .jks offline).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JKS="$ROOT/shop-release.jks"
PROPS="$ROOT/keystore.properties"

if [ -f "$JKS" ]; then
  echo "Already exists: $JKS"
  echo "Delete it only if you understand updates will fail against old installs."
  exit 1
fi

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
KEYTOOL="$JAVA_HOME/bin/keytool"
STORE_PASS="${STORE_PASS:-store88pass}"
KEY_PASS="${KEY_PASS:-store88pass}"
ALIAS="${KEY_ALIAS:-store88}"

"$KEYTOOL" -genkeypair \
  -v \
  -keystore "$JKS" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=88 Store POS, OU=Shop, O=88 Store, L=Hong Kong, ST=HK, C=HK"

cat > "$PROPS" <<EOF
storeFile=shop-release.jks
storePassword=$STORE_PASS
keyAlias=$ALIAS
keyPassword=$KEY_PASS
EOF

echo ""
echo "Created: $JKS"
echo "Wrote:   $PROPS  (gitignored — do not commit)"
echo "Backup the .jks + passwords. Same key is required for APK updates."
