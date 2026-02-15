package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * A text fragment with associated font metrics.
 * Leaf node in the box tree — has no children.
 */
public class TextRun extends Box {

    private String text;
    private PDFont font;
    private float fontSize;
    private float textWidth;
    private boolean isSuperscript;

    public TextRun(ComputedStyle style, String text) {
        super(style);
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public PDFont getFont() { return font; }
    public void setFont(PDFont font) { this.font = font; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public float getTextWidth() { return textWidth; }
    public void setTextWidth(float textWidth) { this.textWidth = textWidth; }

    public boolean isSuperscript() { return isSuperscript; }
    public void setSuperscript(boolean superscript) { isSuperscript = superscript; }

    @Override
    public String getBoxType() {
        return "text";
    }
}
