package ghostjs.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GhostConfigTest {

    @Test
    void defaultsAreSensible() {
        GhostConfig c = new GhostConfig();
        assertEquals(true, c.scanEnabled);
        assertEquals(true, c.activeFetchEnabled);
        assertEquals(true, c.respectScope);
        assertEquals(true, c.scanHtmlBodies);
        assertEquals(true, c.scanJsonBodies);
        assertEquals(true, c.highlightProxy);
        assertEquals(120_000, c.inlineScanLimit);
    }

    @Test
    void fieldsAreMutable() {
        GhostConfig c = new GhostConfig();
        c.scanEnabled = false;
        c.inlineScanLimit = 999;
        assertEquals(false, c.scanEnabled);
        assertEquals(999, c.inlineScanLimit);
    }
}