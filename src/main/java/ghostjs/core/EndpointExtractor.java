package ghostjs.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attack-surface discovery inside JS bodies: API endpoints, cloud-storage URLs,
 * and source-map references. A lightweight companion to {@link SecretScanner};
 * emits {@link Finding}s under the "Discovery" category at low/info severity so
 * they never drown out real secrets.
 */
public final class EndpointExtractor {

    private static final Pattern API_PATH = Pattern.compile(
            "[\"'`](/(?:api|v\\d+|rest|graphql|gql|internal|admin|oauth|auth|user|users|account|payment|webhook)[A-Za-z0-9_\\-/.{}:]*)[\"'`]");

    private static final Pattern ABSOLUTE_URL = Pattern.compile(
            "https?://[A-Za-z0-9.\\-]+(?:/[A-Za-z0-9_\\-/.%?=&{}:]*)?");

    private static final Pattern CLOUD_STORAGE = Pattern.compile(
            "https?://[A-Za-z0-9.\\-]*(?:s3[.\\-][A-Za-z0-9.\\-]*amazonaws\\.com"
                    + "|[A-Za-z0-9.\\-]*\\.s3\\.amazonaws\\.com"
                    + "|storage\\.googleapis\\.com"
                    + "|[A-Za-z0-9.\\-]*\\.blob\\.core\\.windows\\.net"
                    + "|[A-Za-z0-9.\\-]*\\.r2\\.cloudflarestorage\\.com"
                    + "|[A-Za-z0-9.\\-]*\\.digitaloceanspaces\\.com)"
                    + "(?:/[A-Za-z0-9_\\-/.%]*)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SOURCE_MAP = Pattern.compile(
            "//[#@]\\s*sourceMappingURL=([^\\s'\"]+)");

    public List<Finding> scan(String url, String body) {
        List<Finding> out = new ArrayList<>();
        if (body == null || body.isEmpty()) return out;

        Set<String> cloud = collect(CLOUD_STORAGE, body, 0);
        for (String u : cloud) {
            out.add(new Finding("Cloud Storage URL", "Discovery", "low", 60,
                    u, url, 0, u,
                    "Reference to a cloud object-storage endpoint. Worth checking for public "
                            + "listing, unauthenticated read, or writable ACLs.",
                    "Verify the bucket/container is not publicly listable or writable."));
        }

        Set<String> maps = collect(SOURCE_MAP, body, 1);
        for (String u : maps) {
            out.add(new Finding("Source Map Reference", "Discovery", "low", 70,
                    u, url, 0, "sourceMappingURL=" + u,
                    "A source map can reconstruct original (pre-minification) source, "
                            + "including comments, internal paths, and sometimes secrets.",
                    "Do not ship .map files to production, or restrict access to them."));
        }

        Set<String> paths = collect(API_PATH, body, 1);
        for (String p : paths) {
            out.add(new Finding("API Endpoint", "Discovery", "info", 40,
                    p, url, 0, p,
                    "Internal/API path referenced from client JS. Useful for expanding the "
                            + "tested attack surface (IDOR, authz, hidden functionality).",
                    "N/A — informational. Test the endpoint for access-control issues."));
        }

        return out;
    }

    private static Set<String> collect(Pattern p, String body, int group) {
        Set<String> set = new LinkedHashSet<>();
        Matcher m = p.matcher(body);
        int guard = 0;
        while (m.find() && guard < 300) {
            guard++;
            String v = group == 0 ? m.group(0) : m.group(group);
            if (v != null && !v.isBlank()) set.add(v.strip());
        }
        return set;
    }
}
