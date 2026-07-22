# SSCC & Barcode Scanner — Assessment and Implementation Plan

## 1. What we have today (assessment)

The uploaded bundle (`docs/design_handoff/`) is a **design handoff, not an app yet**. It contains:

- `README.md` — a high-fidelity spec: every screen, design token, interaction, the data model, and the field-extraction logic written out precisely enough to port line-for-line.
- `SSCC Scanner (Claude version).dc.html` — working web prototype that sends the label photo to **Claude's vision API** for reading. Accurate, but requires being logged into the design tool. **This is the dependency we are removing.**
- `SSCC Scanner (offline on-device version).dc.html` — working web prototype that runs **Tesseract.js OCR fully on-device**, then extracts SSCC / batch / GTIN / best-before / quantity with deterministic regex + GS1 logic. No account, no network. Its `extractFields` and `ssccCheckStatus` functions are real, tested logic we can port to Kotlin 1:1.

What the prototypes do well (keep all of it):
- GS1 SSCC **mod-10 check-digit validation**, used both to pick the best OCR candidate among noisy 18-digit runs and to live-validate manual edits.
- Field extraction that understands both **human-readable label text** ("Batch no: …", "Best before …") and **GS1 Application Identifiers** printed under barcodes: `(00)` SSCC, `(10)` batch, `(01)/(02)` GTIN, `(15)/(17)` dates.
- A sensible product shape: scans filed into user-named documents, batch mode for back-to-back scanning, manual correction with validation, CSV export, thumbnails-only storage.

What's missing / what changes:
1. **No native Android app exists** — the repo is empty apart from these handoff files. Everything must be built.
2. **No true barcode scanning.** Both prototypes only OCR the printed text. But SSCC labels are GS1-128 labels: the barcodes themselves *encode* the SSCC, batch, GTIN, and dates. Decoding the barcode directly is dramatically more reliable than OCR-ing the human-readable text — this is the single biggest upgrade we'll make.
3. **Claude/AI dependency** — only exists in one prototype variant; the app we build will have zero server calls, no API keys, no login. Everything runs on-device.

## 2. Target architecture

**Native Android app (Kotlin + Jetpack Compose), fully offline, no accounts.**

### Scanning pipeline — barcode-first, OCR-fallback

```
Live camera (CameraX preview + ImageAnalysis)
        │
        ├─► ML Kit Barcode Scanning (on-device, free, offline)
        │      formats: Code 128 / GS1-128, DataMatrix, QR, EAN-13/8, ITF-14, Code 39
        │      │
        │      └─► GS1 element-string parser (our Kotlin code)
        │             AIs: (00) SSCC, (01)/(02) GTIN, (10) batch,
        │                  (15)/(17) best-before/expiry, (37) count
        │             + SSCC mod-10 check-digit validation
        │
        └─► If no barcode found, or fields still missing, or user chose
            "Upload photo" / photo capture:
               ML Kit Text Recognition v2 (on-device OCR)
                  │
                  └─► extractFields ported from the offline prototype
                      (regex + GS1-AI text matching + check-digit scoring)
```

- Barcode and OCR results **merge**: barcode-decoded fields win (they're authoritative), OCR fills any gaps (e.g. a quantity that's only printed as text).
- Confidence model extends the prototype's: `high` = field came from a validated barcode; `medium`/`low` follow the prototype's OCR rules.
- Both ML Kit models are on-device, free, no API key, work in airplane mode. Coworkers install the APK and it just works — the handoff's central requirement.

**One deliberate deviation from the handoff:** the web prototype avoided a live camera preview (a web-sandbox limitation, explicitly called out as such in the README). Native barcode scanning *needs* live frame analysis for the zero-tap UX, so the Scan tab gets a real CameraX viewfinder with a scanning reticle. Photo-capture and gallery-upload remain as the OCR path for damaged/unscannable barcodes.

### Tech stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose (Material 3, custom dark theme from the handoff's design tokens) |
| Camera | CameraX (Preview + ImageAnalysis + ImageCapture) |
| Barcode decode | `com.google.mlkit:barcode-scanning` (bundled model) |
| OCR fallback | `com.google.mlkit:text-recognition` (bundled model) |
| Persistence | Room (documents + scans tables), DataStore (active document, batch-mode flag) |
| Images | Thumbnail-only persistence (~15 KB JPEG), as in the prototype |
| Export/share | CSV via `FileProvider` + share sheet; Storage Access Framework "save as" |
| Fonts | IBM Plex Sans + IBM Plex Mono bundled locally |
| Min SDK | 24 (covers virtually all warehouse devices); target latest stable |

### Data model (from the handoff, plus barcode provenance)

```
Document { id, name, createdAt }
Scan {
  id, documentId,
  sscc, batchNo, gtin, bestBefore, quantity,   // nullable strings
  confidence: high|medium|low,
  source: barcode|ocr|mixed|manual,            // NEW — how each scan was read
  rawBarcodes: [String],                       // NEW — raw symbology payloads, for debugging/audit
  edited: Boolean,
  timestamp, thumbnail (small JPEG only)
}
```

## 3. Build plan (phases)

### Phase 1 — Project scaffold
Gradle/Kotlin project, Compose setup, dark theme + design tokens (colors, type scale, radii from the handoff), bundled Plex fonts, bottom navigation shell (Scan / Library tabs).

### Phase 2 — Core domain logic + unit tests (no UI needed)
- `Gs1Parser`: parse GS1 element strings from barcode payloads (FNC1 / `]C1` AIM prefix / GS `` separators, fixed- vs variable-length AIs 00, 01, 02, 10, 15, 17, 37).
- `SsccValidator`: mod-10 check digit + length checks (two distinct error states, per spec).
- `LabelTextParser`: line-for-line Kotlin port of the prototype's `extractFields` (SSCC candidate scoring, batch/GTIN/date/quantity regexes, confidence rules).
- `FieldMerger`: barcode-first merge of barcode + OCR results.
- **JUnit tests for all of the above** — this logic is the heart of the app and is fully testable without a device.

### Phase 3 — Scanning
- CameraX live preview with ML Kit barcode analyzer; auto-detect → parse → result (zero taps in batch mode besides pointing the phone).
- Photo capture + gallery picker → downscale (~1400 px) → ML Kit OCR → `LabelTextParser`.
- Processing / result / error states styled per the handoff (status dot, low-confidence banner, copy buttons, batch counter).

### Phase 4 — Data layer
Room entities/DAOs for documents and scans, thumbnail generation, DataStore prefs, repository layer.

### Phase 5 — Full UI per the handoff
Document picker sheet, Library list/detail/scan-detail, inline rename, two-tap document delete, edit mode with live SSCC validation, toasts, share sheet, CSV export (`SSCC, Batch No, GTIN/EAN, Best Before, Quantity, Confidence, Edited, Scanned At` — plus a `Source` column).

### Phase 6 — Ship
App icon, permission rationale flow (camera), release build config, signed APK instructions (and optionally a GitHub Actions workflow to build a debug APK per push).

## 4. Suggested repo layout

```
app/                         # Android app module
  src/main/java/.../domain/  # Gs1Parser, SsccValidator, LabelTextParser, FieldMerger
  src/main/java/.../data/    # Room, DataStore, repositories, CSV export
  src/main/java/.../scan/    # CameraX + ML Kit pipeline
  src/main/java/.../ui/      # Compose screens & theme
  src/test/                  # domain unit tests
docs/design_handoff/         # original spec + prototypes (reference)
PLAN.md                      # this file
```
