# Hardware — 88 Store Android POS

## Counter kit

| Piece | Spec |
|-------|------|
| Tablet / AIO | 15.6" Android 10+, landscape |
| Scanner | USB or Bluetooth **keyboard wedge** + Enter |
| Printer | 80mm **ESC/POS**, Wi‑Fi/Ethernet (TCP **9100**) |
| Cash drawer | RJ12 into printer kick port |

## App settings

Admin → **Printer**:

- Host = printer LAN IP (e.g. `192.168.1.50`)
- Port = `9100`
- Paper = 80mm (or 58mm)
- Dry-run = off on real counter

## Behaviour

- **Cash pay** → drawer kick (`ESC p`), optional receipt if print-on-pay is on
- **Non-cash** → no drawer; receipt if print-on-pay
- **Reprint** from Transactions
- **Test drawer / Test print** from Printer settings

## Scanner

Focus the checkout search field; scan barcodes from the seed catalog (e.g. Coke `4890008000510`).

## Dual-screen POS (雙屏 / 客顯)

| Piece | Spec |
|-------|------|
| Dual-display POS | Android 10+ with a **second physical Display** (customer face), not HDMI mirror-only |
| Orientation | Landscape cashier + landscape or portrait customer panel |

The app uses Android `Presentation` + `DisplayManager`:

1. Launch POS on the cashier screen.
2. If a secondary display is present, **Customer Display** opens automatically.
3. Cart / totals / pay / thank-you sync live from the till.
4. On single-screen devices: ⋮ → **Customer Display / 客顯預覽**.

If the customer screen stays blank, check OEM settings for “second screen / presentation / customer display” and disable mirror mode.

## Shop purchase + install

See **[SHOP-SETUP.md](SHOP-SETUP.md)** — buy list, build APK, USB/`adb` or file install, first-day counter setup.
