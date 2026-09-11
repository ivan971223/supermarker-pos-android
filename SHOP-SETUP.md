# Shop setup checklist — 88 Store Android POS

One-page guide: **what to buy** + **how to install the app** on a dual-screen Android cashier machine.

---

## A. What to buy (supermarket cashier kit)

| # | Item | Spec to tell the seller | Notes |
|---|------|-------------------------|--------|
| 1 | **Dual-screen Android POS** | Android **10+**, landscape ~15.6" cashier + customer 客顯 | Ask: *“Android sees a real second Display, not HDMI mirror?”* |
| 2 | **Network** | Wi‑Fi **or** Ethernet on same LAN as printer | Prefer Ethernet if Wi‑Fi is weak |
| 3 | **Receipt printer** | 80mm **ESC/POS**, Wi‑Fi/Ethernet, TCP **port 9100** | Avoid cloud-only / phone-app-only printers |
| 4 | **Cash drawer** | RJ12 plug into **printer** kick port | App opens drawer on cash pay |
| 5 | **Barcode scanner** | USB or BT **keyboard wedge (HID)** + sends **Enter** after scan | No special SDK needed |
| 6 | **Thermal paper** | 80mm rolls | Match printer |
| 7 | **Power** | UPS / strip for tablet + printer + drawer | Avoid random reboots mid-sale |

**Optional later:** Octopus/card terminal (separate device; confirm in app after customer pays), spare scanner, mount/stand.

**You do not need:** brand-specific POS SDK, separate drawer controller board, Google Play listing (sideload APK is fine for shop use).

---

## B. Build the APK (on your Mac)

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
cd /Users/louischan/WebstormProjects/supermarker-pos-android
```

### Debug (UAT / emulator)

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
./scripts/reinstall-debug.sh
```

### Release (shop counter) + simple update

1. **One-time** create a signing key (keep `.jks` + passwords forever — needed for every update):

```bash
./scripts/create-keystore.sh
# writes shop-release.jks + keystore.properties (gitignored)
```

2. **Each release** — bump `versionCode` / `versionName` in `app/build.gradle.kts`, then:

```bash
./scripts/build-release.sh
# → app/build/outputs/apk/release/app-release.apk
# If a tablet is connected: adb install -r (keeps local data)
```

3. **Update without Mac tools on site:** copy the new `app-release.apk` to USB / Drive → open on tablet → Install (same package, same signature → Android upgrades in place).

| Rule | Why |
|------|-----|
| Same `applicationId` (`com.store88.pos`) | Android treats it as the same app |
| Same signing key | Required for update; wrong key → uninstall first (data loss) |
| Higher `versionCode` | Android accepts the new APK as an upgrade |
| `adb install -r` or “Update” in installer | Replaces APK; app storage usually kept |

> Never lose `shop-release.jks`. If you lose it, you must uninstall the old app before installing a new-signed APK.

---

## C. Install on the dual-screen Android machine

Pick **one** method.

### Method 1 — USB + `adb` (best for you / IT)

1. On the POS machine: **Settings → About → tap Build number 7×** → enable **Developer options**.
2. Enable **USB debugging**.
3. Plug USB into your Mac. Allow “USB debugging?” on the tablet.
4. On Mac:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb devices
adb install -r /Users/louischan/WebstormProjects/supermarker-pos-android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.store88.pos/.MainActivity
```

5. App icon: **88 Store POS** (`com.store88.pos`).

### Method 2 — Copy APK file (no Mac tools on site)

1. Copy `app-debug.apk` to a USB stick, Google Drive, WeChat/WhatsApp, or AirDrop → tablet.
2. On the Android machine: open the `.apk` with **Files**.
3. If blocked: **Settings → Security / Apps → Install unknown apps** → allow Files (or Chrome / WeChat).
4. Tap **Install** → **Open**.

### Method 3 — Same Wi‑Fi + `adb` wireless (optional)

```bash
# After one USB pairing on Android 11+:
adb tcpip 5555
adb connect <TABLET_LAN_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## D. First-day counter setup (after install)

1. Connect tablet to shop **Wi‑Fi / Ethernet** (same network as printer).
2. Power on printer; note its **LAN IP** (printer menu / router DHCP list).
3. Plug **cash drawer RJ12** into the printer.
4. Plug **barcode scanner** into USB (or pair Bluetooth as keyboard).
5. Open **88 Store POS** → login:

| Cashier | PIN |
|---------|-----|
| Chan Tai Man / 陳大文 | `1234` |
| Ken Wong / 黃健 | `5678` |

6. Menu (☰) → **Printer / 打印機**:
   - Host = printer IP  
   - Port = `9100`  
   - Paper = `80mm`  
   - **Dry-run = OFF**  
   - **Save** → **Test connection** → **Test print** → **Test drawer**
7. Checkout: focus search bar → scan a seed barcode (e.g. Coke `4890008000510`) → item adds.
8. Confirm **客顯** shows cart/total on the customer screen (if blank: OEM “second screen / presentation”, disable mirror).
9. Do one cash sale: drawer should open; optional print-on-pay.

---

## E. Update the app later

1. Bump `versionCode` (and optionally `versionName`) in `app/build.gradle.kts`.
2. `./scripts/build-release.sh` (or copy the new release APK to the tablet).
3. Install over the old app (`adb install -r` or system “Update”). Local demo data usually stays unless you clear storage.

To wipe demo data: Android **Settings → Apps → 88 Store POS → Storage → Clear data**, or in-app **Reset** on the login screen.

**Manager PIN** for Void / Refund in Transaction Records: `9999` (change `PosConstants.MANAGER_PIN` before production).

---

## F. Quick troubleshooting

| Problem | Check |
|---------|--------|
| Can’t install APK | Allow “Install unknown apps” for Files |
| `adb devices` empty | USB debugging on; try another cable/port |
| Print fails | Same Wi‑Fi/LAN; Dry-run off; ping printer IP; port 9100 |
| Drawer silent | RJ12 into **printer**, not tablet; cash pay or ⋮ → Drawer |
| Scan does nothing | Scanner as keyboard; focus search; ends with Enter |
| 客顯 blank | Not mirror mode; real second Display; reopen app on cashier screen |

More hardware detail: [HARDWARE.md](HARDWARE.md).
