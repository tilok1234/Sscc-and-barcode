# Handoff: SSCC Label Scanner

## Overview
A mobile tool for warehouse/logistics workers to photograph a GS1 shipping label and get the **SSCC**, **batch number**, **GTIN/EAN**, **best-before date**, and **quantity** extracted as clean text — instead of manually retyping them. Scans are organized into user-named "documents" (e.g. one per truck/shipment/day), each exportable as CSV.

**Goal for the real app: no login, no account, works standalone.** This is the central requirement — see "OCR: the one decision that matters" below before writing any code.

## About the Design Files
The bundled `.dc.html` files are **working HTML prototypes** — built and tested in a browser-based design tool, not production code to copy directly. Please **recreate this design and logic natively** (Kotlin/Java + Jetpack Compose or XML views is recommended for Android) using the target project's normal architecture. The prototypes are the spec: exact visuals, exact copy, and — importantly — exact working business logic (see "Core logic to port" below), which you should treat as executable pseudocode, not just a visual reference.

## Fidelity
**High-fidelity.** Colors, type, spacing, copy, and component states in the files are final, not placeholders. The interaction logic (OCR pipeline, GS1 field parsing, check-digit validation, document/scan data model) is real, working code — port its logic 1:1, don't redesign it.

---

## OCR: the one decision that matters

There are two working prototypes in this bundle, using two different OCR approaches:

1. **`SSCC Scanner (Claude version).dc.html`** — sends the photo to Claude's vision API for reading. Very accurate, but **requires being logged into the design tool this was built in** — it will not work as a standalone app.
2. **`SSCC Scanner (offline on-device version).dc.html`** — uses **Tesseract.js**, an open-source OCR engine that runs **entirely in the browser/device**, plus deterministic pattern-matching to pull the SSCC/batch/GTIN/date out of the raw recognized text. No account, no login, no server call, works with no connectivity.

**For the real Android app, use approach #2's architecture — but replace Tesseract.js with Google ML Kit's on-device Text Recognition API** (`com.google.mlkit:text-recognition`). It's free, runs fully offline, needs no API key or login, and is generally more accurate than Tesseract for this use case. Feed ML Kit's recognized text into the same field-extraction logic described below (ported to Kotlin) instead of Tesseract's output. This is what makes coworkers able to just install the APK and use it — no accounts, ever.

---

## Core logic to port

### 1. Image → text (OCR)
Any on-device OCR engine works; ML Kit Text Recognition v2 recommended. Feed it the full-resolution photo (not a thumbnail — recognition accuracy depends on resolution).

### 2. Text → fields (deterministic parsing, not AI)
This is the important part — copied here verbatim from the working prototype (`extractFields` in the offline `.dc.html`) so it can be ported line-for-line:

- **SSCC** (18 digits): scan the recognized text for runs of 18 digits (tolerating embedded OCR spaces, and an optional leading GS1 Application Identifier `(00)`). If multiple candidates are found, **prefer the one whose GS1 mod-10 check digit validates** (algorithm below) — this is what makes the parser robust to noisy OCR.
- **Batch number**: a line matching `batch(\s*(no|number))?\s*[.:]?\s*(value)` (case-insensitive), else fall back to GS1 Application Identifier `(10)`.
- **GTIN / EAN**: a line matching `(ean|gtin|content)(\s*(no|nr|number))?\s*[.:]?\s*(digits)`, else GS1 AI `(01)`/`(02)`, else any other free-standing 13–14 digit run that isn't part of the already-found SSCC.
- **Best-before date**: on a line containing "best before"/"BBD"/"expiry"/"use by", pull the first date-shaped token (`DD-MM-YY(YY)`, `YYYY-MM-DD`, or 8 digits); else fall back to GS1 AI `(15)`/`(17)` (6-digit YYMMDD).
- **Quantity**: a line matching `(quantity|qty|count)\s*[.:]?\s*(digits)`.
- **Confidence**: `high` if the SSCC check digit validated AND OCR engine confidence ≥ 80%; `medium` if either is true; otherwise `low`. Low confidence shows a warning banner to the user on the result screen.

### 3. GS1 SSCC check-digit algorithm (mod-10)
Used both to pick the best OCR candidate above, and to live-validate manual corrections:

