package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * A table row group box (thead, tbody, tfoot).
 * Contains TableRowBox children.
 */
public class TableRowGroupBox extends Box {

    public TableRowGroupBox(ComputedStyle style) {
        super(style);
    }

    @Override
    public String getBoxType() {
        return "table-row-group";
    }
}
