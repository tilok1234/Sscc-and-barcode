# SSCC Scanner

A standalone Android app for warehouse/logistics workers: point the camera at a
GS1 shipping label and get the **SSCC**, **batch number**, **GTIN/EAN**,
**best-before date**, and **quantity** as clean, copyable text — no retyping,
no accounts, no cloud.

**Everything runs on-device.** Barcode decoding and OCR use Google ML Kit's
bundled models; nothing ever leaves the phone. Works in airplane mode.

## How it reads labels

1. **Barcode-first** (primary): the live viewfinder decodes the label's
   barcodes — GS1-128, GS1 DataMatrix/QR, EAN, ITF-14 — and parses the GS1
   application identifiers directly: `(00)` SSCC, `(01)/(02)` GTIN, `(10)`
   batch, `(15)/(17)` dates, `(37)` quantity. Check digits are validated
   (mod-10). This is far more reliable than reading printed text.
2. **OCR fallback**: for damaged or missing barcodes, snap a photo (or upload
   one) — ML Kit text recognition reads the printed text and deterministic
   pattern-matching extracts the fields, preferring SSCC candidates whose
   check digit validates. Barcode results always win; OCR fills gaps.

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

Every push builds a debug APK on GitHub Actions: open the latest **Android CI**
run → **Artifacts** → `sscc-scanner-debug-apk`. Install it on any Android 7.0+
(API 24) device.

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
