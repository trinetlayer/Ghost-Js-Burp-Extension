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
