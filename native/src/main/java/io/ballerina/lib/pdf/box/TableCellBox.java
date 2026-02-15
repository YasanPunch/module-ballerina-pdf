package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * A table cell box (td/th) with colspan support.
 * Acts as a block formatting context for its content.
 */
public class TableCellBox extends Box {

    private int colspan = 1;
    private int startColumn = 0; // 0-based column index

    public TableCellBox(ComputedStyle style) {
        super(style);
    }

    public int getColspan() { return colspan; }
    public void setColspan(int colspan) { this.colspan = colspan; }

    public int getStartColumn() { return startColumn; }
    public void setStartColumn(int startColumn) { this.startColumn = startColumn; }

    @Override
    public String getBoxType() {
        return "table-cell";
    }
}
