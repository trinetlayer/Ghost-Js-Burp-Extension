package ghostjs.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe, de-duplicating store of findings shared between the HTTP handlers
 * (producers) and the Swing UI (consumer). Backed by a lock-guarded ArrayList so
 * appends are amortised O(1) even under heavy scanning.
 */
public final class FindingStore {

    public interface Listener {
        void onFindingsChanged();
    }

    private final Object lock = new Object();
    private final List<Finding> findings = new ArrayList<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    /** Adds new (non-duplicate) findings; returns how many were actually added. */
    public int addAll(List<Finding> batch) {
        int added = 0;
        synchronized (lock) {
            for (Finding f : batch) {
                if (seen.add(f.dedupeKey())) {
                    findings.add(f);
                    added++;
                }
            }
        }
        if (added > 0) notifyListeners();
        return added;
    }

    /** Snapshot sorted by severity (most severe first), then confidence desc. */
    public List<Finding> snapshot() {
        List<Finding> copy;
        synchronized (lock) {
            copy = new ArrayList<>(findings);
        }
        copy.sort(Comparator
                .comparingInt((Finding f) -> Severity.rank(f.severity()))
                .thenComparing(Comparator.comparingInt(Finding::confidence).reversed()));
        return copy;
    }

    public int size() {
        synchronized (lock) {
            return findings.size();
        }
    }

    public void clear() {
        synchronized (lock) {
            findings.clear();
            seen.clear();
        }
        notifyListeners();
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    private void notifyListeners() {
        for (Listener l : listeners) l.onFindingsChanged();
    }
}
