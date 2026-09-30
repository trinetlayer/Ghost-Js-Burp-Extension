package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RawPatternTest {

    @Test
    void fieldsExposed() {
        RawPattern p = new RawPattern(
                "Test", "Category", "AKIA[0-9]{16}", "g",
                "critical", 95, "impact", "remediation");

        assertEquals("Test", p.name());
        assertEquals("Category", p.category());
        assertEquals("AKIA[0-9]{16}", p.source());
        assertEquals("g", p.flags());
        assertEquals("critical", p.severity());
        assertEquals(95, p.confidence());
        assertNotNull(p.impactSummary());
        assertNotNull(p.remediation());
    }
}