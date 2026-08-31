package ghostjs.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shannon-entropy and false-positive heuristics — a faithful port of the core
 * suppression logic in the GhostJS TypeScript validation layer (validation.ts +
 * the known-example / public-by-design lists in secret-patterns.ts). Suppresses
 * documented sample credentials, public-by-design keys, placeholder/template
 * text, UI/natural-language strings, and low-entropy junk.
 */
public final class Entropy {
    private Entropy() {}

    /** Shannon entropy (bits per character) of a string. */
    public static double shannon(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        Map<Character, Integer> freq = new HashMap<>();
        for (int i = 0; i < s.length(); i++) freq.merge(s.charAt(i), 1, Integer::sum);
        double entropy = 0.0;
        int len = s.length();
        for (int count : freq.values()) {
            double p = (double) count / len;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    // ---- Documented sample credentials that appear in tutorials/READMEs ----
    private static final Set<String> KNOWN_EXAMPLE_VALUES = Set.of(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            "ASIAIOSFODNN7EXAMPLE",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6"
                    + "IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

    private static final String[] KNOWN_EXAMPLE_FRAGMENTS = {"EXAMPLEKEY", "AKIAIOSFODNN7EXAMPLE"};

    // ---- Keys that are public by design (safe to ship client-side) ----
    private static final Pattern[] PUBLIC_BY_DESIGN = {
            Pattern.compile("^pk_(?:live|test)_[A-Za-z0-9]{8,}$"),
            Pattern.compile("^6L[0-9A-Za-z_-]{38}$"),
            Pattern.compile("^p(?:k|ub)_[A-Za-z0-9]{8,}$")
    };

    // ---- Generic FP shapes (all matched case-insensitively) ----
    private static final Pattern[] COMMON_FP = {
            Pattern.compile("^[0-9a-f]{32}$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^0{20,}$"),
            Pattern.compile("^1{20,}$"),
            Pattern.compile("^a{20,}$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^x{10,}$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(abc|123|test|demo|example|sample|default|changeme|password|secret|admin)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(undefined|null|none|empty|todo|fixme|placeholder|insert|your)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("^[A-Za-z]+_[A-Za-z]+_[A-Za-z]+$"),
            Pattern.compile("^[a-z]{2,}(?:-[a-z]{2,}){1,}$"),
            Pattern.compile("^https?://localhost", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^https?://127\\.0\\.0\\.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^https?://0\\.0\\.0\\.0", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^[a-z]+=[\\da-f]{32}$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(session|cookie|token|csrf|nonce|hash|sid|id)=", Pattern.CASE_INSENSITIVE)
    };
    private static final String HEX32_SOURCE = "^[0-9a-f]{32}$";

    // Names whose real format legitimately IS 32 hex chars — exempt from the hex32 FP rule.
    private static final Set<String> HEX32_FP_EXEMPT_NAMES = Set.of(
            "Subscription / Access Key", "Algolia Admin API Key", "Twilio Auth Token",
            "Datadog API Key", "Facebook App Secret", "Agora App Certificate",
            "Rollbar Access Token", "Bugsnag API Key", "Fastly API Token", "Plaid Client Secret");

    private static final String[] PLACEHOLDER_MARKERS = {
            "example", "sample", "placeholder", "your_", "yourkey", "your-key",
            "changeme", "change_me", "redacted", "dummy", "test_key", "testkey",
            "lorem", "foobar", "insert_your", "replace_with", "somekey", "fakekey",
            "notreal", "<your", "enter_your", "xxxxx"
    };

    private static final Pattern SLASH_PATH =
            Pattern.compile("^/[A-Za-z0-9._~-]+(?:/[A-Za-z0-9._~-]+)+$");

    private static final Set<String> COMMON_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being", "have", "has",
            "had", "do", "does", "did", "will", "would", "could", "should", "may", "might",
            "shall", "can", "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "please", "enter", "type", "input", "provide", "must", "field",
            "required", "your", "my", "this", "that", "these", "those", "it", "you", "we",
            "and", "or", "if", "not", "no", "all", "each", "some", "click", "here", "select");

    /**
     * Faithful FP check. {@code secretName} enables name-aware exemptions;
     * {@code needsEntropy} tightens checks for generic capture patterns.
     */
    public static boolean isLikelyFalsePositive(String value, String secretName, boolean needsEntropy) {
        if (value == null) return true;
        String v = value.strip();
        if (v.length() < 6) return true;

        // Documented sample credentials.
        if (KNOWN_EXAMPLE_VALUES.contains(v)) return true;
        for (String frag : KNOWN_EXAMPLE_FRAGMENTS) if (v.contains(frag)) return true;

        // Public-by-design keys (Klaviyo private key uses pk_ but is a real secret).
        if (!"Klaviyo Private Key".equals(secretName)) {
            for (Pattern p : PUBLIC_BY_DESIGN) if (p.matcher(v).find()) return true;
        }

        // Generic FP shapes, with the hex32 exemption for names that really use hex32.
        boolean hex32Exempt = secretName != null && HEX32_FP_EXEMPT_NAMES.contains(secretName);
        for (Pattern p : COMMON_FP) {
            if (hex32Exempt && p.pattern().equals(HEX32_SOURCE)) continue;
            if (p.matcher(v).find()) return true;
        }

        String lower = v.toLowerCase();
        if (lower.contains("${") || lower.contains("{{") || lower.contains("%s")
                || lower.contains("process.env") || lower.contains("import.meta")) return true;
        if (v.matches(".*<[a-zA-Z_]+>.*")) return true;
        for (String marker : PLACEHOLDER_MARKERS) if (lower.contains(marker)) return true;

        // Low-variety / low-entropy junk (universal thresholds from validation.ts).
        if (v.length() > 10 && v.chars().distinct().count() <= 3) return true;
        if (v.length() >= 16 && shannon(v) < 2.0) return true;

        // Multi-word UI / natural-language text.
        if (isUiOrNaturalLanguage(v)) return true;

        // A slash-rooted identifier path is an endpoint, not a credential.
        if (SLASH_PATH.matcher(v).find()) return true;

        // Extra tightening for generic capture patterns.
        if (needsEntropy) {
            if (v.matches(".*\\s.*")) return true;
            String core = stripAffixes(v);
            if (core.length() >= 12 && shannon(core) < 3.0) return true;
        }
        return false;
    }

    private static boolean isUiOrNaturalLanguage(String v) {
        String[] words = v.strip().split("\\s+");
        if (words.length < 4) return false;
        int common = 0;
        for (String w : words) if (COMMON_WORDS.contains(w.toLowerCase())) common++;
        return (double) common / words.length >= 0.4;
    }

    private static String stripAffixes(String v) {
        String s = v.replaceAll("^[\"'`]+|[\"'`]+$", "");
        int us = s.lastIndexOf('_');
        if (us >= 0 && us < s.length() - 8) s = s.substring(us + 1);
        return s;
    }
}
