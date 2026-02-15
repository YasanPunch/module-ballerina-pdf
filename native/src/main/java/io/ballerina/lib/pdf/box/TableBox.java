package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * A table box. Contains TableRowGroupBox or TableRowBox children.
 * Stores column width percentages from &lt;colgroup&gt;.
 */
public class TableBox extends Box {

    private final List<Float> columnWidths = new ArrayList<>(); // percentages
    private boolean borderCollapse;

    public TableBox(ComputedStyle style) {
        super(style);
    }

    public List<Float> getColumnWidths() { return columnWidths; }

    public void addColumnWidth(float pct) {
        columnWidths.add(pct);
    }

    public boolean isBorderCollapse() { return borderCollapse; }
    public void setBorderCollapse(boolean val) { this.borderCollapse = val; }

    @Override
    public String getBoxType() {
        return "table";
    }
}
