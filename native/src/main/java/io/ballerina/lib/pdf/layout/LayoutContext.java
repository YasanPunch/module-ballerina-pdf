package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.ConverterOptions;
import io.ballerina.lib.pdf.paint.FontManager;
import io.ballerina.lib.pdf.util.CssValueParser;

/**
 * Holds page dimensions, font manager, and layout state.
 */
public class LayoutContext {

    // A4 page dimensions in points (210mm x 297mm)
    public static final float A4_WIDTH = 595.276f;
    public static final float A4_HEIGHT = 841.89f;

    private float pageWidth;
    private float pageHeight;
    private float marginTop;
    private float marginRight;
    private float marginBottom;
    private float marginLeft;

    private final FontManager fontManager;
    private final float defaultFontSizePt;

    public LayoutContext(FontManager fontManager) {
        this(fontManager, new ConverterOptions());
    }

    public LayoutContext(FontManager fontManager, ConverterOptions options) {
        this.fontManager = fontManager;
        this.defaultFontSizePt = options.getDefaultFontSizePt();
        // Initialize from ConverterOptions (these are defaults; configureFromPageRule may override)
        this.pageWidth = options.getPageWidth();
        this.pageHeight = options.getPageHeight();
        this.marginTop = options.getMarginTop();
        this.marginRight = options.getMarginRight();
        this.marginBottom = options.getMarginBottom();
        this.marginLeft = options.getMarginLeft();
    }

    /**
     * Configures page dimensions from @page CSS rule values.
     */
    public void configureFromPageRule(String pageSize, String pageMargin) {
        if (pageSize != null) {
            String lower = pageSize.trim().toLowerCase();
            if (lower.contains("a4")) {
                pageWidth = A4_WIDTH;
                pageHeight = A4_HEIGHT;
            } else if (lower.contains("letter")) {
                pageWidth = 612f;
                pageHeight = 792f;
            }
            // Could parse explicit dimensions if needed
        }

        if (pageMargin != null) {
            String[] parts = pageMargin.trim().split("\\s+");
            switch (parts.length) {
                case 1 -> {
                    float m = CssValueParser.toPoints(parts[0]);
                    marginTop = marginRight = marginBottom = marginLeft = m;
                }
                case 2 -> {
                    marginTop = marginBottom = CssValueParser.toPoints(parts[0]);
                    marginRight = marginLeft = CssValueParser.toPoints(parts[1]);
                }
                case 3 -> {
                    marginTop = CssValueParser.toPoints(parts[0]);
                    marginRight = marginLeft = CssValueParser.toPoints(parts[1]);
                    marginBottom = CssValueParser.toPoints(parts[2]);
                }
                default -> {
                    marginTop = CssValueParser.toPoints(parts[0]);
                    marginRight = CssValueParser.toPoints(parts[1]);
                    marginBottom = CssValueParser.toPoints(parts[2]);
                    marginLeft = CssValueParser.toPoints(parts[3]);
                }
            }
        }
    }

    /** The available content width (page width minus left/right margins). */
    public float getContentWidth() {
        return pageWidth - marginLeft - marginRight;
    }

    /** The available content height (page height minus top/bottom margins). */
    public float getContentHeight() {
        return pageHeight - marginTop - marginBottom;
    }

    public float getPageWidth() { return pageWidth; }
    public float getPageHeight() { return pageHeight; }
    public float getMarginTop() { return marginTop; }
    public float getMarginRight() { return marginRight; }
    public float getMarginBottom() { return marginBottom; }
    public float getMarginLeft() { return marginLeft; }
    public FontManager getFontManager() { return fontManager; }
    public float getDefaultFontSizePt() { return defaultFontSizePt; }
}
