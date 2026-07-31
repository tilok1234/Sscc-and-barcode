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
  from separate symbols on one label become one scan), center-reticle aim
  gating, and 1–3× zoom for budget cameras
- Fields: SSCC, batch, GTIN/EAN, best-before, quantity, article number —
  with the label photo stored on every scan
- **Damage register**: flag any scan → evidence photos + timestamped notes
  → resolve as restored (with corrected quantity) or sanitized; own tab,
  status icons, shareable report
- **Batch documents**: named batch runs with zero-tap instant saves, plus a
  per-batch summary (pallets per batch, mixed best-before dates, expired
  dates) — also toggleable on normal documents
- Free editing of every field, backed by an **append-only edit ledger**:
  originals stay visible (`18 (was 24)`), changes can't be silently erased
- Notes + photos on every scan and damage report; retention settings
  auto-compress/auto-delete old entries (damage-flagged scans never
  auto-delete)
- Scans filed into named documents (per truck/shipment/day), CSV export per
  document with `Source` provenance column (barcode vs OCR vs manual)
- Manual correction with live SSCC validation (wrong-length vs bad check
  digit, distinct messages) and a confidence indicator with low-confidence
  warning banner
- **Tools tab** (experimental toolbox): an appointment **Schedule** with
  per-appointment note logs, and an **Article registry** (article number,
  name, GTIN, label photos, notes) with instant search — more small tools
  land here as they prove handy

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
         BarcodeInterpreter, FieldMerger, BatchAnalyzer, CsvBuilder —
         fully unit-tested, buildable without the Android SDK.
app/     Android app: CameraX + ML Kit scanning pipeline, Room persistence
         (v7, additive migrations), Jetpack Compose UI.
docs/    VISION.md (product vision & principles), HANDOFF.md (developer
         handoff: state, build pipeline, invariants), and the original
         design handoff (spec + web prototypes). PLAN.md is the original
         build plan (historical).
```

## Documentation

| Doc | What it is |
|---|---|
| [`docs/VISION.md`](docs/VISION.md) | North star: problem context, settled design principles, roadmap |
| [`docs/HANDOFF.md`](docs/HANDOFF.md) | Developer handoff: current state, build/CI pipeline, invariants, next steps |
| [`PLAN.md`](PLAN.md) | Original assessment & build plan (historical) |
