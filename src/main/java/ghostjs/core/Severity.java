package ghostjs.core;

/** Severity ordering helpers (highest first). */
public final class Severity {
    private Severity() {}

    public static final String[] ORDER = {"critical", "high", "medium", "low", "info"};

    /** Lower rank = more severe. Unknown severities sort last. */
    public static int rank(String severity) {
        if (severity == null) return ORDER.length;
        String s = severity.toLowerCase();
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i].equals(s)) return i;
        }
        return ORDER.length;
    }
}
