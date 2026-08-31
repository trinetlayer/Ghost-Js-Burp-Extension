package ghostjs.http;

/** Runtime toggles for the extension, mutated from the UI. */
public final class GhostConfig {
    public volatile boolean scanEnabled = true;
    public volatile boolean activeFetchEnabled = true;
    public volatile boolean respectScope = true;
    public volatile boolean scanHtmlBodies = true;
    public volatile boolean scanJsonBodies = true;
    public volatile boolean highlightProxy = true;

    /**
     * Bodies at or below this size are scanned inline on the proxy thread so the
     * matching proxy-history entry can be highlighted immediately. Larger bodies
     * are scanned on a background pool (tab + log only) so the proxy thread — and
     * therefore the user's browsing — is never blocked. ~120 KB ≈ under ~200 ms.
     */
    public volatile int inlineScanLimit = 120_000;
}
