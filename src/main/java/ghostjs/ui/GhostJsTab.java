package ghostjs.ui;

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
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
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
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detail.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), new JScrollPane(detail));
        split.setResizeWeight(0.6);
        root.add(split, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        root.add(status, BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x14, 0x1B, 0x2E));
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel(ghostjs.Branding.PRODUCT
                + "  —  " + ghostjs.Branding.TAGLINE);
        title.setForeground(new Color(0x66, 0xE0, 0xC0));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        header.add(title, BorderLayout.WEST);

        JLabel vendor = new JLabel("by " + ghostjs.Branding.VENDOR
                + "  ·  v" + ghostjs.Branding.VERSION);
        vendor.setForeground(new Color(0x9A, 0xA6, 0xC0));
        vendor.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
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
        clear.addActionListener(e -> store.clear());
        bar.add(clear);

        JButton export = new JButton("Export report");
        export.addActionListener(e -> exportReport());
        bar.add(export);

        return bar;
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

    /** Colours the severity cell by risk. */
    private static final class SeverityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String sev = value == null ? "" : value.toString().toLowerCase();
            if (!isSelected) {
                c.setBackground(switch (sev) {
                    case "critical" -> new Color(0xFF, 0xCD, 0xD2);
                    case "high" -> new Color(0xFF, 0xE0, 0xB2);
                    case "medium" -> new Color(0xFF, 0xF9, 0xC4);
                    case "low" -> new Color(0xDC, 0xED, 0xC8);
                    default -> table.getBackground();
                });
            }
            setValue(Severity.rank(sev) < 5 ? sev.toUpperCase() : sev);
            return c;
        }
    }
}
