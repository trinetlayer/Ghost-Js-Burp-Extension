package ghostjs.http;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Annotations;
import burp.api.montoya.core.HighlightColor;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import ghostjs.core.EndpointExtractor;
import ghostjs.core.Finding;
import ghostjs.core.FindingStore;
import ghostjs.core.SecretScanner;
import ghostjs.core.Severity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Passive handler. Small responses are scanned inline so the proxy-history entry
 * can be highlighted immediately; larger responses are scanned on a background
 * pool so the proxy thread — and the user's browsing — is never blocked. For
 * HTML, referenced JS bundles are handed to the {@link ActiveJsFetcher}.
 */
public final class JsScanHandler implements HttpHandler {

    private final MontoyaApi api;
    private final SecretScanner scanner;
    private final EndpointExtractor extractor;
    private final FindingStore store;
    private final ActiveJsFetcher fetcher;
    private final GhostConfig config;
    private final ExecutorService scanPool;

    public JsScanHandler(MontoyaApi api, SecretScanner scanner, EndpointExtractor extractor,
                         FindingStore store, ActiveJsFetcher fetcher, GhostConfig config,
                         ExecutorService scanPool) {
        this.api = api;
        this.scanner = scanner;
        this.extractor = extractor;
        this.store = store;
        this.fetcher = fetcher;
        this.config = config;
        this.scanPool = scanPool;
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent request) {
        return RequestToBeSentAction.continueWith(request);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived response) {
        if (!config.scanEnabled) {
            return ResponseReceivedAction.continueWith(response);
        }
        // Skip our own active-fetch traffic; the fetcher scans those bodies itself.
        if (response.toolSource().isFromTool(ToolType.EXTENSIONS)) {
            return ResponseReceivedAction.continueWith(response);
        }

        String url = safeUrl(response);
        boolean isJs = JsDetector.isJavaScript(response, url);
        boolean isHtml = JsDetector.isHtml(response);
        boolean isJson = JsDetector.isJson(response, url);

        boolean scannable = isJs
                || (isHtml && config.scanHtmlBodies)
                || (isJson && config.scanJsonBodies);
        if (!scannable) {
            return ResponseReceivedAction.continueWith(response);
        }

        String body = response.bodyToString();
        if (body == null || body.isEmpty()) {
            return ResponseReceivedAction.continueWith(response);
        }
        if (isJs) {
            fetcher.noteAlreadySeen(url);
        }
        String cookie = safeCookie(response);
        boolean extractJs = isHtml && config.activeFetchEnabled;

        // Large bodies are scanned off the proxy thread so browsing never stalls.
        if (body.length() > config.inlineScanLimit) {
            final String fUrl = url, fBody = body, fCookie = cookie;
            final boolean fExtract = extractJs;
            try {
                scanPool.submit(() -> {
                    List<Finding> found = scanAll(fUrl, fBody);
                    if (fExtract) enqueueJs(fBody, fUrl, fCookie);
                    int added = store.addAll(found);
                    if (added > 0) {
                        api.logging().logToOutput(ghostjs.Branding.LOG_PREFIX + fUrl
                                + " -> " + added + " new finding(s) [async]");
                    }
                });
            } catch (RuntimeException ignored) {
                // pool shutting down (extension unloading) — safe to drop.
            }
            return ResponseReceivedAction.continueWith(response);
        }

        // Small body: scan inline so we can highlight the proxy entry now.
        List<Finding> found = scanAll(url, body);
        if (extractJs) enqueueJs(body, url, cookie);
        int added = store.addAll(found);
        if (added == 0 || !config.highlightProxy) {
            return ResponseReceivedAction.continueWith(response);
        }

        String worst = worstSeverity(found);
        api.logging().logToOutput(ghostjs.Branding.LOG_PREFIX + url + " -> " + added
                + " new finding(s) (worst: " + worst + ")");
        String notes = "GhostJS: " + added + " finding(s), worst=" + worst;
        return ResponseReceivedAction.continueWith(response, Annotations.annotations(notes, colorFor(worst)));
    }

    private List<Finding> scanAll(String url, String body) {
        List<Finding> found = new ArrayList<>(scanner.scan(url, body));
        found.addAll(extractor.scan(url, body));
        return found;
    }

    private void enqueueJs(String body, String pageUrl, String cookie) {
        Set<String> jsUrls = JsDetector.extractJsUrls(body, pageUrl);
        for (String jsUrl : jsUrls) {
            fetcher.enqueue(jsUrl, pageUrl, cookie);
        }
    }

    private static String worstSeverity(List<Finding> found) {
        String worst = "info";
        for (Finding f : found) {
            if (Severity.rank(f.severity()) < Severity.rank(worst)) worst = f.severity();
        }
        return worst;
    }

    private static HighlightColor colorFor(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase()) {
            case "critical", "high" -> HighlightColor.RED;
            case "medium" -> HighlightColor.ORANGE;
            case "low" -> HighlightColor.YELLOW;
            default -> HighlightColor.GRAY;
        };
    }

    private static String safeUrl(HttpResponseReceived response) {
        try {
            return response.initiatingRequest().url();
        } catch (Exception e) {
            return "(unknown)";
        }
    }

    private static String safeCookie(HttpResponseReceived response) {
        try {
            return response.initiatingRequest().headerValue("Cookie");
        } catch (Exception e) {
            return null;
        }
    }
}
