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

    // Default margin: 36pt = 0.5 inch
    public static final float DEFAULT_MARGIN = 36f;

    private final float defaultFontSizePt;
    private final float pageWidth;
    private final float pageHeight;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;
    private final float marginLeft;
    private final String additionalCss;
    private final boolean preprocess;
    private final Map<String, byte[]> customFonts;

    /** Default options: 12pt font, A4, 36pt margins, no additional CSS, preprocessing enabled. */
    public ConverterOptions() {
        this(DEFAULT_FONT_SIZE_PT, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, true, null);
    }

    /** Convenience constructor for font size only (backwards compatibility). */
    public ConverterOptions(float defaultFontSizePt) {
        this(defaultFontSizePt, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, true, null);
    }

    /** Constructor with all options except custom fonts (backwards compatibility). */
    public ConverterOptions(float defaultFontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss, boolean preprocess) {
        this(defaultFontSizePt, pageWidth, pageHeight,
                marginTop, marginRight, marginBottom, marginLeft,
                additionalCss, preprocess, null);
    }

    /** Full constructor with all options. */
    public ConverterOptions(float defaultFontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss, boolean preprocess,
                            Map<String, byte[]> customFonts) {
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
    }

    /** Returns options configured for CIBIL reports (9pt, A4, tight margins). For testing only. */
    public static ConverterOptions cibil() {
        return new ConverterOptions(9f, A4_WIDTH, A4_HEIGHT,
                15f * 2.8346457f, 10f * 2.8346457f,
                15f * 2.8346457f, 10f * 2.8346457f,
                String.join("\n",
                        ".maincontainer { width: 100% !important; max-width: 100% !important; }",
                        ".head1, .headtitle1 { width: auto !important; }"
                ),
                true);
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
}
