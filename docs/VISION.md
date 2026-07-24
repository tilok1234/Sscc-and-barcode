# DCR (Damage Control Register) — Vision & Design Principles

*Name decided 2026-07-25: **DCR — Damage Control Register**, following the
industry's descriptor-initialism convention (WMS, ERP, SAP). "Nova DCR" is
reserved as the optional brand layer (brand + initialism, like "Astro WMS")
if this ever ships as a product. The technical applicationId keeps its
original value so installed devices retain their data across the rename.*

*Captured 2026-07-25 from design discussions during the first field-test week.
This is the north-star document: when a feature decision is unclear, check it
against these principles.*

---

## 1. What this is

A standalone Android app for warehouse/factory floor workers that:

1. Reads GS1 pallet labels (barcode-first, OCR fallback) in ~1 second with
   zero navigation cost, replacing manual retyping of 18-digit SSCCs.
2. Tracks the **damage lifecycle** of goods: flag a pallet → photograph the
   evidence → log timestamped notes → resolve as **restored** or **sanitized**
   with corrected quantities — replacing the paper sheet that physically
   follows a pallet and routinely gets lost.
3. Records **physical truth at the moment of contact** (decoded barcodes,
   photos, timestamps), in contrast to WMS systems that record *intent* and
   trust people to make reality match.

Everything runs on-device. No accounts, no login, no internet permission.

## 2. The problem it solves (field context)

Observed reality at a large beverage factory/warehouse (~600M NOK/yr):

- **Damage registration runs on paper.** A sheet with article nr, SSCC and
  reason follows the damaged pallet to a designated area; someone must also
  manually re-register the pallet's MHA in the WMS (Astro). Either step can
  silently fail. Descriptions live only on the paper.
- **Full pallets get sanitized** because partial-damage registration is too
  cumbersome — pure, recurring loss that better capture would prevent.
- **Ghost pallets:** system entries with no physical pallet and pallets with
  no system entry, reconciled by hope and quarterly admin cleanups.
- **WMS trusts compliance:** users can checksum their way past physical
  placement rules; the system can't tell.
- **Data errors go unnoticed:** e.g. a system granting +1 year shelf life
  past expiry; a month-only producer date entered as the 31st when the
  product was stamped the 9th. (Note: this app renders GS1 month-only dates
  as `YYYY-MM` — it never invents a day.)
- **WMS units ≠ product units:** Astro's STquantity counts pallet units
  (2 half-pallets = "2"), so actual product counts for damaged goods may not
  exist anywhere except this app.

### Astro WMS facts that matter for integration
- SSCC is the pallet's unique license plate, in the system and on the label.
- Every pallet lives on an **MHA** (materials handling area) + usually a
  position; some MHAs are mobile (conveyors, trailers, user's own unit).
- Astro has **no damage/status field** for pallets at all.
- Integration posture: **read/reconcile alongside Astro, never write into
  it** (permissions and politics). v1 "integration" = this app's CSV replaces
  the paper sheet wherever the paper currently gets typed in.

## 3. Design principles (settled — do not relitigate casually)

1. **Tamper-evident, not tamper-proof.** Lying can't be made impossible (the
   user controls what the camera sees). Make honest use the path of least
   resistance and make every deviation *visible in the record*.
2. **Free edits + immutable ledger.** Any field can be corrected (usability),
   but every change is appended to an edit ledger *before* the value is
   overwritten. The ledger has no update or delete path. Original values are
   always in reach (`18 (was 24)`).
3. **Accept unavoidable trust surfaces; instrument them. Never manufacture
   honeypots.** Self-reported quantities and camera framing can't be
   verified — record provenance (`source`: barcode/OCR/manual, `edited`,
   confidence) and learn from patterns. But known holes in safety-relevant
   data (expiry dates!) get closed, with attempts logged, not left open as
   sensors.
4. **Anonymous by design — capability absence over policy.** No user
   accounts, no names, no per-person analytics. If a central server is added:
   random rotatable device UUID only, timestamps coarsened server-side, and
   **no user column in the schema** — so surveillance isn't a toggle a future
   owner can flip; it's a feature that doesn't exist. This is a product
   differentiator: floor workers decide whether tools get used honestly, and
   they can smell surveillance.
5. **Record what is physically true, with zero navigation cost.** A scan is
   proof a pallet existed, at a time, in a photographed condition. Every
   workflow must survive gloves, one hand, and a 15-second attention budget.
6. **The app never fabricates data.** Month-only dates stay month-only;
   partial sums are omitted rather than shown misleadingly; unparseable
   values are kept raw, not "fixed."