```
input: sscc (must be exactly 18 digits after stripping whitespace)
sum = 0
for i in 0..16 (the first 17 digits, 0-indexed):
    weight = 3 if i is even, else 1
    sum += weight * digit_at(i)
checkDigit = (10 - (sum % 10)) % 10
valid = (checkDigit == digit_at(17))
```
If the SSCC is not exactly 18 digits, that's a distinct "wrong length" error state (different message than "check digit mismatch") — both are surfaced in the manual-edit field.

### 4. Image handling
- Photo is downscaled/recompressed before OCR — try progressively smaller widths (1400→260px) and JPEG qualities (0.85→0.26) until under a byte budget, largest/best-quality that fits wins. (In the Claude version this budget is 200KB because of an API cap; for on-device OCR there's no such cap, so just pick a resolution that balances OCR accuracy against processing time — 1000–1400px wide is a good default.)
- A separate, much smaller thumbnail (~15KB budget in the prototype) is generated for storage — **only the thumbnail is persisted**, never the full-resolution photo, to keep local storage small across hundreds of scans.

---

## Screens / Views

### 1. Scan tab — Ready state (default)
- Full-bleed dark camera-tinted background (`#0e1113`) with a large, low-opacity (12%) camera glyph centered, purely decorative.
- Top-left: app wordmark "SSCC SCANNER", 13px IBM Plex Mono, 700 weight, letter-spacing 0.12em, uppercase, `rgba(245,245,240,.85)`.
- Top-center pill button, floating: "Filing to {document name} ⌄" — opens the document picker. Semi-transparent black pill (`rgba(0,0,0,.55)`), 1px border `rgba(237,237,233,.22)`, 11.5px 600 weight text, folder icon + chevron-down icon (both inline SVG, 14px/10px).
- Center content, vertically stacked, centered:
  - Heading "Scan a shipping label" — 15px, 600 weight, `#EDEDE9`.
  - Subheading "Take a new photo, or upload one you already have." — 12.5px, 400 weight, line-height 1.5, `rgba(237,237,233,.5)`, max-width 240px.
  - Two full-width stacked buttons (max-width 260px), 12px gap:
    - **"Start camera"** (primary): accent-colored background, 12px radius, 16px vertical padding, camera icon + label, 14.5px 700 weight text in `#1a1204` (near-black, for contrast on the amber accent). Press state: scale to 0.97.
    - **"Upload photo"** (secondary): `rgba(237,237,233,.08)` background, 1px border `rgba(237,237,233,.18)`, same padding/radius, gallery icon + label, `#EDEDE9` text. Same press state.
  - Below the buttons: **batch-mode toggle** — small pill switch (34×20px, thumb 16px) + label "Batch mode — scan labels back-to-back", 12px 600 weight, `rgba(237,237,233,.7)`. Off = track `rgba(237,237,233,.18)`; on = track is the accent color, thumb slides right.
- **"Start camera"** opens the device's native camera app via a file input with the `capture="environment"` attribute (do NOT use `getUserMedia`/live video preview — see "Why no live camera preview" below). **"Upload photo"** opens a plain file picker (no `capture` attribute) for the gallery/existing photos.
- If **batch mode** is on, a small pill badge appears top-right of the header: "BATCH · {n}" (accent background, `#1a1204` text, 10px 700 weight, uppercase) — increments after each successful scan while batch mode stays on.

### 2. Scan tab — Processing state
- Full-bleed: the just-captured photo, darkened (`brightness(.4) saturate(.7)`), as background.
- Centered overlay: 40px spinning ring (accent-colored top border, 0.9s linear rotation) + "Reading label…" (13px, 600 weight, mono, `#F5F5F0`).

### 3. Scan tab — Result state
- Header bar: status dot (green `#5FBF83` / yellow `#F2C94C` / red `#E5484D` by confidence) + "Saved to {document name}" (13px 600, truncates with ellipsis) + an "Edit" button (pencil icon, outlined pill, top-right).
- Scrollable body:
  - Small photo thumbnail strip (110px tall, cover-fit, rounded 10px).
  - **Low-confidence banner** (conditional): red-tinted box — "Low confidence — double-check against the printed label." (11px 600, `#F5A3A6` text on `rgba(229,72,77,.12)` bg, 1px `rgba(229,72,77,.3)` border).
  - **SSCC** field: uppercase label (11px 600, letterspacing .08em, `rgba(237,237,233,.45)`) + inline "Copy" text-button (accent color) on the same row; value below in large mono (21px, 600, letterspacing .02em, `#F5F5F0`, wraps if needed).
  - **Batch no.** field: same pattern, value at 18px mono.
  - Three-column grid (GTIN/EAN, Best before, Quantity): smaller labels (10px) and values (13px mono), no copy buttons.
- Footer (sticky): **"Share"** button (outlined, flex:1) + either **"Next scan →"** (if batch mode is on — accent-filled, larger flex:1.4, bold) or **"Scan another"** (if batch mode is off — accent-filled, flex:1).
- **"Next scan"** immediately clears the result and re-opens the native camera capture — the point of batch mode is zero taps between consecutive scans besides the shutter itself.

### 4. Scan tab — Result state, Edit mode
Triggered by the "Edit" button. Replaces the read-only field display with editable inputs, same screen/header:
- SSCC input: numeric keypad (`inputMode="numeric"`), large mono text (16px), border turns red-tinted (`rgba(229,72,77,.5)`) and a hint line appears below when invalid — "SSCC should be exactly 18 digits." (wrong length) or "Check digit doesn't match — one digit may be wrong." (bad check digit). Validation runs live on every keystroke.
- Batch no. input: plain text, mono 15px.
- Two-column row: GTIN/EAN input (numeric) + Best-before input (plain text).
- Quantity input (numeric), half-width.
- All inputs: `rgba(237,237,233,.06)` background, 1px `rgba(237,237,233,.16)` border (unless invalid, see above), 9px radius, `#F5F5F0` text.
- Footer swaps to: **"Cancel"** (outlined, discards edits) + **"Save changes"** (accent-filled, persists and shows a "Changes saved" toast). Saved edits are flagged internally (`edited: true`) so they can be distinguished in CSV export.

### 5. Scan tab — Error state
- Header: red status dot + "Couldn't read label".
- Centered: dimmed photo thumbnail (120px, 50% opacity) + explanatory message (13px, `rgba(237,237,233,.6)`, e.g. "Couldn't find an SSCC or batch number on this label. Try getting closer and keeping the text in focus.").
- Footer: full-width **"Try again"** button (accent-filled) → returns to Ready state.

### 6. Document picker (modal sheet)
- Dark scrim (`rgba(0,0,0,.6)`) covering the whole screen; tapping it closes the sheet.
- Card anchored near the **top** of the screen (88px from top, NOT bottom — bottom-anchored sheets get pushed below the fold on real phone browsers where the page can be taller than the viewport). 88% width, up to 64% height, `#101314` background, 16px radius all corners, 1px border, drop shadow.
- Header: "File to document" (14px 700) + ✕ close button.
- Scrollable list: each document is a full-width tappable row — radio circle (accent when selected) + name (13px 600, truncates) + scan count subtitle (11px, `rgba(237,237,233,.45)`).
- Footer (always visible, not scrolled): text input "New document name" + accent "Create" button (disabled/dimmed at 45% opacity until non-empty). Creating a document immediately makes it the active filing target and closes the sheet.

### 7. Library tab — Document list (default)
- Header: "Library" (17px 700) + document count subtitle (12px mono) on the left; "+ New" outlined button on the right (opens the same document picker sheet).
- Scrollable rows, one per document: 48×48px thumbnail (most recent scan's photo, rounded 8px) or a placeholder folder-icon tile if the document has no scans yet; name (13px 600, truncates) + subtitle "{n} scans · {relative time}"; a delete button on the right showing "✕" normally, turning into "Sure?" (red-tinted) on first tap — tap again within ~2.5s to confirm, otherwise it reverts. Deleting a document also deletes all scans filed in it.

### 8. Library tab — Document detail
- Slides over the list (same tab). Header: back arrow + document name/count (or, in rename mode, a text input replacing the name + "Save"/✕ buttons) + edit-pencil icon button (starts rename) + "Export CSV" button (disabled/dimmed when the document has no scans).
- Empty state: "No scans in this document yet" + explanatory subtext.
- Populated: scrollable rows identical in style to Library's document rows, but per-scan — thumbnail, SSCC (mono, truncates), "Batch {batch} · {relative time}" subtitle, ✕ delete (immediate, no confirm — lower-stakes than deleting a whole document).

### 9. Library tab — Scan detail
- Full-screen overlay. Header: back arrow + "{relative time} · {document name}".
- Body: photo (130px), SSCC (large mono + Copy), Batch no. (mono + Copy), then the GTIN/Best-before/Quantity three-column grid — same layout/type as the Scan-tab result screen.
- Footer: "Delete" (red-tinted outlined, flex:1) + "Share" (accent-filled, flex:1).

### 10. Toast (global, transient)
- Centered near the bottom (78px from bottom), dark pill (`rgba(20,23,26,.95)`), 1px border, drop shadow, 12px 600 white text. Fades in, holds, fades out over ~1.8s total. Used for: copy confirmations, "Filing to {doc}", "Document renamed/deleted", "Changes saved", and error fallbacks like "Nothing to copy".

### 11. Bottom navigation (persistent, both tabs)
- Two-item tab bar, `#101314` background, 1px top border. Each tab: 22px line icon + 10.5px 600 label, inactive `rgba(237,237,233,.4)`, active = accent color.
- **Scan** (camera+viewfinder icon) / **Library** (document icon, with a small accent count-badge top-right when scan count > 0, showing "99+" past 99).

---

## Interactions & Behavior summary
- **Start camera** / **Upload photo** → native file inputs (see OCR section) → `processImage()` → OCR → field extraction → save scan (with a small thumbnail only) → Result state.
- **Batch mode** persists across sessions (see storage keys) and resets its counter to 0 each time it's toggled on.
- **Copy** buttons use the OS clipboard API; **Share** uses the native share sheet if available, else falls back to clipboard copy of a plain-text summary.
- **Export CSV**: one file per document, columns `SSCC, Batch No, GTIN/EAN, Best Before, Quantity, Confidence, Edited, Scanned At` (ISO timestamp), values quote-escaped, filename derived from the document's name.
- **Document delete** requires a second confirming tap (arms for ~2.5s); **scan delete** is immediate.
- No page transitions/routing — this is a single persistent app shell with local component state switching between tab and flow states (see Data model below).

### Why no live camera preview
An earlier version of this prototype used `getUserMedia` for a live viewfinder. It was dropped because the permission is unreliable inside embedded/sandboxed preview frames and added complexity for no real benefit — triggering the OS camera app via a `capture` file input is simpler, more reliable across devices, and is exactly what a native Android implementation should do too (an in-app `CameraX` preview is a reasonable native equivalent if you want a nicer capture UX, but is not required — even calling the stock Camera app via intent and getting the result back is enough).

## State Management
Local component state (no routing):
- `activeTab`: `'scan' | 'library'`
- `flow`: `'live' | 'processing' | 'result' | 'error'` (Scan tab only)
- `currentScan`: the in-progress/most recent scan object (see Data model)
- `documents`: array of `{ id, name, createdAt }`
- `activeDocumentId`: which document new scans are filed into
- `scans`: array of all scan records across all documents
- `batchMode`: boolean, `batchCount`: number (resets on toggle)
- `editingResult` + `editVals`: manual-correction draft state for the current result
- `docPickerOpen`, `newDocName`: document-picker sheet state
- `libraryDocId`: which document is open in Library detail (null = list view)
- `renamingDoc`, `renameValue`: inline document-rename state
- `scanDetail`: which scan is open in Library's scan-detail view
- `confirmDeleteDocId`: arms the two-tap document delete
- `toast`: transient message string or null

### Data model
```
Document { id: string, name: string, createdAt: number(ms) }

Scan {
  id: string,
  sscc: string|null,              // 18 digits, no separators
  batchNo: string|null,
  gtin: string|null,
  bestBefore: string|null,        // stored as read/typed, not normalized to a date type
  quantity: string|null,
  confidence: 'high'|'medium'|'low',
  timestamp: number(ms),
  thumbnail: string,              // small base64 JPEG data URL — the ONLY image persisted
  documentId: string,             // FK -> Document.id
  edited: boolean (optional)      // true if manually corrected after OCR
}
```

### Local persistence (replace with proper Android local storage — Room DB recommended over raw key-value for the scans table)
The web prototype uses four `localStorage` keys, listed here so the equivalent Android storage covers the same surface:
- Documents list (JSON array)
- Scans list (JSON array, capped at 300 most-recent in the prototype — reconsider this cap for a Room-backed store)
- Active document id (string)
- Batch-mode on/off (string "1"/"0")

If storage write fails (the prototype handles a `localStorage` quota error by trimming the scans array in half and retrying) — with a proper database this shouldn't occur, but do handle low-storage gracefully.

---

## Design Tokens

**Colors**
- Page/void background: `#05070a`; radial backdrop `radial-gradient(circle at 30% 15%, #12161a 0%, #05070a 62%)` (this gradient is only around the phone frame in the prototype's desktop preview — inside the app itself the background is flat)
- App surface: `#0b0d0c`; camera-idle surface: `#0e1113`; header/sheet/nav surface: `#101314`
- Borders (all white-based, varying opacity): `rgba(237,237,233,.08 / .12 / .14 / .16 / .18 / .22)`
- Primary text: `#EDEDE9` and `#F5F5F0` (used near-interchangeably for near-white text)
- Secondary/tertiary text: `rgba(237,237,233, .4 / .45 / .5 / .6 / .7)` — pick opacity by hierarchy
- **Accent** (tweakable — ship a real color picker or fixed brand color): default `#F2A93C` (amber). Alternatives used in the prototype's tweak options: `#5FBF83` (green), `#4C8BF5` (blue), `#E5484D` (red).
- Text-on-accent (for filled amber buttons/badges): `#1a1204`
- Status: success `#5FBF83`, warning/medium-confidence `#F2C94C`, error/danger `#E5484D` (with tinted backgrounds `rgba(229,72,77,.12)` and borders `rgba(229,72,77,.3)`, text `#F5A3A6`)

**Typography**
- **IBM Plex Sans** — all UI copy, weights 400/500/600/700 ([Google Fonts](https://fonts.google.com/specimen/IBM+Plex+Sans))
- **IBM Plex Mono** — all data values (SSCC, batch, GTIN, dates, badge counts), weights 400–700 ([Google Fonts](https://fonts.google.com/specimen/IBM+Plex+Mono))
- Scale in use: 10 / 10.5 / 11 / 11.5 / 12 / 12.5 / 13 / 14 / 15 / 17 / 18 / 20 / 21px
- Micro labels (field names) are consistently: 10–11px, 600 weight, `letter-spacing: .08em`, `text-transform: uppercase`, at `rgba(237,237,233,.45)`

**Radii**: 8px (small tiles/buttons), 9–10px (inputs, cards), 12px (primary CTAs), 16px (sheets/modals), 100px (pills, toggles, badges, the doc-filing chip)

**Shadows**: sheet/modal `0 16px 40px rgba(0,0,0,.5)`; toast `0 8px 24px rgba(0,0,0,.4)`

**Spacing**: predominantly flex `gap` (not margins) at 2 / 4 / 6 / 8 / 10 / 12 / 14 / 16 / 18 / 24 / 28px; screen-edge padding typically 16–18px

## Assets
No bitmap/image assets. All icons are hand-authored inline SVG, 24×24 viewBox, ~1.6–1.8px stroke weight, no fill (outline style): camera, folder/document, image/gallery-picker, chevron-down, pencil/edit, checkmark. Recreate as vector icons (any icon set with a similar outline weight works, or export these exact SVG paths — see the `.dc.html` source).

Two Google Fonts (IBM Plex Sans, IBM Plex Mono) loaded via `fonts.googleapis.com` in the web version — bundle as local font files in the Android app instead of a network fetch.

The rounded device bezel visible around the app in the design files is a **preview-only frame** (from a generic "Android device mockup" helper) used to preview the design at desktop scale — it is not part of the shipped app UI.

## Files
- `SSCC Scanner (Claude version).dc.html` — full source, Claude-vision OCR primary path with on-device Tesseract fallback. Reference for exact visuals/copy/layout; do not port the Claude API call itself (requires login).
- `SSCC Scanner (offline on-device version).dc.html` — full source, Tesseract-only, zero external calls. **Reference this one for the OCR/parsing architecture** — its `extractFields` and `ssccCheckStatus` methods are the logic to port to Kotlin against ML Kit's output.
