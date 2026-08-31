package ghostjs.http;

import burp.api.montoya.http.message.MimeType;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Classifies responses as JavaScript / HTML and pulls JS URLs out of HTML. */
public final class JsDetector {
    private JsDetector() {}

    private static final Pattern SCRIPT_SRC =
            Pattern.compile("<script[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    // Require a path-like reference (absolute, root-relative, or ./ ../) so bare
    // words like "app.js" inside prose don't trigger spurious fetches.
    private static final Pattern JS_REF = Pattern.compile(
            "[\"']((?:https?://|\\.{0,2}/)[^\"'\\s]+?\\.m?js(?:\\?[^\"']*)?)[\"']",
            Pattern.CASE_INSENSITIVE);

    public static boolean isJavaScript(HttpResponse resp, String url) {
        MimeType mt = resp.mimeType();
        if (mt == MimeType.SCRIPT) return true;
        String ct = resp.headerValue("Content-Type");
        if (ct != null) {
            String c = ct.toLowerCase();
            if (c.contains("javascript") || c.contains("ecmascript")) return true;
        }
        String path = pathOf(url).toLowerCase();
        return path.endsWith(".js") || path.endsWith(".mjs");
    }

    public static boolean isHtml(HttpResponse resp) {
        if (resp.mimeType() == MimeType.HTML) return true;
        String ct = resp.headerValue("Content-Type");
        return ct != null && ct.toLowerCase().contains("text/html");
    }

    public static boolean isJson(HttpResponse resp, String url) {
        if (resp.mimeType() == MimeType.JSON) return true;
        String ct = resp.headerValue("Content-Type");
        if (ct != null && ct.toLowerCase().contains("json")) return true;
        return pathOf(url).toLowerCase().endsWith(".json");
    }

    /** Absolute JS URLs referenced by an HTML body, resolved against the page URL. */
    public static Set<String> extractJsUrls(String html, String pageUrl) {
        Set<String> out = new LinkedHashSet<>();
        collect(SCRIPT_SRC, html, pageUrl, out);
        collect(JS_REF, html, pageUrl, out);
        return out;
    }

    private static void collect(Pattern p, String html, String pageUrl, Set<String> out) {
        Matcher m = p.matcher(html);
        int guard = 0;
        while (m.find() && guard < 400) {
            guard++;
            String abs = resolve(pageUrl, m.group(1));
            if (abs != null && (abs.startsWith("http://") || abs.startsWith("https://"))) {
                out.add(abs);
            }
        }
    }

    private static String resolve(String base, String ref) {
        if (ref == null || ref.isBlank() || ref.startsWith("data:")) return null;
        try {
            return URI.create(base).resolve(ref.strip()).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String pathOf(String url) {
        try {
            String p = URI.create(url).getPath();
            return p == null ? "" : p;
        } catch (Exception e) {
            return url;
        }
    }
}
