package ghostjs.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointExtractorTest {

    private final EndpointExtractor x = new EndpointExtractor();

    @Test
    void empty() {
        assertTrue(x.scan("https://example.com", null).isEmpty());
        assertTrue(x.scan("https://example.com", "").isEmpty());
    }

    @Test
    void apiPath() {
        List<Finding> f = x.scan("https://example.com/app.js", "fetch(\"/api/v2/users/1024\");");
        assertEquals(1, f.size());
        assertEquals("API Endpoint", f.get(0).type());
        assertTrue(f.get(0).value().contains("/api/v2/users/1024"));
    }

    @Test
    void s3() {
        List<Finding> f = x.scan("https://example.com/app.js",
                "const u = \"https://demo-assets.s3.amazonaws.com/private/dump.json\";");
        assertEquals(1, f.size());
        assertEquals("Cloud Storage URL", f.get(0).type());
        assertTrue(f.get(0).value().contains("s3.amazonaws.com"));
    }

    @Test
    void sourceMap() {
        List<Finding> f = x.scan("https://example.com/app.js", "//# sourceMappingURL=app.js.map\n");
        assertEquals(1, f.size());
        assertEquals("Source Map Reference", f.get(0).type());
        assertEquals("app.js.map", f.get(0).value());
    }

    @Test
    void mixed() {
        List<Finding> f = x.scan("https://example.com/app.js", """
                fetch("/api/v1/payment");
                const u = "https://my-bucket.s3.amazonaws.com/data";
                //# sourceMappingURL=bundle.js.map
                """);
        assertEquals(3, f.size());
    }

    @Test
    void dedupeWithinCategory() {
        List<Finding> f = x.scan("https://example.com/app.js",
                "fetch(\"/api/v1/payment\"); fetch(\"/api/v1/payment\");");
        assertEquals(1, f.size());
        assertNotNull(f.get(0).value());
    }

    @Test
    void impactAndRemediationSet() {
        List<Finding> f = x.scan("https://example.com/app.js", "fetch(\"/api/v2/users\");");
        assertTrue(f.get(0).impactSummary().length() > 20);
        assertTrue(f.get(0).remediation().length() > 5);
    }
}