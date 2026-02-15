package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

/**
 * A replaced element box (img with base64 data URL).
 * Stores the data URL source and intrinsic dimensions.
 */
public class ReplacedBox extends Box {

    private String src; // data:image/png;base64,...
    private float intrinsicWidth;
    private float intrinsicHeight;

    public ReplacedBox(ComputedStyle style) {
        super(style);
    }

    public String getSrc() { return src; }
    public void setSrc(String src) { this.src = src; }

    public float getIntrinsicWidth() { return intrinsicWidth; }
    public void setIntrinsicWidth(float w) { this.intrinsicWidth = w; }

    public float getIntrinsicHeight() { return intrinsicHeight; }
    public void setIntrinsicHeight(float h) { this.intrinsicHeight = h; }

    @Override
    public String getBoxType() {
        return "replaced";
    }
}
