/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

    private float fontSizePt;
    private float pageWidth;
    private float pageHeight;
    private float marginTop;
    private float marginRight;
    private float marginBottom;
    private float marginLeft;
    private String additionalCss;
    private Map<String, byte[]> customFonts;
    private Integer maxPages;

    /** Default options: 12pt font, A4, 0pt margins, no additional CSS. */
    public ConverterOptions() {
        this(DEFAULT_FONT_SIZE_PT, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
    }

    /** Convenience constructor for font size only. */
    public ConverterOptions(float fontSizePt) {
        this(fontSizePt, A4_WIDTH, A4_HEIGHT,
                DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                null, null, null);
    }

    /** Constructor with all options except custom fonts and maxPages. */
    public ConverterOptions(float fontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss) {
        this(fontSizePt, pageWidth, pageHeight,
                marginTop, marginRight, marginBottom, marginLeft,
                additionalCss, null, null);
    }

    /** Constructor with all options except maxPages. */
    public ConverterOptions(float fontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss,
                            Map<String, byte[]> customFonts) {
        this(fontSizePt, pageWidth, pageHeight,
                marginTop, marginRight, marginBottom, marginLeft,
                additionalCss, customFonts, null);
    }

    /** Full constructor with all options. */
    public ConverterOptions(float fontSizePt,
                            float pageWidth, float pageHeight,
                            float marginTop, float marginRight,
                            float marginBottom, float marginLeft,
                            String additionalCss,
                            Map<String, byte[]> customFonts, Integer maxPages) {
        if (fontSizePt <= 0) {
            throw new IllegalArgumentException("fontSizePt must be positive, got: " + fontSizePt);
        }
        if (pageWidth <= 0) {
            throw new IllegalArgumentException("pageWidth must be positive, got: " + pageWidth);
        }
        if (pageHeight <= 0) {
            throw new IllegalArgumentException("pageHeight must be positive, got: " + pageHeight);
        }
        if (marginTop < 0) {
            throw new IllegalArgumentException("marginTop must be non-negative, got: " + marginTop);
        }
        if (marginRight < 0) {
            throw new IllegalArgumentException("marginRight must be non-negative, got: " + marginRight);
        }
        if (marginBottom < 0) {
            throw new IllegalArgumentException("marginBottom must be non-negative, got: " + marginBottom);
        }
        if (marginLeft < 0) {
            throw new IllegalArgumentException("marginLeft must be non-negative, got: " + marginLeft);
        }
        this.fontSizePt = fontSizePt;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
        this.marginLeft = marginLeft;
        this.additionalCss = additionalCss;
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

    public float getFontSizePt() { return fontSizePt; }
    public float getPageWidth() { return pageWidth; }
    public float getPageHeight() { return pageHeight; }
    public float getMarginTop() { return marginTop; }
    public float getMarginRight() { return marginRight; }
    public float getMarginBottom() { return marginBottom; }
    public float getMarginLeft() { return marginLeft; }
    public String getAdditionalCss() { return additionalCss; }
    public Map<String, byte[]> getCustomFonts() { return customFonts; }
    public Integer getMaxPages() { return maxPages; }

    public void setFontSizePt(float fontSizePt) { this.fontSizePt = fontSizePt; }
    public void setPageWidth(float pageWidth) { this.pageWidth = pageWidth; }
    public void setPageHeight(float pageHeight) { this.pageHeight = pageHeight; }
    public void setMarginTop(float marginTop) { this.marginTop = marginTop; }
    public void setMarginRight(float marginRight) { this.marginRight = marginRight; }
    public void setMarginBottom(float marginBottom) { this.marginBottom = marginBottom; }
    public void setMarginLeft(float marginLeft) { this.marginLeft = marginLeft; }
    public void setAdditionalCss(String additionalCss) { this.additionalCss = additionalCss; }
    public void setCustomFonts(Map<String, byte[]> customFonts) { this.customFonts = customFonts; }
    public void setMaxPages(Integer maxPages) { this.maxPages = maxPages; }
}
