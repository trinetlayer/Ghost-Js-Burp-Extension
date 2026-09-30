package ghostjs.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindingStoreTest {

    private static Finding finding(String value) {
        return new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                value, "https://example.com/app.js", 1,
                "ctx", "impact", "remediation");
    }

    @Test
    void duplicatesAreCollapsed() {
        FindingStore store = new FindingStore();
        List<Finding> batch = List.of(finding("AKIA1111"), finding("AKIA1111"), finding("AKIA2222"));

        int added = store.addAll(batch);

        assertEquals(2, added);
        assertEquals(2, store.size());
    }

    @Test
    void snapshotSortsBySeverityThenConfidenceDescending() {
        FindingStore store = new FindingStore();
        Finding lowConfCrit = new Finding("X", "Cat", "critical", 50, "v1", "u", 1, "c", "i", "r");
        Finding highConfCrit = new Finding("Y", "Cat", "critical", 99, "v2", "u", 1, "c", "i", "r");
        Finding medium = new Finding("Z", "Cat", "medium", 80, "v3", "u", 1, "c", "i", "r");
        store.addAll(List.of(medium, lowConfCrit, highConfCrit));

        List<Finding> snap = store.snapshot();

        assertEquals("critical", snap.get(0).severity());
        assertEquals(99, snap.get(0).confidence());
        assertEquals(50, snap.get(1).confidence());
        assertEquals("medium", snap.get(2).severity());
    }

    @Test
    void clearEmptiesFindingsAndDedupSet() {
        FindingStore store = new FindingStore();
        store.addAll(List.of(finding("AKIA1111")));

        store.clear();

        assertEquals(0, store.size());
        // re-adding the same value after clear should not be considered a dup
        assertEquals(1, store.addAll(List.of(finding("AKIA1111"))));
    }

    @Test
    void listenerFiresOncePerNonEmptyAdd() {
        FindingStore store = new FindingStore();
        List<Integer> fires = new ArrayList<>();
        store.addListener(() -> fires.add(store.size()));

        store.addAll(List.of(finding("AKIA1111")));   // fires once
        store.addAll(List.of());                       // no-op, no fire
        store.addAll(List.of(finding("AKIA1111")));   // dup, no fire

        assertEquals(List.of(1), fires);
    }

    @Test
    void concurrentAddsAreSafe() throws Exception {
        FindingStore store = new FindingStore();
        int threads = 8;
        int perThread = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) {
            int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    List<Finding> batch = new ArrayList<>();
                    for (int i = 0; i < perThread; i++) {
                        batch.add(finding("AKIA" + tid + "-" + i));
                    }
                    store.addAll(batch);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "pool did not finish");

        assertEquals(threads * perThread, store.size());
    }
}