package ghostjs.ui;

import ghostjs.core.Finding;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public final class FindingsTableModel extends AbstractTableModel {

    private static final String[] COLS =
            {"Severity", "Type", "Category", "Conf", "Value", "URL", "Line"};

    private List<Finding> rows = new ArrayList<>();

    public void setRows(List<Finding> rows) {
        this.rows = rows;
        fireTableDataChanged();
    }

    public Finding at(int row) {
        return (row >= 0 && row < rows.size()) ? rows.get(row) : null;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Finding f = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> f.severity();
            case 1 -> f.type();
            case 2 -> f.category();
            case 3 -> f.confidence();
            case 4 -> f.maskedValue();
            case 5 -> f.url();
            case 6 -> f.lineNumber() == 0 ? "" : f.lineNumber();
            default -> "";
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
