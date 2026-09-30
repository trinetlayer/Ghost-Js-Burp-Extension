package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntropyTest {

    // --- shannon ---

    @Test
    void shannonIsZeroForEmpty() {
        assertTrue(Entropy.shannon("") == 0.0);
        assertTrue(Entropy.shannon(null) == 0.0);
    }

    @Test
    void shannonIsZeroForSingleRepeatedChar() {
        assertTrue(Entropy.shannon("aaaaaaaaaa") == 0.0);
    }

    @Test
    void shannonIsHighForMixedAlphanumeric() {
        assertTrue(Entropy.shannon("AKIA1234567890ABCDEF") > 3.0);
    }

    // --- isLikelyFalsePositive ---

    @Test
    void nullAndShortValuesAreSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(null, "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("abc", "Anything", false));
    }

    @Test
    void documentedAwsDocSampleIsSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("AKIAIOSFODNN7EXAMPLE", "AWS Access Key", false));
    }

    @Test
    void documentedAwsSecretSampleIsSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "AWS Secret Access Key", false));
    }

    @Test
    void stripePublishableKeyIsPublicByDesign() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "pk_live_51AbCdEfGhIjKlMnOpQrStUv", "Stripe Live Secret Key", false));
    }

    @Test
    void klaviyoPrivateKeyIsNotSuppressedDespitePkPrefix() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "pk_abcdef0123456789abcdef0123456789ab", "Klaviyo Private Key", false));
    }

    @Test
    void googleCaptchaSiteKeyIsPublicByDesign() {
        // 6L + 38 chars from [0-9A-Za-z_-] = 40 total
        assertTrue(Entropy.isLikelyFalsePositive(
                "6LabcdefghijklmnopqrstuvwxyzABCDEFGHIJKL", "Anything", false));
    }

    @Test
    void hex32IsSuppressedForUnknownNames() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "0123456789abcdef0123456789abcdef", "Generic Token", false));
    }

    @Test
    void hex32IsAllowedForKnownDatadog() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "0123456789abcdef0123456789abcdef", "Datadog API Key", false));
    }

    @Test
    void allZerosAreSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("00000000000000000000", "Anything", false));
    }

    @Test
    void placeholderMarkersAreSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("your_api_key_here", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("changeme12345", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("replace_with_real_value", "Anything", false));
    }

    @Test
    void templateVarsAreSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("${API_KEY}", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("{{ token }}", "Anything", false));
    }

    @Test
    void lowVarietyLongStringIsSuppressed() {
        // >10 chars, ≤3 distinct chars
        assertTrue(Entropy.isLikelyFalsePositive("aaaaaaaaaaaa", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("aabbbaaabbb", "Anything", false));
    }

    @Test
    void naturalLanguageIsSuppressed() {
        // 5+ words, ≥40% common words
        assertTrue(Entropy.isLikelyFalsePositive(
                "please enter your password here", "Anything", false));
    }

    @Test
    void pathLikeValueIsSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "/api/v2/users/123/profile", "Anything", false));
    }

    @Test
    void needsEntropyRejectsWhitespace() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "value with spaces here 1234567890", "Generic API Key", true));
    }

    @Test
    void needsEntropyRejectsLowEntropyCore() {
        // generic capture pattern + low-entropy core (after stripping trailing _)
        assertTrue(Entropy.isLikelyFalsePositive(
                "aaaaaaaaaa1234567890", "Generic API Key", true));
    }

    @Test
    void realisticAwsAccessKeyPasses() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "AKIA1234567890ABCDEF", "AWS Access Key", false));
    }

    @Test
    void realisticGitHubPatPasses() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "ghp_1234567890abcdefghijklmnopqrstuvwxyz12", "GitHub Personal Access Token", false));
    }
}