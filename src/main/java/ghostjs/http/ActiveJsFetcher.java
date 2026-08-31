package ghostjs.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import ghostjs.core.EndpointExtractor;
import ghostjs.core.Finding;
import ghostjs.core.FindingStore;
import ghostjs.core.SecretScanner;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fetches JavaScript files that were referenced by a page but not necessarily
 * loaded through the proxy, then scans them. De-duplicates by absolute URL so
 * the same bundle is never fetched or scanned twice.
 */
public final class ActiveJsFetcher {

    private final MontoyaApi api;
    private final SecretScanner scanner;
    private final EndpointExtractor extractor;
    private final FindingStore store;
    private final GhostConfig config;

    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "ghostjs-fetch");
        t.setDaemon(true);
        return t;
    });

    public ActiveJsFetcher(MontoyaApi api, SecretScanner scanner,
                           EndpointExtractor extractor, FindingStore store, GhostConfig config) {
        this.api = api;
        this.scanner = scanner;
        this.extractor = extractor;
        this.store = store;
        this.config = config;
    }

    /** Record that a URL was already scanned (e.g. seen live in the proxy). */
    public void noteAlreadySeen(String url) {
        if (url != null) seen.add(normalize(url));
    }

    /**
     * Queue a referenced JS URL for fetch+scan if not seen and in policy.
     * {@code cookieHeader} is the originating page's Cookie header; it is replayed
     * only when the JS URL is on the same host, so authenticated bundles are
     * scanned with the live session instead of as an anonymous request.
     */
    public void enqueue(String jsUrl, String pageUrl, String cookieHeader) {
        if (!config.activeFetchEnabled) return;
        String key = normalize(jsUrl);
        if (!seen.add(key)) return;               // already fetched/seen
        if (!allowed(jsUrl, pageUrl)) return;
        String cookie = sameHost(jsUrl, pageUrl) ? cookieHeader : null;
        pool.submit(() -> fetchAndScan(jsUrl, cookie));
    }

    private void fetchAndScan(String jsUrl, String cookieHeader) {
        try {
            HttpRequest req = HttpRequest.httpRequestFromUrl(jsUrl);
            if (cookieHeader != null && !cookieHeader.isBlank()) {
                req = req.withHeader("Cookie", cookieHeader);
            }
            HttpRequestResponse rr = api.http().sendRequest(req);
            HttpResponse resp = rr.response();
            if (resp == null) return;
            String body = resp.bodyToString();
            if (body == null || body.isBlank()) return;

            List<Finding> found = scanner.scan(jsUrl, body);
            found.addAll(extractor.scan(jsUrl, body));
            int added = store.addAll(found);
            if (added > 0) {
                api.logging().logToOutput(ghostjs.Branding.LOG_PREFIX + "active-fetch " + jsUrl
                        + " -> " + added + " new finding(s)");
            }
        } catch (Exception e) {
            api.logging().logToError(ghostjs.Branding.LOG_PREFIX + "active-fetch failed: " + jsUrl + " (" + e.getMessage() + ")");
        }
    }

    private boolean allowed(String jsUrl, String pageUrl) {
        try {
            if (config.respectScope && api.scope().isInScope(jsUrl)) return true;
            return sameHost(jsUrl, pageUrl);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean sameHost(String a, String b) {
        try {
            String ha = URI.create(a).getHost();
            String hb = URI.create(b).getHost();
            return ha != null && ha.equalsIgnoreCase(hb);
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalize(String url) {
        if (url == null) return "";
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    public void shutdown() {
        pool.shutdownNow();
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
