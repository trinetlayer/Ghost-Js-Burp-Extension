# GhostJS — Technical Notes

Deep-dive for developers and reviewers. For install/usage see the [README](../README.md).

## Architecture

```
ghostjs/
  GhostJsExtension     entry point (BurpExtension) — wires everything, owns the scan pool
  Branding             all product/vendor strings in one place
  core/
    GeneratedPatterns  AUTO-GENERATED 150 patterns from the TS engine (do not edit)
    RawPattern         uncompiled pattern record (name, source, flags, severity, …)
    SecretScanner      compiles regex, scans text, applies FP filter, time-bounded
    Entropy            false-positive suppression (ported from validation.ts)
    EndpointExtractor  API endpoints / cloud-storage URLs / source maps
    Finding            one detection (type, value, url, line, impact, remediation)
    FindingStore       thread-safe, de-duplicating store + UI listeners
    Severity           severity ordering
  http/
    JsScanHandler      passive HttpHandler; inline for small bodies, async for large
    ActiveJsFetcher    fetch + scan referenced JS bundles (scope-gated, cookie-aware)
    JsDetector         classify JS/HTML/JSON; extract JS URLs from HTML
    GhostConfig        runtime toggles
  ui/
    GhostJsTab         suite tab: table + detail + toolbar + export
    FindingsTableModel table model
```

## Single source of truth for patterns

The 150 regexes are **not** hand-written here. `export-patterns.mjs` imports `SECRET_PATTERNS`
from the TrinetLayer GhostJS TypeScript engine (`secret-patterns.ts`) and emits
`GeneratedPatterns.java`. `build.sh` re-runs it automatically when the engine is present.

```bash
node --import tsx export-patterns.mjs   # or: npx tsx export-patterns.mjs
```

JS `RegExp` flags map to Java: `i → CASE_INSENSITIVE|UNICODE_CASE`, `m → MULTILINE`,
`s → DOTALL`, `g` is implicit (we loop `Matcher.find()`). Any pattern that fails to compile
under Java regex is skipped and logged rather than breaking the extension.

## Performance & robustness

- **Non-blocking**: bodies larger than `GhostConfig.inlineScanLimit` (120 KB) are scanned on
  a 3-thread background pool, so the proxy thread is never stalled. Smaller bodies scan inline
  so their Proxy-history row can be highlighted immediately.
- **Time budget**: each scan is capped (`SecretScanner`, ~2.5 s) so a pathological body can't
  run away.
- **Exception isolation**: each pattern's match loop is wrapped — one bad pattern or input
  can't crash the handler.
- **Cost**: roughly linear, ~1.5 ms/KB across all 150 patterns.

## Validation (against `ghostjs-testbed/`)

- **All 150 patterns compile** under Java regex — 0 incompatibilities.
- **Positives**: 45/46 files flagged, **143/150 distinct pattern types fire**. The 7 that
  don't are low/info types whose value is still surfaced under a sibling type (e.g. a GCP
  service-account key's PEM fires as a private-key finding) or via the discovery detector.
- **Negatives**: **4 residual matches**, all in `commented-out.js` — flagged by design.
  The jwt.io demo token, i18n labels, and public-by-design keys are correctly suppressed.
- **Runtime**: the full `initialize()` was executed against a mock Montoya API — registers
  1 HTTP handler + 1 suite tab, builds the Swing tab, 0 exceptions. Burp's `ServiceLoader`
  discovery resolves `ghostjs.GhostJsExtension`.

## False-positive filter (`Entropy.java`)

Ported from the TS `validation.ts` + the known-example lists in `secret-patterns.ts`:
- documented sample credentials (AWS `…EXAMPLE`, the jwt.io demo token)
- public-by-design keys (Stripe `pk_`, reCAPTCHA site keys) — except real private keys
- common FP shapes (32-hex with name exemptions, repeated chars, kebab slugs, localhost URLs)
- placeholder/template markers, UI/natural-language text, low-entropy junk
- slash-rooted identifier paths (endpoints, not credentials)

Not ported: the full context-scoring (variable-name / file-path weighting) from `validation.ts`.

## Compatibility

Built against **Montoya API 2026.7**, but only long-stable API (since ~2023) is used, so it
loads on any recent Burp. For a much older Burp, build against an older API:

```bash
MONTOYA=2023.12 ./build.sh
```

## Known limitations

- Findings from large (async-scanned) bodies appear in the tab + log but are not inline-
  highlighted in Proxy history (Montoya doesn't reliably annotate a past item from a
  background thread).
- Comment-embedded secrets are reported (intentional divergence from the TS testbed).
- Active fetch honours Burp scope and replays same-host cookies, but doesn't throttle beyond
  its 4-thread pool.
