package ghostjs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeverityTest {

    @Test
    void criticalIsFirst() {
        assertEquals(0, Severity.rank("critical"));
    }

    @Test
    void orderDesc() {
        assertTrue(Severity.rank("critical") < Severity.rank("high"));
        assertTrue(Severity.rank("high") < Severity.rank("medium"));
        assertTrue(Severity.rank("medium") < Severity.rank("low"));
        assertTrue(Severity.rank("low") < Severity.rank("info"));
    }

    @Test
    void caseInsensitive() {
        assertEquals(Severity.rank("critical"), Severity.rank("CRITICAL"));
    }

    @Test
    void unknownLast() {
        assertEquals(Severity.ORDER.length, Severity.rank("nonsense"));
    }

    @Test
    void nullLast() {
        assertEquals(Severity.ORDER.length, Severity.rank(null));
    }
}