### Explicitly rejected
- Flagging missing batch numbers as discrepancies (unbatched goods — cups,
  glasses, storage materials — are normal; the summary's "No batch no." row
  is visibility enough).
- Per-user tracking of any kind, even "just for debugging."
- Blocking edits to force honesty (produces workarounds, not truth).

### Parked for later (speculative, revisit with field data)
- **"Guilty confirmations":** targeted friction citing actual data ("Sanitize
  the whole pallet? It was reported with 2 broken bottles"). Evidence says
  specific prompts work and generic ones breed click-through blindness.
  Requires months of baseline data first — and if analytics ever inform
  interventions, they stay aggregate, never per-person.
- Location/MHA field on scans and damage reports (mirror "moved to damage
  area" without touching Astro).
- Implausibly-distant best-before warning (e.g. >18 months out, threshold in
  settings) — would have caught the +1-year system bug.

## 4. Current state (v0.1.x, ~build 22)

Working, field-testing now: barcode mode (live, aim-gated, multi-symbol
merge, zoom 1–3×) · label mode (shutter → barcode+OCR merge) · batch mode
(named batch documents, instant save, dedupe, per-batch summary with
mixed-date and expired-date discrepancy flags, opt-in summary for normal
docs) · damage log (photos, timestamped notes, restored/sanitized statuses
with corrected quantities) · notes+photos+full edit history on every scan ·
label photo stored per scan · CSV export (incl. Source/Damaged/Article No
columns) · retention auto-compress/auto-delete (damage-flagged scans never
auto-delete) · fields: SSCC, batch, GTIN, best-before, quantity, article no.

Pipeline: phone-only. CI builds every push; fixed download link
(`releases/download/latest/sscc-scanner.apk`); updates preserve data
(additive Room migrations v1→v7).

## 5. Field-test protocol (running now)

- Collect **label diversity, not volume**: a few scans per distinct label
  layout (imports especially), every misread, every failure. Label mode's
  photo captures the evidence even when parsing fails.
- The app self-collects reliability stats: % scans clean without manual
  correction (source + edited flags), confidence distribution.
- Feel-questions: is aim-gating precise or fussy? Is the 1.2s merge window
  snappy or laggy? Does zoom fix budget-camera blur?
- **Discretion rule:** only scan physical labels passing through your hands
  as normal work. No bulk WMS exports. Reconciliation experiments happen
  later, with IT's blessing, using their export.

## 6. Roadmap (in value order)

1. **Now — field-test iteration.** Tune parser/UX from real labels and
   misreads. Refine article-number extraction from actual label photos.
2. **Central sync (opt-in).** Small server owned by the operator
   (**PocketBase** on a cheap VPS/NAS is the recommendation; Supabase if
   hosting nothing is preferred). App gets "Send to server" (URL + key in
   settings): document JSON bundle + photos, retry on connectivity. Random
   device UUID, no user identity, timestamps coarsened server-side. This is
   also what makes records genuinely tamper-resistant (they leave the phone).
3. **Reconciliation.** Server compares app records vs a WMS export (CSV):
   ghost-pallet quantification, damage totals, restored-vs-sanitized rates,
   actual product counts. This produces the numbers for any internal pitch.
4. **Pitch-or-hobby decision point.** Three months of real data decides.
   The pitch is losses quantified, not features demoed.

## 7. Should we restart from scratch? (assessed 2026-07-25)

**No.** Reasons:

- The architecture already matches the vision: pure-JVM `core/` with the
  domain logic (GS1 parsing, validation, extraction, batch analysis, CSV)
  under unit test; Android layer cleanly on top; additive migrations;
  append-only ledger. Nothing structural fights the roadmap — sync bolts on
  as a new module without touching scanning.
- The app is mid-field-test. A rewrite trades a working tool collecting real
  data for months of reimplementing what already works — the classic rewrite
  trap. Field learnings should drive code, not the reverse.
- What a restart would *actually* buy is cosmetic: tidier file layout, fewer
  accreted UI files. That's a refactor, not a rewrite.

**Instead: a consolidation pass, scheduled, not urgent:**
- Split the largest UI files (LibraryScreen.kt has accreted list + detail +
  editing + history; ScanScreen.kt similar) into one-file-per-screen.
- Extract the shared button/pill/field composables into a single components
  file (three near-copies exist).
- Add core tests for edit-ledger diff logic and batch-name flows.
- Decide the real product name + application id before any distribution
  beyond the author's phone (changing applicationId later = a new app on
  every device).
- Consider a `sync/` module skeleton so principle #4's schema constraints
  are encoded in code review, not memory.

*This document supersedes chat history. Update it when principles change —
and changing §3 requires a better argument than convenience.*
