package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FindingTest {

    private static Finding mk(String value) {
        return new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                value, "https://example.com/app.js", 12,
                "ctx", "impact", "remediation");
    }

    @Test
    void shortMask() {
        assertEquals("a***", mk("abc").maskedValue());
    }

    @Test
    void longMask() {
        assertEquals("AKIA…CDEF (20 chars)", mk("AKIA1234567890ABCDEF").maskedValue());
    }

    @Test
    void nullMask() {
        assertEquals("", mk(null).maskedValue());
    }

    @Test
    void dedupeKeyShape() {
        Finding a = mk("AKIA0001");
        Finding b = mk("AKIA0001");
        Finding c = new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                "AKIA0001", "https://other.com/app.js", 12,
                "ctx", "impact", "remediation");

        assertEquals(a.dedupeKey(), b.dedupeKey());
        assertNotEquals(a.dedupeKey(), c.dedupeKey());
    }
}