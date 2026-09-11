# 88 Store POS (Android)

Native Kotlin + Jetpack Compose POS for a **15.6" landscape Android tablet**, ported from [`supermarker-pos-web`](../supermarker-pos-web) (same teal UI flows, catalog, promos, admin, offline queue stub).

## Features

- Login (PIN), checkout grid/cart/pay bar, payment + cash change
- Day close, products/categories/promos/inventory/expiry/receive/reports/transactions/receipt design
- **LAN ESC/POS** receipt print (TCP `:9100`) and **cash drawer kick** via printer RJ12
- Dry-run mode (default in debug) for emulator without hardware
- Bilingual EN / 繁 / 雙語
- **Dual-screen 客顯**: cart lines + totals on the secondary customer display (auto); in-app preview via ⋮ → Customer Display

## Demo login

| Cashier | PIN |
|---------|-----|
| Chan Tai Man / 陳大文 | `1234` |
| Ken Wong / 黃健 | `5678` |

## Dual-screen customer display (客顯)

On a **雙屏 Android POS** tablet, the app detects the secondary `Display` and shows a customer-facing UI:

| Customer sees | When |
|---------------|------|
| Welcome | Cart empty |
| Item list + subtotal / discount / **total** | Items in cart |
| Payment method + total (+ cash change) | On payment screen |
| Thank you + amount | ~5s after successful pay |

Cashier screen is unchanged. No extra permission required while the app is in the foreground.

**Single-screen / emulator test:** checkout → ⋮ → **Customer Display / 客顯預覽**.

Hardware tip: buy units that expose a real second Android `Display` (not HDMI mirror-only). See [HARDWARE.md](HARDWARE.md).

## Build & install

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd /Users/louischan/WebstormProjects/supermarker-pos-android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or: `./scripts/reinstall-debug.sh`

**Shop buy + install guide (雙屏機 / printer / drawer / scanner):** [SHOP-SETUP.md](SHOP-SETUP.md)

## Printer & drawer

1. Connect **80mm ESC/POS** printer to shop LAN; note its IP.
2. Plug cash drawer into printer **RJ12**.
3. In app: menu → **Printer** → set IP / port `9100` → disable Dry-run → **Save**.
4. Use **Test print** / **Test drawer**.
5. Cash payments open the drawer automatically; enable **Print receipt on pay** on the payment screen as needed.

See [HARDWARE.md](HARDWARE.md).

## Package

- Application id: `com.store88.pos`
- Min SDK 26, target SDK 34, landscape locked
