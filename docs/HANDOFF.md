# DCR — Developer Handoff

*Written 2026-07-24, at the end of the first build phase. Audience: any
developer (human or AI session) picking this project up cold. Read
`docs/VISION.md` first for the "why"; this document is the "how" and
"where things stand". `PLAN.md` is the original build plan, kept for
history — this document supersedes its status information.*

---

## 1. Thirty-second orientation

**DCR (Damage Control Register)** is a standalone Android app for
warehouse floor workers: scan a GS1 pallet label in ~1 second (SSCC,
batch, GTIN, best-before, quantity, article number), and track the
damage lifecycle of goods (photos, timestamped notes, restored/sanitized
resolution) — replacing both manual retyping of 18-digit codes and the
paper damage sheet. Fully on-device: no accounts, no network calls, no
internet permission.

- **Status:** v0.1.x, working, in active field testing at a beverage
  factory warehouse. All planned passes complete; project parked green
  awaiting field-test findings.
- **Owner's phone is the only install.** Data on it matters to the
  owner as test data, but nothing is production-critical yet.
- **The product's center of gravity moved during development** from
  "SSCC scanner" to "damage register" — the rename to DCR reflects that.

## 2. Repository map

```
core/    Pure-JVM Kotlin module (no Android SDK) — all domain logic, fully
         unit-tested (43+ tests). Included as a composite build
         (settings.gradle.kts: includeBuild("core"), dep "com.ssccscanner:core").
  Gs1Parser.kt         GS1 element-string parsing (AIM prefixes, FNC1/GS
                       separators, fixed/variable-length AIs, date pivot,
                       month-only dates render "YYYY-MM" — never invent a day)
  SsccValidator.kt     SSCC mod-10 check digit; distinct wrong-length vs
                       bad-check-digit states; GTIN check
  LabelTextParser.kt   OCR-text field extraction (port of the web prototype's
                       extractFields, + article-number regex)
  BarcodeInterpreter.kt  Barcode payload → fields (direct GTIN formats,
                       bare-18-digit SSCC, else GS1 parse)
  FieldMerger.kt       Merge barcode + OCR results; barcode always wins
  BatchAnalyzer.kt     Per-batch grouping + discrepancy detection (mixed
                       best-before dates, expired dates; missing batch no.
                       is deliberately NOT a discrepancy)
  CsvBuilder.kt        CSV export incl. Source/Edited/Damaged/Article No
  ScanFields.kt        The shared field container + confidence/source enums

app/     Android app (Kotlin + Jetpack Compose, Material 3 custom dark theme)
  data/      Room database (v7), DataStore prefs, repository, CSV share
    Entities.kt        Documents, scans (labelPhotoPath, originalQuantity,
                       articleNo, isBatch, showSummary), scan notes/photos,
                       field_edits (append-only edit ledger)
    DamageEntities.kt  Damage reports (open/restored/sanitized), damage
                       notes (kind: note/restored/sanitized), damage photos
    ScannerDatabase.kt Room v7; MIGRATION_1_2 … 6_7, all additive SQL
    ScannerRepository.kt  All business operations; logFieldEdits() appends
                       old→new diffs BEFORE overwriting; retention cleanup
                       (auto-compress/auto-delete; damage-flagged scans are
                       never auto-deleted); label photo files under
                       filesDir/label_photos/
  scan/      CameraX + ML Kit pipeline
    BarcodeAnalyzer.kt Live analyzer; aim gating (only barcodes in the
                       center window 0.12–0.88 × 0.20–0.80 count)
    ScanViewModel.kt   Modes (BARCODE live / LABEL shutter), 1200 ms
                       multi-symbol aggregation, 5000 ms dedupe, batch-mode
                       instant save, edit flow
    StillImageProcessor.kt  Shutter/gallery path: barcode + OCR merged
    ImageUtils.kt      Scaled decode (bounds-pass bug fixed — see §6),
                       label/evidence/thumbnail JPEG budgets
  ui/        Compose screens; tab order Scan | Damage | Library
    scan/      ScanScreen (camera/processing/error, zoom 1→2→3×),
               ReadyOverlay (reticle, mode toggle, batch prompt), ResultView
    library/   LibraryScreen (doc list, Σ summary toggle), DocumentDetail
               (batch summary card), ScanDetail (full edit + amber
               read-only edit-history box, "N (was M)" quantities)
    damage/    DamageScreen (status icons/filters, status-note composer,
               qty-after-restore field), DamageViewModel
    components/Components.kt  THE shared button/pill/field composables —
               do not re-create local copies
    AttachmentSections.kt / PhotoViewer.kt  notes+photos UI shared by
               scan detail and damage detail
  debug.keystore   Committed fixed debug signing key (standard
               androiddebugkey/android creds) — see §5

docs/
  VISION.md        North-star: principles (§3 is settled — don't relitigate
                   casually), field context, roadmap. THE authority.
  HANDOFF.md       This file.
  design_handoff/  Original web-prototype spec (reference; superseded in
                   places by the native app's evolution)
PLAN.md            Original assessment + phased build plan (historical)
```

## 3. Build & CI — READ THIS FIRST if you're an AI session

