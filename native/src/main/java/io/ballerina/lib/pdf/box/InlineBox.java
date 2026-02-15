package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * An inline-level box (span, strong, a, em, etc.).
 * Participates in inline formatting context — flows horizontally.
 */
public class InlineBox extends Box {

    public InlineBox(ComputedStyle style) {
        super(style);
    }

    @Override
    public String getBoxType() {
        return "inline";
    }
}
