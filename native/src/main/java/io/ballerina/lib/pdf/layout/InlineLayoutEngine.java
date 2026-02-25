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

import io.ballerina.lib.pdf.box.BlockBox;
import io.ballerina.lib.pdf.box.Box;
import io.ballerina.lib.pdf.box.BrBox;
import io.ballerina.lib.pdf.box.InlineBox;
import io.ballerina.lib.pdf.box.ReplacedBox;
import io.ballerina.lib.pdf.box.TextRun;
import io.ballerina.lib.pdf.css.ComputedStyle;
import io.ballerina.lib.pdf.paint.FontManager;
import io.ballerina.lib.pdf.util.CssValueParser;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays out inline content: measures text, breaks lines, applies text-align.
 * Converts inline boxes and text runs into positioned line boxes.
 */
public class InlineLayoutEngine {

    private final FontManager fontManager;
    private final float fontSizePt;
    private final BlockFormattingContext bfc;

    public InlineLayoutEngine(FontManager fontManager, float fontSizePt,
                               BlockFormattingContext bfc) {
        this.fontManager = fontManager;
        this.fontSizePt = fontSizePt;
        this.bfc = bfc;
    }

    /**
     * Provides line-specific width and x-offset based on vertical position.
     * Used by the BFC to narrow lines next to floated elements.
     */
    @FunctionalInterface
    interface LineWidthProvider {
        /** Returns {availableWidth, xOffset} for a line starting at the given y position. */
        float[] getWidthAndOffsetAtY(float y);
    }

    /**
     * Lays out inline children of a block box within the given available width.
     * Returns the total height consumed by the inline content.
     */
    public float layout(Box container, float availableWidth) {
        return layout(container, availableWidth, null, 0f);
    }

    /**
     * Lays out inline children with float-aware line widths.
     * @param provider supplies per-line width and x-offset (null = constant availableWidth)
     * @param startY the container's y position within the BFC (for provider lookups)
     */
    public float layout(Box container, float availableWidth, LineWidthProvider provider, float startY) {
        List<Box> inlineChildren = container.getChildren();
        if (inlineChildren.isEmpty()) return 0;

        // Collect all leaf inline items (text runs, replaced boxes, inline-blocks)
        List<InlineItem> items = new ArrayList<>();
        collectInlineItems(inlineChildren, items, availableWidth);
        if (items.isEmpty()) return 0;

        // Break into lines (float-aware if provider is given)
        List<Line> lines = (provider != null)
                ? breakIntoLinesWithProvider(items, availableWidth, provider, startY)
                : breakIntoLines(items, availableWidth);

        // Position each line
        float cursorY = 0;
        String textAlign = container.getStyle() != null ? container.getStyle().getTextAlign() : "left";

        for (Line line : lines) {
            float lineHeight = line.height;

            // Get line-specific x offset from float context or alignment
            float lineAvailWidth = availableWidth;
            float floatXOffset = 0;
            if (provider != null) {
                float[] wa = provider.getWidthAndOffsetAtY(startY + cursorY);
                lineAvailWidth = wa[0];
                floatXOffset = wa[1];
            }

            float xOffset = floatXOffset;
            // Text alignment within the available (possibly narrowed) width
            float lineWidth = line.width;
            switch (textAlign) {
                case "center" -> xOffset += (lineAvailWidth - lineWidth) / 2f;
                case "right" -> xOffset += lineAvailWidth - lineWidth;
            }

            // Position each item on this line
            float cx = xOffset;
            for (InlineItem item : line.items) {
                item.box.setX(cx);
                item.box.setY(cursorY);
                // Only set width/height for text runs. Inline-block and replaced boxes
                // have their own dimensions computed during layout — overwriting would
                // corrupt their box model (e.g., setWidth(outerWidth) double-counts padding).
                if (item.box instanceof TextRun) {
                    item.box.setWidth(item.width);
                    item.box.setHeight(lineHeight);
                }
                cx += item.width;
            }

            cursorY += lineHeight;
        }

        // Store positioned leaf items alongside original children (non-destructive).
        // The painter reads getEffectiveChildren() which returns layoutChildren when set.
        // Original children are preserved for potential re-layout.
        List<Box> positioned = new ArrayList<>();
        for (Line line : lines) {
            for (InlineItem item : line.items) {
                positioned.add(item.box);
            }
        }
        container.setLayoutChildren(positioned);

        return cursorY;
    }

