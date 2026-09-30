package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntropyTest {

    // shannon

    @Test
    void shannonEmpty() {
        assertTrue(Entropy.shannon("") == 0.0);
        assertTrue(Entropy.shannon(null) == 0.0);
    }

    @Test
    void shannonRepeat() {
        assertTrue(Entropy.shannon("aaaaaaaaaa") == 0.0);
    }

    @Test
    void shannonMixed() {
        assertTrue(Entropy.shannon("AKIA1234567890ABCDEF") > 3.0);
    }

    // isLikelyFalsePositive

    @Test
    void shortOrNullSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(null, "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("abc", "Anything", false));
    }

    @Test
    void awsDocSampleSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("AKIAIOSFODNN7EXAMPLE", "AWS Access Key", false));
    }

    @Test
    void awsSecretDocSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "AWS Secret Access Key", false));
    }

    @Test
    void stripePubSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "pk_live_51AbCdEfGhIjKlMnOpQrStUv", "Stripe Live Secret Key", false));
    }

    @Test
    void klaviyoKept() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "pk_abcdef0123456789abcdef0123456789ab", "Klaviyo Private Key", false));
    }

    @Test
    void captchaSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "6LabcdefghijklmnopqrstuvwxyzABCDEFGHIJKL", "Anything", false));
    }

    @Test
    void hex32Suppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "0123456789abcdef0123456789abcdef", "Generic Token", false));
    }

    @Test
    void hex32KeptForDatadog() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "0123456789abcdef0123456789abcdef", "Datadog API Key", false));
    }

    @Test
    void zerosSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("00000000000000000000", "Anything", false));
    }

    @Test
    void placeholdersSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("your_api_key_here", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("changeme12345", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("replace_with_real_value", "Anything", false));
    }

    @Test
    void templateVarsSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("${API_KEY}", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("{{ token }}", "Anything", false));
    }

    @Test
    void lowVarietySuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive("aaaaaaaaaaaa", "Anything", false));
        assertTrue(Entropy.isLikelyFalsePositive("aabbbaaabbb", "Anything", false));
    }

    @Test
    void naturalLangSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "please enter your password here", "Anything", false));
    }

    @Test
    void pathSuppressed() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "/api/v2/users/123/profile", "Anything", false));
    }

    @Test
    void needsEntropyRejectsSpace() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "value with spaces here 1234567890", "Generic API Key", true));
    }

    @Test
    void needsEntropyRejectsLowEntropy() {
        assertTrue(Entropy.isLikelyFalsePositive(
                "aaaaaaaaaa1234567890", "Generic API Key", true));
    }

    @Test
    void realAwsPasses() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "AKIA1234567890ABCDEF", "AWS Access Key", false));
    }

    @Test
    void realGithubPasses() {
        assertFalse(Entropy.isLikelyFalsePositive(
                "ghp_1234567890abcdefghijklmnopqrstuvwxyz12", "GitHub Personal Access Token", false));
    }
}