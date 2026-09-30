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

    private static Finding mk(String value) {
        return new Finding("AWS Access Key", "Cloud Secrets", "critical", 95,
                value, "https://example.com/app.js", 1,
                "ctx", "impact", "remediation");
    }

    @Test
    void dedupe() {
        FindingStore s = new FindingStore();
        int added = s.addAll(List.of(mk("AKIA1111"), mk("AKIA1111"), mk("AKIA2222")));
        assertEquals(2, added);
        assertEquals(2, s.size());
    }

    @Test
    void sortOrder() {
        FindingStore s = new FindingStore();
        s.addAll(List.of(
                new Finding("X", "Cat", "medium", 80, "v3", "u", 1, "c", "i", "r"),
                new Finding("Y", "Cat", "critical", 50, "v1", "u", 1, "c", "i", "r"),
                new Finding("Z", "Cat", "critical", 99, "v2", "u", 1, "c", "i", "r")));

        List<Finding> snap = s.snapshot();
        assertEquals("critical", snap.get(0).severity());
        assertEquals(99, snap.get(0).confidence());
        assertEquals(50, snap.get(1).confidence());
        assertEquals("medium", snap.get(2).severity());
    }

    @Test
    void clearWorks() {
        FindingStore s = new FindingStore();
        s.addAll(List.of(mk("AKIA1111")));
        s.clear();
        assertEquals(0, s.size());
        assertEquals(1, s.addAll(List.of(mk("AKIA1111"))));
    }

    @Test
    void listenerOncePerAdd() {
        FindingStore s = new FindingStore();
        List<Integer> fires = new ArrayList<>();
        s.addListener(() -> fires.add(s.size()));

        s.addAll(List.of(mk("AKIA1111")));
        s.addAll(List.of());
        s.addAll(List.of(mk("AKIA1111")));

        assertEquals(List.of(1), fires);
    }

    @Test
    void threadSafe() throws Exception {
        FindingStore s = new FindingStore();
        int threads = 8, per = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        for (int t = 0; t < threads; t++) {
            int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    List<Finding> b = new ArrayList<>();
                    for (int i = 0; i < per; i++) b.add(mk("AKIA" + tid + "-" + i));
                    s.addAll(b);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(threads * per, s.size());
    }
}