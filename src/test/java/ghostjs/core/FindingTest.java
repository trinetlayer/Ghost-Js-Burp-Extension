package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FindingTest {

    private static Finding makeFinding(String value) {
        return new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                value, "https://example.com/app.js", 12,
                "ctx", "impact", "remediation");
    }

    @Test
    void shortValueIsMaskedAsFirstCharPlusStars() {
        assertEquals("a***", makeFinding("abc").maskedValue());
    }

    @Test
    void longValueShowsFirstFourLastFourAndLength() {
        String value = "AKIA1234567890ABCDEF";
        assertEquals("AKIA…CDEF (20 chars)", makeFinding(value).maskedValue());
    }

    @Test
    void nullValueMasksToEmpty() {
        assertEquals("", makeFinding(null).maskedValue());
    }

    @Test
    void dedupeKeyCombinesTypeValueAndUrl() {
        Finding a = makeFinding("AKIA0001");
        Finding b = makeFinding("AKIA0001");
        Finding c = new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                "AKIA0001", "https://other.com/app.js", 12,
                "ctx", "impact", "remediation");

        assertEquals(a.dedupeKey(), b.dedupeKey());
        assertNotEquals(a.dedupeKey(), c.dedupeKey());
    }
}