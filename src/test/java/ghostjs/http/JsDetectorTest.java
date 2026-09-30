package ghostjs.http;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsDetectorTest {

    private static final String PAGE = "https://example.com/page/index.html";

    @Test
    void resolves() {
        Set<String> urls = JsDetector.extractJsUrls("""
                <script src="https://cdn.example.com/app.js"></script>
                <script src="/static/other.js"></script>
                """, PAGE);
        assertEquals(2, urls.size());
        assertTrue(urls.contains("https://cdn.example.com/app.js"));
        assertTrue(urls.contains("https://example.com/static/other.js"));
    }

    @Test
    void dedupe() {
        Set<String> urls = JsDetector.extractJsUrls("""
                <script src="app.js"></script>
                <script src="app.js"></script>
                """, PAGE);
        assertEquals(1, urls.size());
    }

    @Test
    void ignoreDataUri() {
        Set<String> urls = JsDetector.extractJsUrls(
                "<script src=\"data:text/javascript,alert(1)\"></script>", PAGE);
        assertTrue(urls.isEmpty());
    }

    @Test
    void ignoreProse() {
        Set<String> urls = JsDetector.extractJsUrls(
                "<p>Make sure to include app.js after bundle.js.</p>", PAGE);
        assertFalse(urls.stream().anyMatch(u -> u.endsWith("app.js")));
    }

    @Test
    void mjs() {
        Set<String> urls = JsDetector.extractJsUrls(
                "<script src=\"/modules/util.mjs\"></script>", PAGE);
        assertTrue(urls.contains("https://example.com/modules/util.mjs"));
    }

    @Test
    void queryString() {
        Set<String> urls = JsDetector.extractJsUrls(
                "<script src=\"/app.js?v=42\"></script>", PAGE);
        assertTrue(urls.contains("https://example.com/app.js?v=42"));
    }
}