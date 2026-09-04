package ghostjs.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Central palette/typography for the GhostJS tab, plus the one-time FlatLaf
 * installation that gives the whole extension (and, as a Swing side effect,
 * the rest of Burp) a modern flat look matching Burp's current light/dark
 * theme.
 */
public final class GhostTheme {
    private GhostTheme() {}

    private static volatile boolean installed = false;

    public static Color accent = new Color(0x2D, 0xC9, 0xA5);
    public static Color headerBg = new Color(0x14, 0x1B, 0x2E);
    public static Color textMuted = new Color(0x9A, 0xA6, 0xC0);
    public static Color surfaceBorder = new Color(0x00, 0x00, 0x00, 0x40);

    public static Color critical = new Color(0xE5, 0x3E, 0x3E);
    public static Color criticalFg = Color.WHITE;
    public static Color high = new Color(0xF0, 0x8A, 0x24);
    public static Color highFg = Color.WHITE;
    public static Color medium = new Color(0xE0, 0xB4, 0x00);
    public static Color mediumFg = Color.BLACK;
    public static Color low = new Color(0x4C, 0xAF, 0x50);
    public static Color lowFg = Color.WHITE;

    public static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    public static final Font FONT_SUBTLE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /**
     * Installs a FlatLaf look-and-feel matching Burp's current light/dark theme.
     * Safe to call multiple times; never throws — on any failure the extension
     * keeps running under Burp's stock look-and-feel.
     */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            Color base = UIManager.getColor("Panel.background");
            boolean dark = base != null && luminance(base) < 0.5;

            FlatLaf laf = dark ? new FlatDarkLaf() : new FlatLightLaf();
            UIManager.setLookAndFeel(laf);
            FlatLaf.updateUI();

            computePalette(dark);
        } catch (Throwable t) {
            // Keep Burp's stock look-and-feel; GhostTheme's field defaults
            // above still give the tab a reasonable fixed palette.
        }
    }

    private static void computePalette(boolean dark) {
        Color panelBg = UIManager.getColor("Panel.background");
        Color disabledFg = UIManager.getColor("Label.disabledForeground");
        Color borderColor = UIManager.getColor("Component.borderColor");

        accent = new Color(0x2D, 0xC9, 0xA5);
        headerBg = panelBg != null ? shift(panelBg, dark ? 14 : -10) : headerBg;
        textMuted = disabledFg != null ? disabledFg : textMuted;
        surfaceBorder = borderColor != null ? borderColor : surfaceBorder;

        // Saturated-but-readable pill fills, tuned to hold contrast on both
        // light and dark FlatLaf surfaces.
        critical = new Color(0xD9, 0x3B, 0x3B);
        criticalFg = Color.WHITE;
        high = new Color(0xE0, 0x82, 0x1F);
        highFg = Color.WHITE;
        medium = dark ? new Color(0xD1, 0xA6, 0x00) : new Color(0xE0, 0xB4, 0x00);
        mediumFg = Color.BLACK;
        low = new Color(0x3F, 0xA8, 0x54);
        lowFg = Color.WHITE;
    }

    private static Color shift(Color c, int delta) {
        int r = clamp(c.getRed() + delta);
        int g = clamp(c.getGreen() + delta);
        int b = clamp(c.getBlue() + delta);
        return new Color(r, g, b);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static double luminance(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }

    /** Fill color for a severity pill badge. */
    public static Color severityFill(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase()) {
            case "critical" -> critical;
            case "high" -> high;
            case "medium" -> medium;
            case "low" -> low;
            default -> textMuted;
        };
    }

    /** Foreground (text) color for a severity pill badge. */
    public static Color severityForeground(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase()) {
            case "critical" -> criticalFg;
            case "high" -> highFg;
            case "medium" -> mediumFg;
            case "low" -> lowFg;
            default -> Color.WHITE;
        };
    }
}
