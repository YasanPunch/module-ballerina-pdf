package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * A block-level box (div, p, body, h1, etc.).
 * Participates in block formatting context — stacks vertically.
 */
public class BlockBox extends Box {

    public BlockBox(ComputedStyle style) {
        super(style);
    }

    @Override
    public String getBoxType() {
        return "block";
    }
}
