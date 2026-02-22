package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * Marker box for {@code <br>} elements.
 * Signals a forced line break during inline layout.
 */
public class BrBox extends Box {

    public BrBox(ComputedStyle style) {
        super(style);
    }

    @Override
    public String getBoxType() {
        return "br";
    }
}
