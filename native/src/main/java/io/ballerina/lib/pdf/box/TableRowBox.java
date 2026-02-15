package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * A table row box (tr).
 * Contains TableCellBox children.
 */
public class TableRowBox extends Box {

    public TableRowBox(ComputedStyle style) {
        super(style);
    }

    @Override
    public String getBoxType() {
        return "table-row";
    }
}
