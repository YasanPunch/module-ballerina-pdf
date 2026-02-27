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

package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.ConverterOptions;
import io.ballerina.lib.pdf.paint.FontManager;
import io.ballerina.lib.pdf.util.CssValueParser;

/**
 * Holds page dimensions, font manager, and layout state.
 */
public class LayoutContext {

    private float pageWidth;
    private float pageHeight;
    private float marginTop;
    private float marginRight;
    private float marginBottom;
    private float marginLeft;

    private final FontManager fontManager;
    private final float fontSizePt;

    public LayoutContext(FontManager fontManager) {
        this(fontManager, new ConverterOptions());
    }

    public LayoutContext(FontManager fontManager, ConverterOptions options) {
        this.fontManager = fontManager;
        this.fontSizePt = options.getFontSizePt();
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
            if (lower.equals("a4")) {
                pageWidth = ConverterOptions.A4_WIDTH;
                pageHeight = ConverterOptions.A4_HEIGHT;
            } else if (lower.equals("letter")) {
                pageWidth = ConverterOptions.LETTER_WIDTH;
                pageHeight = ConverterOptions.LETTER_HEIGHT;
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
    public float getFontSizePt() { return fontSizePt; }
}