**The app module cannot be compiled in the usual remote sandbox**:
`dl.google.com` (Google's Maven repo) is blocked by the proxy, so AGP and
Android dependencies can't resolve locally. Maven Central works, so:

- `./gradlew -p core test` **works locally** — the core module is pure
  JVM with deps from Maven Central only. Run this before every push.
- The **app compiles only in GitHub Actions** (`.github/workflows/android.yml`).
  CI is the compile-check loop: push, then poll the run via the GitHub
  API. Every push so far has followed this rhythm; ~15+ green builds.
- The workflow ignores `docs/**` and `**.md` pushes (no build churn).

CI publishes every green build to a **fixed release URL** the owner has
bookmarked on their phone:

> https://github.com/tilok1234/Sscc-and-barcode/releases/download/latest/sscc-scanner.apk

The `latest` release is deleted and recreated each build (title
"DCR (Damage Control Register) — build N").

## 4. Invariants — do not break these

1. **`applicationId = "com.ssccscanner"` stays** until backup/restore
   exists (VISION roadmap 1b). Changing it = a new app on the owner's
   phone = field-test data loss. The final owned ID cutover is planned
   as one clean break AFTER export/import ships.
2. **Release asset stays named `sscc-scanner.apk`** and the tag stays
   `latest` — the owner's phone bookmark depends on the exact URL.
3. **Room migrations are additive-only** and every schema change needs a
   new version + migration. The owner updates over the air; a destructive
   migration wipes their field data.
4. **The `field_edits` ledger is append-only.** No update or delete DAO
   method may ever be added. Same for damage status notes. Free edits +
   immutable ledger is design principle #2.
5. **The committed `app/debug.keystore` must not be regenerated** —
   a new key means signature mismatch and the owner must uninstall
   (losing data) to update.
6. **No internet permission.** Adding network features happens in the
   planned opt-in sync layer (VISION roadmap 2), designed around
   anonymity-by-capability-absence — no user identity anywhere.
7. **Never fabricate data**: month-only GS1 dates stay `YYYY-MM`;
   unparseable values are kept raw; partial sums omitted, not guessed.
8. **Missing batch numbers are not errors.** Unbatched goods are normal.

## 5. Distribution & signing

- Debug builds signed with the committed `app/debug.keystore`
  (storepass/keypass `android`, alias `androiddebugkey`) so every CI
  build installs over the previous one.
- `versionCode` = `GITHUB_RUN_NUMBER` (monotonic), `versionName` =
  `0.1.<run>`. Local builds fall back to versionCode 1.
- Min SDK 24, target/compile SDK 35. Compose BOM 2024.10.01, AGP 8.7.3,
  Kotlin 2.0.21, KSP 2.0.21-1.0.25, CameraX 1.4.0, ML Kit barcode 17.3.0
  / text 16.0.1, Room 2.6.1. Gradle wrapper 8.14.3.

## 6. Bugs already fought (don't refight them)

- **`BitmapFactory.decodeStream` returns null on the bounds pass by
  design** (`inJustDecodeBounds=true`). Judging that pass by its return
  value made every label photo "unreadable". Judge by
  `outWidth/outHeight` (fixed in `ImageUtils.loadScaled`).
- **Preview.surfaceProvider is set via `setSurfaceProvider(...)`**, not
  property assignment.
- **DataStore `Flow.collect` never completes** — use `.first()` when you
  need one value (an early `ensureActiveDocumentId` hung on this).
- **A literal GS control byte (0x1D) in source** breaks tooling — always
  the escape `'\u001D'`.
- **Per-runner debug keys** caused the original "update won't install"
  bug — hence the committed keystore.
- Live scanning grabbed a stray barcode while the user was still aiming
  at a label wall — hence aim gating + the LABEL (shutter) mode.

## 7. Where things stand / what comes next

**Running now: field test** (VISION §5). The owner scans real pallet
labels during normal work — collecting label diversity, misreads, and
UX feel (aim gating, 1.2 s merge window, zoom). Discretion rule: only
physical labels passing through their hands; no bulk WMS exports.

**Awaiting from the owner** (blocks the next work, in rough order):
1. Field-test findings → parser/UX tuning.
2. A photo of their company's standard label → identify the three
   barcodes' exact AI contents, tune article-number extraction.
3. Astro WMS export/report format details → design the reconciliation
   input (read-only; never write into Astro).
4. Their thinking on the anonymity model → sync-layer design.

**Roadmap** (full rationale in VISION §6): 1) field-test iteration →
1b) backup/restore, then applicationId cutover → 2) opt-in sync
(PocketBase recommended; random device UUID, no user column, timestamps
coarsened server-side) → 3) reconciliation vs WMS exports →
4) pitch-or-hobby decision after ~3 months of real data.

**Parked features** (VISION §3 — revisit only with field data): guilty
confirmations, MHA/location field, implausibly-distant best-before
warning (>18 months).

**Process intent, agreed with the owner:** the current codebase is a
deliberate prototype. Once field experience accumulates, a full
specification will be distilled from the prototype + logs + VISION, and
a clean commercial build planned from that spec. Quality over speed;
no rush. Don't "tidy toward production" piecemeal in the meantime —
the consolidation pass already done (split screens, shared components)
is the intended level of hygiene for this phase.

## 8. Working agreement with the owner

- Commit and push in small, complete passes with clear messages; verify
  core tests locally, then confirm CI green before calling a pass done.
- The owner tests on a budget Android phone via the fixed APK link —
  ergonomics findings (blur, glove use, one-handed reach) are design
  input, not nitpicks.
- Honest assessment is explicitly wanted, including "this idea is
  weak" — the owner asks for critique and acts on it.
- Never add anything that could function as worker surveillance. This
  is a hard product line (VISION §3, principle 4), not a preference.
