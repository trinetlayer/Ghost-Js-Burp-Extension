package ghostjs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiles the exported {@link GeneratedPatterns} into Java regex and scans
 * arbitrary text (typically JavaScript response bodies) for secrets.
 *
 * The scanner is stateless per-scan and thread-safe: {@link #scan} can be
 * called concurrently from multiple HTTP handler threads.
 */
public final class SecretScanner {

    /** A pattern that compiled successfully, paired with its metadata. */
    private record Compiled(RawPattern raw, Pattern pattern, boolean needsEntropy) {}

    private final List<Compiled> compiled = new ArrayList<>();
    private final List<String> compileFailures = new ArrayList<>();
    private final int maxBodyChars;
    private final long scanBudgetNanos;

    public SecretScanner() {
        this(5_000_000, 2_500);
    }

    public SecretScanner(int maxBodyChars, long scanBudgetMillis) {
        this.maxBodyChars = maxBodyChars;
        this.scanBudgetNanos = scanBudgetMillis * 1_000_000L;
        for (RawPattern raw : GeneratedPatterns.ALL) {
            try {
                Pattern p = Pattern.compile(raw.source(), translateFlags(raw.flags()));
                compiled.add(new Compiled(raw, p, needsEntropy(raw)));
            } catch (PatternSyntaxException e) {
                compileFailures.add(raw.name() + ": " + e.getMessage());
            }
        }
    }

    public int patternCount() {
        return compiled.size();
    }

    public List<String> compileFailures() {
        return List.copyOf(compileFailures);
    }

    /** Scan a resource body, returning all surviving findings (post FP filter). */
    public List<Finding> scan(String url, String body) {
        List<Finding> findings = new ArrayList<>();
        if (body == null || body.isEmpty()) return findings;
        String text = body.length() > maxBodyChars ? body.substring(0, maxBodyChars) : body;

        long deadline = System.nanoTime() + scanBudgetNanos;
        for (Compiled c : compiled) {
            if (System.nanoTime() > deadline) break;   // per-scan time budget guard
            try {
                Matcher m = c.pattern().matcher(text);
                int guard = 0;
                while (m.find() && guard < 500) {
                    guard++;
                    String value = extractValue(m);
                    if (value == null || value.isBlank()) continue;
                    if (Entropy.isLikelyFalsePositive(value, c.raw().name(), c.needsEntropy())) continue;

                    findings.add(new Finding(
                            c.raw().name(), c.raw().category(), c.raw().severity(), c.raw().confidence(),
                            value.strip(), url, lineNumber(text, m.start()),
                            snippet(text, m.start(), m.end()),
                            c.raw().impactSummary(), c.raw().remediation()));
                }
            } catch (RuntimeException | StackOverflowError e) {
                // One bad pattern (or a pathological body) must never break the scan.
            }
        }
        return findings;
    }

    /** Prefer the first non-null capture group (the actual secret) over the whole match. */
    private static String extractValue(Matcher m) {
        for (int g = 1; g <= m.groupCount(); g++) {
            String grp = m.group(g);
            if (grp != null && !grp.isBlank()) return grp;
        }
        return m.group(0);
    }

    private static int lineNumber(String text, int index) {
        int line = 1;
        int limit = Math.min(index, text.length());
        for (int i = 0; i < limit; i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 40);
        int to = Math.min(text.length(), end + 40);
        String s = text.substring(from, to).replaceAll("\\s+", " ").strip();
        if (s.length() > 200) s = s.substring(0, 200) + "…";
        return s;
    }

    private static int translateFlags(String flags) {
        int f = 0;
        if (flags == null) return f;
        if (flags.contains("i")) f |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        if (flags.contains("m")) f |= Pattern.MULTILINE;
        if (flags.contains("s")) f |= Pattern.DOTALL;
        return f;
    }

    // Generic capture patterns (hardcoded tokens, loose API keys) are the ones
    // most prone to placeholder false positives, so gate them behind entropy.
    private static boolean needsEntropy(RawPattern raw) {
        String n = raw.name().toLowerCase();
        return n.contains("hardcoded") || n.contains("generic") || raw.confidence() <= 80;
    }
}
