package ghostjs.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndpointExtractorTest {

    private final EndpointExtractor extractor = new EndpointExtractor();

    @Test
    void emptyBodyReturnsNothing() {
        assertTrue(extractor.scan("https://example.com", null).isEmpty());
        assertTrue(extractor.scan("https://example.com", "").isEmpty());
    }

    @Test
    void extractsApiPath() {
        String body = "fetch(\"/api/v2/users/1024\");";

        List<Finding> found = extractor.scan("https://example.com/app.js", body);

        assertEquals(1, found.size());
        Finding f = found.get(0);
        assertEquals("API Endpoint", f.type());
        assertEquals("Discovery", f.category());
        assertTrue(f.value().contains("/api/v2/users/1024"));
    }

    @Test
    void extractsS3Url() {
        String body = "const url = \"https://demo-assets.s3.amazonaws.com/private/dump.json\";";

        List<Finding> found = extractor.scan("https://example.com/app.js", body);

        assertEquals(1, found.size());
        assertEquals("Cloud Storage URL", found.get(0).type());
        assertTrue(found.get(0).value().contains("s3.amazonaws.com"));
    }

    @Test
    void extractsSourceMapReference() {
        String body = "//# sourceMappingURL=app.js.map\n";

        List<Finding> found = extractor.scan("https://example.com/app.js", body);

        assertEquals(1, found.size());
        assertEquals("Source Map Reference", found.get(0).type());
        assertEquals("app.js.map", found.get(0).value());
    }

    @Test
    void extractsMultipleCategories() {
        String body = "fetch(\"/api/v1/payment\");\n"
                + "const u = \"https://my-bucket.s3.amazonaws.com/data\";\n"
                + "//# sourceMappingURL=bundle.js.map\n";

        List<Finding> found = extractor.scan("https://example.com/app.js", body);

        assertEquals(3, found.size());
        assertTrue(found.stream().anyMatch(f -> f.type().equals("API Endpoint")));
        assertTrue(found.stream().anyMatch(f -> f.type().equals("Cloud Storage URL")));
        assertTrue(found.stream().anyMatch(f -> f.type().equals("Source Map Reference")));
    }

    @Test
    void duplicatesWithinCategoryAreDeduplicated() {
        String body = "fetch(\"/api/v1/payment\"); fetch(\"/api/v1/payment\");";

        List<Finding> found = extractor.scan("https://example.com/app.js", body);

        assertEquals(1, found.size());
        assertNotNull(found.get(0).value());
    }

    @Test
    void findingsCarryImpactAndRemediation() {
        List<Finding> found = extractor.scan("https://example.com/app.js",
                "fetch(\"/api/v2/users\");");

        assertTrue(found.get(0).impactSummary().length() > 20);
        assertTrue(found.get(0).remediation().length() > 5);
    }
}