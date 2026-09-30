package ghostjs.http;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsDetectorTest {

    private static final String PAGE = "https://example.com/page/index.html";

    @Test
    void extractJsUrlsResolvesAbsoluteAndRelative() {
        String html = "<script src=\"https://cdn.example.com/app.js\"></script>\n"
                + "<script src=\"/static/other.js\"></script>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://cdn.example.com/app.js"));
        assertTrue(urls.contains("https://example.com/static/other.js"));
    }

    @Test
    void extractJsUrlsDeduplicates() {
        String html = "<script src=\"app.js\"></script>\n"
                + "<script src=\"app.js\"></script>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertEquals(1, urls.size());
    }

    @Test
    void extractJsUrlsIgnoresDataUris() {
        String html = "<script src=\"data:text/javascript,alert(1)\"></script>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertTrue(urls.isEmpty());
    }

    @Test
    void extractJsUrlsIgnoresBareWordsInProse() {
        String html = "<p>Make sure to include app.js after bundle.js.</p>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertFalse(urls.stream().anyMatch(u -> u.endsWith("app.js")),
                "bare prose words should not trigger fetches");
    }

    @Test
    void extractJsUrlsFindsMjs() {
        String html = "<script src=\"/modules/util.mjs\"></script>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertTrue(urls.contains("https://example.com/modules/util.mjs"));
    }

    @Test
    void extractJsUrlsHandlesQueryStrings() {
        String html = "<script src=\"/app.js?v=42\"></script>";

        Set<String> urls = JsDetector.extractJsUrls(html, PAGE);

        assertTrue(urls.contains("https://example.com/app.js?v=42"));
    }
}