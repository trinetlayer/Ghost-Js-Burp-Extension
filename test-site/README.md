# GhostJS Test Target

A tiny site seeded with **fake, format-valid** secrets (not real credentials) so you can
see GhostJS light up inside Burp and take a screenshot.

## Steps

1. **Build + load the extension**

   ```bash
   cd .. && ./build.sh
   ```
   In Burp: `Extensions ▸ Installed ▸ Add ▸ Java ▸` select `dist/ghostjs.jar`.
   A **GhostJS** tab appears.

2. **Serve this test site**

   ```bash
   ./serve.sh          # or:  python3 -m http.server 8000
   ```

3. **Point a browser at Burp's proxy** (default `127.0.0.1:8080`) — e.g. Burp's built-in
   browser (`Proxy ▸ Intercept ▸ Open Browser`) needs no setup.

4. **Browse** `http://localhost:8000/` through that browser.
   GhostJS passively scans the HTML + each `<script>`, and active-fetches the referenced
   bundles.

5. **Open the GhostJS tab** — you should see ~18 findings across all severities, and the
   matching entries in **Proxy ▸ HTTP history** coloured red/orange/yellow. Screenshot away.
   Click any row for impact + remediation; **Export report** saves them as Markdown.

## What you should see

- **CRITICAL** — AWS Access Key (×2), AWS Secret, Stripe Live Key, GitHub PAT (×2),
  Slack Token, Google OAuth Secret
- **MEDIUM** — Firebase Configuration Block
- **LOW** — Google API Key (×2), Cloud Storage URL, Source Map Reference
- **INFO** — Sentry DSN (from inline HTML), API endpoints
- **Nothing** from `js/placeholders.js` — those are AWS/JWT doc samples, a Stripe
  publishable key, an i18n label, etc. Their absence proves the false-positive filter works.

## Verifying Discovery findings (source map, cloud URL, API endpoints)

These three types are **references extracted from JS**, reported at low/info with confidence ≤ 70.
GhostJS does not request them. The presence of the string in the JS body *is* the true positive;
whether the resource is live is a separate, manual check.

- `js/vendor.min.js.map` is served on purpose. `curl -I http://localhost:8000/js/vendor.min.js.map`
  returns **200**, demonstrating a real exposed source map.
- The S3 URL in `js/config.js` is a fake demo bucket and will **404**. Expected.
- `serve.sh` is `python3 -m http.server`, a static file server with no backend. `/v1/graphql`,
  `/api/v2/users/1024/payment` and `/api/internal/admin/config` return **404 by design**. Do not
  classify those as false positives; there is no route table for them to hit.
