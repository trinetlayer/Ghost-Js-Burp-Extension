package ghostjs.ui;

import com.formdev.flatlaf.FlatClientProperties;
import ghostjs.core.Finding;
import ghostjs.core.FindingStore;
import ghostjs.core.Severity;
import ghostjs.http.GhostConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** The "GhostJS" suite tab: findings table + detail pane + controls. */
public final class GhostJsTab implements FindingStore.Listener {

    private final FindingStore store;
    private final GhostConfig config;
    private final int patternCount;

    private final JPanel root = new JPanel(new BorderLayout());
    private final FindingsTableModel model = new FindingsTableModel();
    private final JTable table = new JTable(model);
    private final JTextArea detail = new JTextArea();
    private final JLabel status = new JLabel();

    public GhostJsTab(FindingStore store, GhostConfig config, int patternCount) {
        this.store = store;
        this.config = config;
        this.patternCount = patternCount;
        build();
        store.addListener(this);
        refresh();
    }

    public Component component() {
        return root;
    }

    private void build() {
        JPanel north = new JPanel(new BorderLayout());
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(buildToolbar(), BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.getColumnModel().getColumn(0).setCellRenderer(new SeverityRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(6).setMaxWidth(60);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(GhostTheme.FONT_MONO);
        detail.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(detail));
        split.setResizeWeight(0.6);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(6);
        root.add(split, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        status.setForeground(GhostTheme.textMuted);
        status.setFont(GhostTheme.FONT_SUBTLE);
        root.add(status, BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(GhostTheme.headerBg);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel(ghostjs.Branding.PRODUCT
                + "  —  " + ghostjs.Branding.TAGLINE);
        title.setForeground(GhostTheme.accent);
        title.setFont(GhostTheme.FONT_TITLE);
        header.add(title, BorderLayout.WEST);

        JLabel vendor = new JLabel("by " + ghostjs.Branding.VENDOR
                + "  ·  v" + ghostjs.Branding.VERSION);
        vendor.setForeground(GhostTheme.textMuted);
        vendor.setFont(GhostTheme.FONT_SUBTLE);
        header.add(vendor, BorderLayout.EAST);
        return header;
    }

    private JComponent buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        bar.add(toggle("Scan", config.scanEnabled, v -> config.scanEnabled = v));
        bar.add(toggle("Active fetch", config.activeFetchEnabled, v -> config.activeFetchEnabled = v));
        bar.add(toggle("Scan HTML", config.scanHtmlBodies, v -> config.scanHtmlBodies = v));
        bar.add(toggle("Scan JSON", config.scanJsonBodies, v -> config.scanJsonBodies = v));
        bar.add(toggle("Highlight proxy", config.highlightProxy, v -> config.highlightProxy = v));
        bar.add(toggle("Respect scope", config.respectScope, v -> config.respectScope = v));
        bar.addSeparator();

        JButton clear = new JButton("Clear");
        clear.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        clear.addActionListener(e -> store.clear());
        bar.add(clear);

        JButton export = new JButton("Export report");
        export.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        export.putClientProperty(FlatClientProperties.STYLE,
                "background:" + toHex(GhostTheme.accent) + "; foreground:#FFFFFF; focusedBackground:" + toHex(GhostTheme.accent));
        export.addActionListener(e -> exportReport());
        bar.add(export);

        return bar;
    }

    private static String toHex(java.awt.Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private JCheckBox toggle(String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        JCheckBox box = new JCheckBox(label, initial);
        box.addActionListener(e -> onChange.accept(box.isSelected()));
        return box;
    }

    private void showDetail() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            detail.setText("");
            return;
        }
        Finding f = model.at(table.convertRowIndexToModel(viewRow));
        if (f == null) return;
        detail.setText(
                "Type:        " + f.type() + "\n" +
                "Category:    " + f.category() + "\n" +
                "Severity:    " + f.severity() + "   (confidence " + f.confidence() + ")\n" +
                "URL:         " + f.url() + "\n" +
                "Line:        " + (f.lineNumber() == 0 ? "-" : f.lineNumber()) + "\n" +
                "Value:       " + f.value() + "\n\n" +
                "Context:\n  " + f.snippet() + "\n\n" +
                "Impact:\n" + f.impactSummary() + "\n\n" +
                "Remediation:\n" + f.remediation() + "\n");
        detail.setCaretPosition(0);
    }

    private void exportReport() {
        List<Finding> all = store.snapshot();
        if (all.isEmpty()) {
            JOptionPane.showMessageDialog(root, "No findings to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("ghostjs-findings.md"));
        if (chooser.showSaveDialog(root) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), buildMarkdown(all),
                    StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(root, "Saved " + all.size() + " findings.");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String buildMarkdown(List<Finding> all) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(ghostjs.Branding.PRODUCT).append(" Findings\n\n")
          .append("_").append(ghostjs.Branding.TAGLINE)
          .append(" · by ").append(ghostjs.Branding.VENDOR)
          .append(" · v").append(ghostjs.Branding.VERSION).append("_\n\n")
          .append("Total: ").append(all.size()).append("\n\n");
        for (Finding f : all) {
            sb.append("## [").append(f.severity().toUpperCase()).append("] ")
              .append(f.type()).append("\n\n")
              .append("- URL: ").append(f.url()).append("\n")
              .append("- Line: ").append(f.lineNumber() == 0 ? "-" : f.lineNumber()).append("\n")
              .append("- Confidence: ").append(f.confidence()).append("\n")
              .append("- Value: `").append(f.value()).append("`\n")
              .append("- Context: `").append(f.snippet()).append("`\n\n")
              .append(f.impactSummary()).append("\n\n")
              .append("**Remediation:** ").append(f.remediation()).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    @Override
    public void onFindingsChanged() {
        SwingUtilities.invokeLater(this::refresh);
    }

    private void refresh() {
        model.setRows(store.snapshot());
        status.setText("  " + ghostjs.Branding.PRODUCT + " by " + ghostjs.Branding.VENDOR
                + "  |  " + store.size() + " findings  |  "
                + patternCount + " patterns loaded");
    }

    /** Renders the severity cell as a rounded, colour-coded pill badge. */
    private static final class SeverityRenderer extends DefaultTableCellRenderer {
        private String severity = "";

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            severity = value == null ? "" : value.toString().toLowerCase();
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            setHorizontalAlignment(CENTER);
            setValue(Severity.rank(severity) < 5 ? severity.toUpperCase() : severity);
            setOpaque(true);
            return c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            String text = getText();
            if (text == null || text.isBlank()) {
                super.paintComponent(g);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(getFont());
            int textWidth = g2.getFontMetrics().stringWidth(text);
            int pillWidth = Math.min(getWidth() - 6, textWidth + 18);
            int pillHeight = Math.min(getHeight() - 6, 18);
            int x = (getWidth() - pillWidth) / 2;
            int y = (getHeight() - pillHeight) / 2;

            g2.setColor(GhostTheme.severityFill(severity));
            g2.fill(new RoundRectangle2D.Float(x, y, pillWidth, pillHeight, pillHeight, pillHeight));

            g2.setColor(GhostTheme.severityForeground(severity));
            int textX = x + (pillWidth - textWidth) / 2;
            int textY = y + (pillHeight - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(text, textX, textY);
            g2.dispose();
        }
    }
}
