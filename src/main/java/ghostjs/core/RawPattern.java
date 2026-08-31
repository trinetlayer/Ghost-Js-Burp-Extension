package ghostjs.core;

/**
 * Raw (uncompiled) pattern definition as exported from the GhostJS TypeScript
 * engine. {@code source} + {@code flags} mirror a JavaScript RegExp; they are
 * compiled into a {@link java.util.regex.Pattern} by {@link SecretScanner}.
 */
public record RawPattern(
        String name,
        String category,
        String source,
        String flags,
        String severity,
        int confidence,
        String impactSummary,
        String remediation) {
}
