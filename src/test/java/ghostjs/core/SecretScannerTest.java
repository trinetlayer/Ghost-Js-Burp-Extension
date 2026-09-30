package ghostjs.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretScannerTest {

    @Test
    void compilesAllShippedPatterns() {
        SecretScanner scanner = new SecretScanner();
        assertTrue(scanner.patternCount() > 100, "expected 100+ patterns");
        assertTrue(scanner.compileFailures().isEmpty(),
                "compile failures: " + scanner.compileFailures());
    }

    @Test
    void detectsAwsAccessKey() {
        SecretScanner scanner = new SecretScanner();
        String body = "const AWS_ACCESS_KEY = \"AKIA1234567890ABCDEF\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().anyMatch(f -> f.type().equals("AWS Access Key")),
                "expected AWS Access Key finding");
    }

    @Test
    void detectsStripeLiveKey() {
        SecretScanner scanner = new SecretScanner();
        String body = "const stripeKey = \"sk_live_abcdEFGH1234567890ijklMNOP\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().anyMatch(f -> f.type().equals("Stripe Live Secret Key")));
    }

    @Test
    void detectsGitHubPat() {
        SecretScanner scanner = new SecretScanner();
        String body = "const token = \"ghp_1234567890abcdefghijklmnopqrstuvwxyz12\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().anyMatch(f -> f.type().equals("GitHub Personal Access Token")));
    }

    @Test
    void detectsFirebasePrivateKey() {
        SecretScanner scanner = new SecretScanner();
        String body = "const k = \"-----BEGIN PRIVATE KEY-----\\nMIIEv...\\n-----END PRIVATE KEY-----\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().anyMatch(f -> f.type().equals("Firebase Private Key")));
    }

    @Test
    void suppressAwsDocSample() {
        SecretScanner scanner = new SecretScanner();
        String body = "const sample = \"AKIAIOSFODNN7EXAMPLE\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().noneMatch(f -> f.type().equals("AWS Access Key")),
                "AWS doc sample must be suppressed");
    }

    @Test
    void suppressStripePublishableKey() {
        SecretScanner scanner = new SecretScanner();
        String body = "const k = \"pk_live_51AbCdEfGhIjKlMnOpQrStUv\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().noneMatch(f -> f.type().contains("Stripe")),
                "publishable key must be suppressed");
    }

    @Test
    void mixedBodyYieldsRealFindingsOnly() {
        SecretScanner scanner = new SecretScanner();
        String body = """
                // real secret
                const AWS_ACCESS_KEY = "AKIA1234567890ABCDEF";
                // doc sample (must be suppressed)
                const sample = "AKIAIOSFODNN7EXAMPLE";
                // publishable (must be suppressed)
                const pk = "pk_live_51AbCdEfGhIjKlMnOpQrStUv";
                """;

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        assertTrue(found.stream().anyMatch(f -> f.type().equals("AWS Access Key")));
        assertTrue(found.stream().noneMatch(f -> f.type().equals("AWS Secret Access Key")));
        assertTrue(found.stream().noneMatch(f -> f.type().contains("Stripe")),
                "pk_live must not match any Stripe pattern");
    }

    @Test
    void nullAndEmptyBodyReturnEmpty() {
        SecretScanner scanner = new SecretScanner();
        assertTrue(scanner.scan("https://example.com/app.js", null).isEmpty());
        assertTrue(scanner.scan("https://example.com/app.js", "").isEmpty());
    }

    @Test
    void malformedInputDoesNotThrow() {
        SecretScanner scanner = new SecretScanner();
        // mix of unterminated strings, control chars, NULs
        String weird = "\u0000\u0001var x = \"AKIA1234567890ABCDEF\\u0000\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", weird);

        assertNotNull(found);
        assertTrue(found.stream().anyMatch(f -> f.type().equals("AWS Access Key")));
    }

    @Test
    void oversizedBodyIsTruncated() {
        SecretScanner scanner = new SecretScanner(200, 100);
        String body = "x".repeat(10_000);

        // scanner must not throw on bodies exceeding maxBodyChars
        List<Finding> found = scanner.scan("https://example.com/app.js", body);
        assertNotNull(found);
    }

    @Test
    void findingCarriesLineNumber() {
        SecretScanner scanner = new SecretScanner();
        String body = "var a = 1;\nvar b = 2;\nconst k = \"AKIA1234567890ABCDEF\";";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        Finding aws = found.stream()
                .filter(f -> f.type().equals("AWS Access Key"))
                .findFirst()
                .orElseThrow();
        assertEquals(3, aws.lineNumber());
    }

    @Test
    void findingCarriesSnippet() {
        SecretScanner scanner = new SecretScanner();
        String body = "const AWS_ACCESS_KEY = \"AKIA1234567890ABCDEF\"; // the key";

        List<Finding> found = scanner.scan("https://example.com/app.js", body);

        Finding aws = found.stream()
                .filter(f -> f.type().equals("AWS Access Key"))
                .findFirst()
                .orElseThrow();
        assertNotNull(aws.snippet());
        assertFalse(aws.snippet().isBlank());
        assertTrue(aws.snippet().contains("AKIA") || aws.snippet().length() > 10);
    }

    @Test
    void findingCarriesImpactAndRemediation() {
        SecretScanner scanner = new SecretScanner();
        String body = "const k = \"AKIA1234567890ABCDEF\";";

        Finding aws = scanner.scan("https://example.com/app.js", body).stream()
                .filter(f -> f.type().equals("AWS Access Key"))
                .findFirst()
                .orElseThrow();

        assertTrue(aws.impactSummary().length() > 50);
        assertTrue(aws.remediation().length() > 20);
    }

    @Test
    void sameSecretAppearsOnceWhenDedupedByStore() {
        SecretScanner scanner = new SecretScanner();
        FindingStore store = new FindingStore();

        String body = "const k = \"AKIA1234567890ABCDEF\";";
        store.addAll(scanner.scan("https://example.com/app.js", body));
        store.addAll(scanner.scan("https://example.com/app.js", body));

        assertEquals(1, store.size());
    }
}