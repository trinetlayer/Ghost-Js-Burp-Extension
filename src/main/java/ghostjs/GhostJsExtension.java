package ghostjs;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import ghostjs.core.EndpointExtractor;
import ghostjs.core.FindingStore;
import ghostjs.core.SecretScanner;
import ghostjs.http.ActiveJsFetcher;
import ghostjs.http.GhostConfig;
import ghostjs.http.JsScanHandler;
import ghostjs.ui.GhostJsTab;
import ghostjs.ui.GhostTheme;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * GhostJS — a Burp Suite (Montoya API) extension that passively scans every
 * JavaScript (and optional HTML) response for hardcoded secrets and attack-
 * surface discovery data, actively fetches referenced JS bundles, highlights
 * hits in the proxy history, and lists them in a dedicated suite tab.
 *
 * Detection patterns are generated from the GhostJS TypeScript engine
 * (see export-patterns.mjs / GeneratedPatterns) — single source of truth.
 */
public final class GhostJsExtension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName(Branding.EXTENSION_NAME);

        SecretScanner scanner = new SecretScanner();
        EndpointExtractor extractor = new EndpointExtractor();
        FindingStore store = new FindingStore();
        GhostConfig config = new GhostConfig();

        ExecutorService scanPool = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "ghostjs-scan");
            t.setDaemon(true);
            return t;
        });

        ActiveJsFetcher fetcher = new ActiveJsFetcher(api, scanner, extractor, store, config);
        JsScanHandler handler = new JsScanHandler(api, scanner, extractor, store, fetcher, config, scanPool);
        api.http().registerHttpHandler(handler);

        GhostJsTab tab = buildTab(store, config, scanner.patternCount());
        api.userInterface().registerSuiteTab(Branding.TAB_TITLE, tab.component());

        api.extension().registerUnloadingHandler(() -> {
            fetcher.shutdown();
            scanPool.shutdownNow();
            try {
                scanPool.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        api.logging().logToOutput(Branding.BANNER);
        api.logging().logToOutput(Branding.LOG_PREFIX + "loaded — " + scanner.patternCount() + " patterns active");
        if (!scanner.compileFailures().isEmpty()) {
            api.logging().logToOutput(Branding.LOG_PREFIX + scanner.compileFailures().size()
                    + " pattern(s) skipped (regex incompatible with Java):");
            scanner.compileFailures().forEach(f -> api.logging().logToOutput("    - " + f));
        }
    }

    private static GhostJsTab buildTab(FindingStore store, GhostConfig config, int patternCount) {
        // Install the theme and build the tab on the EDT together — FlatLaf's
        // install touches live Swing component trees, and the tab itself must
        // be constructed on the EDT.
        // If initialize() is ever invoked on the EDT, invokeAndWait would throw — build
        // the tab directly in that case.
        if (SwingUtilities.isEventDispatchThread()) {
            GhostTheme.install();
            return new GhostJsTab(store, config, patternCount);
        }
        final GhostJsTab[] holder = new GhostJsTab[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                GhostTheme.install();
                holder[0] = new GhostJsTab(store, config, patternCount);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return holder[0];
    }
}
