# DCR — Damage Control Register

A standalone Android app for warehouse/factory floor workers: scan a GS1
pallet label in a second (SSCC, batch, GTIN/EAN, article number, best-before,
quantity), and when goods arrive damaged, file a **damage report** — evidence
photos, timestamped notes, restored/sanitized resolution — into the app's
register. Replaces retyping 18-digit codes *and* the paper damage sheet.
No accounts, no cloud.

*(Formerly "SSCC Scanner" — renamed when the damage register became the
product's center; see docs/VISION.md.)*

**Everything runs on-device.** Barcode decoding and OCR use Google ML Kit's
bundled models; nothing ever leaves the phone. Works in airplane mode.

## How it reads labels

Two scan modes, toggleable right on the viewfinder (the choice is remembered):

1. **Barcode mode** (instant): the live viewfinder decodes the label's
   barcodes the moment they're visible — GS1-128, GS1 DataMatrix/QR, EAN,
   ITF-14 — and parses the GS1 application identifiers directly: `(00)` SSCC,
   `(01)/(02)` GTIN, `(10)` batch, `(15)/(17)` dates, `(37)` quantity. Check
   digits are validated (mod-10). Zero taps per label.
2. **Label mode** (framed): live decoding is off so a stray barcode can't
   trigger the scanner while you aim. Frame the whole label and press the
   shutter — the photo goes through barcode detection AND ML Kit text
   recognition together, with deterministic pattern-matching over the printed
   text (SSCC candidates are ranked by check-digit validity). Barcode results
   always win; OCR fills the gaps.

Uploading an existing photo uses the same combined barcode + OCR pipeline.

## Features

- Live camera scanning with multi-barcode aggregation (SSCC + GTIN + batch
  from separate symbols on one label become one scan)
- Batch mode for back-to-back scanning with zero taps between labels
- Scans filed into named documents (per truck/shipment/day), CSV export per
  document with `Source` provenance column (barcode vs OCR vs manual)
- Manual correction with live SSCC validation (wrong-length vs bad check
  digit, distinct messages)
- Confidence indicator (green/yellow/red) with a low-confidence warning banner
- Only small thumbnails are stored — hundreds of scans stay lightweight

## Getting the APK

**Easiest (phone-friendly):** every successful build publishes the APK to a
fixed link — bookmark it and tap to download the newest build any time:

> https://github.com/tilok1234/Sscc-and-barcode/releases/download/latest/sscc-scanner.apk

Updates install straight over the existing app (fixed debug signing key,
auto-incrementing version). Works on any Android 7.0+ (API 24) device.

Alternatively: open the latest **Android CI** run on the Actions tab →
**Artifacts** → `sscc-scanner-debug-apk`.

## Building locally

Requires JDK 17+ and the Android SDK (compileSdk 35):

```bash
./gradlew :app:assembleDebug        # APK at app/build/outputs/apk/debug/
./gradlew -p core test              # domain logic unit tests (no Android SDK needed)
```

## Project layout

```
core/    Pure-JVM domain logic: Gs1Parser, SsccValidator, LabelTextParser,
         BarcodeInterpreter, FieldMerger, CsvBuilder — fully unit-tested,
         buildable without the Android SDK.
app/     Android app: CameraX + ML Kit scanning pipeline, Room persistence,
         Jetpack Compose UI following docs/design_handoff/.
docs/    Original design handoff (spec + web prototypes) and PLAN.md.
```
