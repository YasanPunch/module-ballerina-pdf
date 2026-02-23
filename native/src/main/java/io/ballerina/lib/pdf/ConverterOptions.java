package io.ballerina.lib.pdf;

import java.util.Map;

/**
 * Configuration options for HTML-to-PDF conversion.
 */
public class ConverterOptions {

    /** CSS-spec default: medium = 16px = 12pt */
    public static final float DEFAULT_FONT_SIZE_PT = 12f;

    // A4 dimensions in points (210mm x 297mm)
    public static final float A4_WIDTH = 595.276f;
    public static final float A4_HEIGHT = 841.89f;

    // US Letter dimensions in points (8.5" x 11")
    public static final float LETTER_WIDTH = 612f;
    public static final float LETTER_HEIGHT = 792f;

    // US Legal dimensions in points (8.5" x 14")
    public static final float LEGAL_WIDTH = 612f;
    public static final float LEGAL_HEIGHT = 1008f;

    // Default margin: 0pt (no page margin; CSS controls spacing)
    public static final float DEFAULT_MARGIN = 0f;

    private float defaultFontSizePt;
    private float pageWidth;
    private float pageHeight;
    private float marginTop;
    private float marginRight;
    private float marginBottom;
    private float marginLeft;
    private String additionalCss;
    private boolean preprocess;
    private Map<String, byte[]> customFonts;
    private int maxPages;

    /** Default options: 12pt font, A4, 36pt margins, no additional CSS, preprocessing enabled. */
    public ConverterOptions() {
        this(DEFAULT_FONT_SIZE_PT, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, true, null, 0);
    }

    /** Convenience constructor for font size only. */
    public ConverterOptions(float defaultFontSizePt) {
        this(defaultFontSizePt, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, true, null, 0);
    }

    /** Constructor with all options except custom fonts and maxPages. */
    public ConverterOptions(float defaultFontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss, boolean preprocess) {
        this(defaultFontSizePt, pageWidth, pageHeight,
                marginTop, marginRight, marginBottom, marginLeft,
                additionalCss, preprocess, null, 0);
    }

    /** Constructor with all options except maxPages. */
    public ConverterOptions(float defaultFontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss, boolean preprocess,
                            Map<String, byte[]> customFonts) {
        this(defaultFontSizePt, pageWidth, pageHeight,
                marginTop, marginRight, marginBottom, marginLeft,
                additionalCss, preprocess, customFonts, 0);
    }

    /** Full constructor with all options. */
    public ConverterOptions(float defaultFontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss, boolean preprocess,
                            Map<String, byte[]> customFonts, int maxPages) {
        this.defaultFontSizePt = defaultFontSizePt;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;
        this.additionalCss = additionalCss;
        this.preprocess = preprocess;
        this.customFonts = customFonts;
        this.maxPages = maxPages;
    }

    /** Resolves page dimensions from a page size name (A4, LETTER, LEGAL). */
    public static float[] pageDimensions(String pageSize) {
        return switch (pageSize.toUpperCase()) {
            case "LETTER" -> new float[]{LETTER_WIDTH, LETTER_HEIGHT};
            case "LEGAL" -> new float[]{LEGAL_WIDTH, LEGAL_HEIGHT};
            default -> new float[]{A4_WIDTH, A4_HEIGHT};
        };
    }

    public float getDefaultFontSizePt() { return defaultFontSizePt; }
    public float getPageWidth() { return pageWidth; }
    public float getPageHeight() { return pageHeight; }
    public float getMarginTop() { return marginTop; }
    public float getMarginRight() { return marginRight; }
    public float getMarginBottom() { return marginBottom; }
    public float getMarginLeft() { return marginLeft; }
    public String getAdditionalCss() { return additionalCss; }
    public boolean isPreprocess() { return preprocess; }
    public Map<String, byte[]> getCustomFonts() { return customFonts; }
    public int getMaxPages() { return maxPages; }

    public void setDefaultFontSizePt(float defaultFontSizePt) { this.defaultFontSizePt = defaultFontSizePt; }
    public void setPageWidth(float pageWidth) { this.pageWidth = pageWidth; }
    public void setPageHeight(float pageHeight) { this.pageHeight = pageHeight; }
    public void setMarginTop(float marginTop) { this.marginTop = marginTop; }
    public void setMarginRight(float marginRight) { this.marginRight = marginRight; }
    public void setMarginBottom(float marginBottom) { this.marginBottom = marginBottom; }
    public void setMarginLeft(float marginLeft) { this.marginLeft = marginLeft; }
    public void setAdditionalCss(String additionalCss) { this.additionalCss = additionalCss; }
    public void setPreprocess(boolean preprocess) { this.preprocess = preprocess; }
    public void setCustomFonts(Map<String, byte[]> customFonts) { this.customFonts = customFonts; }
    public void setMaxPages(int maxPages) { this.maxPages = maxPages; }
}
