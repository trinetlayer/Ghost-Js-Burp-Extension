package ghostjs.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretScannerTest {

    @Test
    void compilesAll() {
        SecretScanner s = new SecretScanner();
        assertTrue(s.patternCount() > 100);
        assertTrue(s.compileFailures().isEmpty(), "failures: " + s.compileFailures());
    }

    @Test
    void awsFound() {
        List<Finding> f = new SecretScanner().scan("u",
                "const AWS_ACCESS_KEY = \"AKIA1234567890ABCDEF\";");
        assertTrue(f.stream().anyMatch(x -> x.type().equals("AWS Access Key")));
    }

    @Test
    void stripeFound() {
        List<Finding> f = new SecretScanner().scan("u",
                "const k = \"sk_live_abcdEFGH1234567890ijklMNOP\";");
        assertTrue(f.stream().anyMatch(x -> x.type().equals("Stripe Live Secret Key")));
    }

    @Test
    void githubFound() {
        List<Finding> f = new SecretScanner().scan("u",
                "const t = \"ghp_1234567890abcdefghijklmnopqrstuvwxyz12\";");
        assertTrue(f.stream().anyMatch(x -> x.type().equals("GitHub Personal Access Token")));
    }

    @Test
    void firebaseKeyFound() {
        List<Finding> f = new SecretScanner().scan("u",
                "const k = \"-----BEGIN PRIVATE KEY-----\\nMIIE\\n-----END PRIVATE KEY-----\";");
        assertTrue(f.stream().anyMatch(x -> x.type().equals("Firebase Private Key")));
    }

    @Test
    void awsDocSuppressed() {
        List<Finding> f = new SecretScanner().scan("u", "const s = \"AKIAIOSFODNN7EXAMPLE\";");
        assertTrue(f.stream().noneMatch(x -> x.type().equals("AWS Access Key")));
    }

    @Test
    void stripePubSuppressed() {
        List<Finding> f = new SecretScanner().scan("u", "const k = \"pk_live_51AbCdEfGhIjKlMnOpQrStUv\";");
        assertTrue(f.stream().noneMatch(x -> x.type().contains("Stripe")));
    }

    @Test
    void mixedBody() {
        List<Finding> f = new SecretScanner().scan("u", """
                const AWS_ACCESS_KEY = "AKIA1234567890ABCDEF";
                const sample = "AKIAIOSFODNN7EXAMPLE";
                const pk = "pk_live_51AbCdEfGhIjKlMnOpQrStUv";
                """);
        assertTrue(f.stream().anyMatch(x -> x.type().equals("AWS Access Key")));
        assertTrue(f.stream().noneMatch(x -> x.type().contains("Stripe")));
    }

    @Test
    void nullEmptyBody() {
        SecretScanner s = new SecretScanner();
        assertTrue(s.scan("u", null).isEmpty());
        assertTrue(s.scan("u", "").isEmpty());
    }

    @Test
    void weirdInput() {
        List<Finding> f = new SecretScanner().scan("u",
                "\u0000\u0001var x = \"AKIA1234567890ABCDEF\u0000\";");
        assertNotNull(f);
        assertTrue(f.stream().anyMatch(x -> x.type().equals("AWS Access Key")));
    }

    @Test
    void oversized() {
        SecretScanner s = new SecretScanner(200, 100);
        assertNotNull(s.scan("u", "x".repeat(10_000)));
    }

    @Test
    void lineNumber() {
        Finding aws = new SecretScanner().scan("u", """
                var a = 1;
                var b = 2;
                const k = "AKIA1234567890ABCDEF";
                """).stream()
                .filter(x -> x.type().equals("AWS Access Key"))
                .findFirst().orElseThrow();
        assertEquals(3, aws.lineNumber());
    }

    @Test
    void snippet() {
        Finding aws = new SecretScanner().scan("u",
                "const k = \"AKIA1234567890ABCDEF\";").stream()
                .filter(x -> x.type().equals("AWS Access Key"))
                .findFirst().orElseThrow();
        assertFalse(aws.snippet().isBlank());
    }

    @Test
    void impactAndRemediation() {
        Finding aws = new SecretScanner().scan("u", "const k = \"AKIA1234567890ABCDEF\";").stream()
                .filter(x -> x.type().equals("AWS Access Key"))
                .findFirst().orElseThrow();
        assertTrue(aws.impactSummary().length() > 50);
        assertTrue(aws.remediation().length() > 20);
    }

    @Test
    void dedupeViaStore() {
        SecretScanner s = new SecretScanner();
        FindingStore store = new FindingStore();
        String body = "const k = \"AKIA1234567890ABCDEF\";";
        store.addAll(s.scan("u", body));
        store.addAll(s.scan("u", body));
        assertEquals(1, store.size());
    }
}