    /**
     * Recursively collects inline items (text runs, replaced boxes, inline-blocks) from inline boxes.
     */
    private void collectInlineItems(List<Box> boxes, List<InlineItem> items, float availableWidth) {
        for (Box box : boxes) {
            if (box.getStyle() != null && "absolute".equals(box.getStyle().getPosition())) {
                continue; // absolute-positioned elements are handled by BFC
            }
            if (box instanceof TextRun textRun) {
                resolveTextMetrics(textRun);
                // Split text into words for line breaking
                String text = textRun.getText();
                if (text == null || text.isEmpty()) continue;

                // Resolve letter-spacing and word-spacing for width calculations
                float letterSpacing = 0;
                float wordSpacing = 0;
                ComputedStyle textStyle = textRun.getStyle();
                if (textStyle != null) {
                    float fs = textRun.getFontSize();
                    letterSpacing = textStyle.getLetterSpacing(fs);
                    wordSpacing = textStyle.getWordSpacing(fs);
                }

                String[] words = text.split("(?<= )"); // split keeping trailing spaces
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    TextRun wordRun = new TextRun(textRun.getStyle(), word);
                    wordRun.setFont(textRun.getFont());
                    wordRun.setFontSize(textRun.getFontSize());
                    wordRun.setSuperscript(textRun.isSuperscript());
                    wordRun.setSubscript(textRun.isSubscript());
                    if (textRun.getHref() != null) wordRun.setHref(textRun.getHref());
                    float wordWidth = fontManager.measureText(word, textRun.getFont(), textRun.getFontSize());
                    // Add letter-spacing between characters
                    if (letterSpacing != 0 && word.length() > 1) {
                        wordWidth += letterSpacing * (word.length() - 1);
                    }
                    // Add word-spacing for each space character in this word chunk
                    if (wordSpacing != 0) {
                        for (int ci = 0; ci < word.length(); ci++) {
                            if (word.charAt(ci) == ' ') wordWidth += wordSpacing;
                        }
                    }
                    wordRun.setTextWidth(wordWidth);
                    float cssLH = textStyle != null ? textStyle.getLineHeight(textRun.getFontSize()) : -1;
                    float effectiveLH = cssLH > 0
                            ? cssLH
                            : fontManager.getLineHeight(textRun.getFont(), textRun.getFontSize());
                    items.add(new InlineItem(wordRun, wordWidth, effectiveLH));
                }
            } else if (box instanceof ReplacedBox replaced) {
                float w = replaced.getIntrinsicWidth();
                float h = replaced.getIntrinsicHeight();
                items.add(new InlineItem(replaced, w, h));
            } else if (box instanceof BlockBox bb
                    && bb.getStyle() != null
                    && "inline-block".equals(bb.getStyle().getDisplay())) {
                // Inline-block: resolve box model, compute shrink-to-fit content width, lay out
                bfc.resolveBoxModelWithWidth(bb, availableWidth);
                float contentWidth = computeShrinkToFitWidth(bb, availableWidth);
                bb.setWidth(contentWidth);

                float contentHeight = bfc.layoutChildren(bb, contentWidth);
                // Apply explicit height + min/max clamping (matches BFC block-level handling)
                ComputedStyle ibStyle = bb.getStyle();
                float ibFontSize = ibStyle.getFontSize(fontSizePt);
                float explicitHeight = ibStyle.getHeight(contentHeight, ibFontSize);
                if (explicitHeight > 0) {
                    contentHeight = Math.max(contentHeight, explicitHeight);
                }
                float maxH = ibStyle.getMaxHeight(contentHeight, ibFontSize);
                float minH = ibStyle.getMinHeight(contentHeight, ibFontSize);
                contentHeight = Math.max(minH, Math.min(contentHeight, maxH));
                bb.setHeight(contentHeight);

                float outerWidth = bb.getOuterWidth();
                float outerHeight = bb.getOuterHeight();
                items.add(new InlineItem(bb, outerWidth, outerHeight));
            } else if (box instanceof BrBox br) {
                // Resolve line-height for the break so empty lines have correct spacing
                float brFontSize = fontSizePt;
                float brLineHeight;
                ComputedStyle brStyle = br.getStyle();
                if (brStyle != null) {
                    brFontSize = brStyle.getFontSize(fontSizePt);
                    float cssLH = brStyle.getLineHeight(brFontSize);
                    brLineHeight = cssLH > 0 ? cssLH
                            : fontManager.getLineHeight(fontManager.getDefaultFont(), brFontSize);
                } else {
                    brLineHeight = fontManager.getLineHeight(fontManager.getDefaultFont(), brFontSize);
                }
                items.add(new InlineItem(br, 0, brLineHeight, true));
            } else if (box instanceof InlineBox) {
                // Propagate href from <a> InlineBox to children before recursing,
                // since the InlineBox itself is discarded (only leaf items are surfaced)
                String href = box.getHref();
                if (href != null) {
                    for (Box child : box.getChildren()) {
                        if (child.getHref() == null) {
                            child.setHref(href);
                        }
                    }
                }
                collectInlineItems(box.getChildren(), items, availableWidth);
            }
        }
    }

    private void resolveTextMetrics(TextRun textRun) {
        ComputedStyle style = textRun.getStyle();
        if (style == null) {
            textRun.setFont(fontManager.getDefaultFont());
            textRun.setFontSize(fontSizePt);
            return;
        }

        String[] families = CssValueParser.parseFontFamilyList(style.getFontFamily());
        boolean bold = style.isBold();
        boolean italic = style.isItalic();
        float fontSize = style.getFontSize(fontSizePt);

        // Check for superscript / subscript
        String verticalAlign = style.get("vertical-align");
        if ("super".equals(verticalAlign)) {
            textRun.setSuperscript(true);
            fontSize *= 0.7f;
        } else if ("sub".equals(verticalAlign)) {
            textRun.setSubscript(true);
            fontSize *= 0.7f;
        }

        PDFont font = fontManager.getFont(families, bold, italic);
        textRun.setFont(font);
        textRun.setFontSize(fontSize);

        // Apply text-transform
        String transform = style.getTextTransform();
        if ("uppercase".equals(transform)) {
            textRun.setText(textRun.getText().toUpperCase());
        } else if ("lowercase".equals(transform)) {
            textRun.setText(textRun.getText().toLowerCase());
        } else if ("capitalize".equals(transform)) {
            textRun.setText(capitalize(textRun.getText()));
        }

        float textWidth = fontManager.measureText(textRun.getText(), font, fontSize);
        textRun.setTextWidth(textWidth);
    }

    private List<Line> breakIntoLines(List<InlineItem> items, float maxWidth) {
        List<Line> lines = new ArrayList<>();
        List<InlineItem> currentLine = new ArrayList<>();
        float currentWidth = 0;
        float maxLineHeight = 0;

        for (InlineItem item : items) {
            // Forced break from <br>: finalize the current line immediately
            if (item.forcedBreak) {
                float lineHeight = maxLineHeight > 0 ? maxLineHeight : item.height;
                lines.add(new Line(currentLine, currentWidth, lineHeight));
                currentLine = new ArrayList<>();
                currentWidth = 0;
                maxLineHeight = 0;
                continue;
            }

            if (currentWidth + item.width > maxWidth && !currentLine.isEmpty()) {
                // Wrap to next line
                lines.add(new Line(currentLine, currentWidth, maxLineHeight));
                currentLine = new ArrayList<>();
                currentWidth = 0;
                maxLineHeight = 0;

                // Strip leading whitespace from the first word on the new line
                if (item.box instanceof TextRun tr && tr.getText().startsWith(" ")) {
                    String trimmed = tr.getText().stripLeading();
                    if (trimmed.isEmpty()) continue;
                    tr.setText(trimmed);
                    float newWidth = fontManager.measureText(trimmed, tr.getFont(), tr.getFontSize());
                    tr.setTextWidth(newWidth);
                    item = new InlineItem(tr, newWidth, item.height);
                }
            }

            currentLine.add(item);
            currentWidth += item.width;
            maxLineHeight = Math.max(maxLineHeight, item.height);
        }

        if (!currentLine.isEmpty()) {
            lines.add(new Line(currentLine, currentWidth, maxLineHeight));
        }

        return lines;
    }

    /**
     * Float-aware line breaking: queries the provider for available width at each line's
     * y position, so lines next to floats are narrower.
     */
    private List<Line> breakIntoLinesWithProvider(List<InlineItem> items, float fallbackWidth,
                                                   LineWidthProvider provider, float startY) {
        List<Line> lines = new ArrayList<>();
        List<InlineItem> currentLine = new ArrayList<>();
        float currentWidth = 0;
        float maxLineHeight = 0;
        float cursorY = 0;

        float[] wa = provider.getWidthAndOffsetAtY(startY + cursorY);
        float lineMaxWidth = wa[0];

        for (InlineItem item : items) {
            // Forced break from <br>: finalize the current line immediately
            if (item.forcedBreak) {
                float lineHeight = maxLineHeight > 0 ? maxLineHeight : item.height;
                lines.add(new Line(currentLine, currentWidth, lineHeight));
                cursorY += lineHeight;
                currentLine = new ArrayList<>();
                currentWidth = 0;
                maxLineHeight = 0;

                wa = provider.getWidthAndOffsetAtY(startY + cursorY);
                lineMaxWidth = wa[0];
                continue;
            }

            if (currentWidth + item.width > lineMaxWidth && !currentLine.isEmpty()) {
                float lineH = maxLineHeight;
                lines.add(new Line(currentLine, currentWidth, lineH));
                cursorY += lineH;
                currentLine = new ArrayList<>();
                currentWidth = 0;
                maxLineHeight = 0;

                // Re-query available width for the new line's y position
                wa = provider.getWidthAndOffsetAtY(startY + cursorY);
                lineMaxWidth = wa[0];

                // Strip leading whitespace from the first word on the new line
                if (item.box instanceof TextRun tr && tr.getText().startsWith(" ")) {
                    String trimmed = tr.getText().stripLeading();
                    if (trimmed.isEmpty()) continue;
                    tr.setText(trimmed);
                    float newWidth = fontManager.measureText(trimmed, tr.getFont(), tr.getFontSize());
                    tr.setTextWidth(newWidth);
                    item = new InlineItem(tr, newWidth, item.height);
                }
            }

            currentLine.add(item);
            currentWidth += item.width;
            maxLineHeight = Math.max(maxLineHeight, item.height);
        }

        if (!currentLine.isEmpty()) {
            lines.add(new Line(currentLine, currentWidth, maxLineHeight));
        }

        return lines;
    }

    private String capitalize(String text) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Computes shrink-to-fit width per CSS 2.1 §10.3.9.
     * Formula: min(max-content, max(min-content, available))
     */
    private float computeShrinkToFitWidth(BlockBox box, float availableWidth) {
        ComputedStyle style = box.getStyle();
        float fontSize = style.getFontSize(fontSizePt);
        float explicitWidth = style.getWidth(availableWidth, fontSize);
        if (explicitWidth > 0) {
            return explicitWidth;
        }

        float maxContent = measureMaxContentWidth(box);
        float minContent = measureMinContentWidth(box);
        float shrink = Math.min(maxContent, Math.max(minContent, availableWidth));

        float maxW = style.getMaxWidth(availableWidth, fontSize);
        float minW = style.getMinWidth(availableWidth, fontSize);
        return Math.max(minW, Math.min(shrink, maxW));
    }

    private float measureMaxContentWidth(Box box) {
        if (box instanceof TextRun textRun) {
            return measureTextRunWidth(textRun);
        }
        float total = 0;
        for (Box child : box.getChildren()) {
            total += measureMaxContentWidth(child);
        }
        return total;
    }

    private float measureMinContentWidth(Box box) {
        if (box instanceof TextRun textRun) {
            return measureWidestWord(textRun);
        }
        float max = 0;
        for (Box child : box.getChildren()) {
            max = Math.max(max, measureMinContentWidth(child));
        }
        return max;
    }

    private float measureTextRunWidth(TextRun textRun) {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) return 0;

        ComputedStyle style = textRun.getStyle();
        PDFont font;
        float fontSize;
        if (style != null) {
            String[] families = CssValueParser.parseFontFamilyList(style.getFontFamily());
            font = fontManager.getFont(families, style.isBold(), style.isItalic());
            fontSize = style.getFontSize(fontSizePt);

            // Apply text-transform before measuring (must match resolveTextMetrics)
            String transform = style.getTextTransform();
            if ("uppercase".equals(transform)) text = text.toUpperCase();
            else if ("lowercase".equals(transform)) text = text.toLowerCase();
            else if ("capitalize".equals(transform)) text = capitalize(text);
        } else {
            font = fontManager.getDefaultFont();
            fontSize = fontSizePt;
        }

        float letterSpacing = (style != null) ? style.getLetterSpacing(fontSize) : 0;

        // Measure word-by-word to match inline layout's word splitting,
        // preventing floating-point mismatch between shrink-to-fit and actual layout
        float total = 0;
        for (String word : text.split("(?<= )")) {
            if (!word.isEmpty()) {
                total += fontManager.measureText(word, font, fontSize);
                if (letterSpacing != 0 && word.length() > 1) {
                    total += letterSpacing * (word.length() - 1);
                }
            }
        }
        return total;
    }

    private float measureWidestWord(TextRun textRun) {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) return 0;

        ComputedStyle style = textRun.getStyle();
        PDFont font;
        float fontSize;
        if (style != null) {
            String[] families = CssValueParser.parseFontFamilyList(style.getFontFamily());
            font = fontManager.getFont(families, style.isBold(), style.isItalic());
            fontSize = style.getFontSize(fontSizePt);

            // Apply text-transform before measuring (must match resolveTextMetrics)
            String transform = style.getTextTransform();
            if ("uppercase".equals(transform)) text = text.toUpperCase();
            else if ("lowercase".equals(transform)) text = text.toLowerCase();
            else if ("capitalize".equals(transform)) text = capitalize(text);
        } else {
            font = fontManager.getDefaultFont();
            fontSize = fontSizePt;
        }

        float letterSpacing = (style != null) ? style.getLetterSpacing(fontSize) : 0;

        float maxWidth = 0;
        for (String word : text.split("\\s+")) {
            if (!word.isEmpty()) {
                float wordWidth = fontManager.measureText(word, font, fontSize);
                if (letterSpacing != 0 && word.length() > 1) {
                    wordWidth += letterSpacing * (word.length() - 1);
                }
                maxWidth = Math.max(maxWidth, wordWidth);
            }
        }
        return maxWidth;
    }

    record InlineItem(Box box, float width, float height, boolean forcedBreak) {
        InlineItem(Box box, float width, float height) {
            this(box, width, height, false);
        }
    }
    record Line(List<InlineItem> items, float width, float height) {}
}
