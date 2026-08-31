package ghostjs;

/** Central place for all TrinetLayer / GhostJS branding strings. */
public final class Branding {
    private Branding() {}

    public static final String VENDOR = "TrinetLayer";
    public static final String PRODUCT = "GhostJS";
    public static final String VERSION = "1.0.0";
    public static final String TAGLINE = "JS Secret & Endpoint Scanner";

    /** Name shown in Burp's Extensions list. */
    public static final String EXTENSION_NAME =
            PRODUCT + " by " + VENDOR + " — " + TAGLINE;

    /** Short title for the suite tab. */
    public static final String TAB_TITLE = PRODUCT;

    /** Prefix for every log line. */
    public static final String LOG_PREFIX = "[" + PRODUCT + " · " + VENDOR + "] ";

    /** ASCII banner printed to the output pane on load. */
    public static final String BANNER = String.join("\n",
            "",
            "   ▄████  ██   ██  ▄███▄  ▄███▄ ██████  ██  ▄███▄",
            "  ██      ██   ██ ██   ██ ██     ██  ██ ██ ██        " + PRODUCT + " v" + VERSION,
            "  ██ ▄██  ███████ ██   ██ ▀███▄  ██  ██ ██ ▀███▄     " + TAGLINE,
            "  ██  ██  ██   ██ ██   ██    ██  ██  ██ ██    ██     by " + VENDOR,
            "   ▀███▀  ██   ██  ▀███▀  ▀███▀ ██████  ██ ▀███▀",
            "");
}
