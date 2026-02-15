package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.*;
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
    private final float defaultFontSizePt;

    public InlineLayoutEngine(FontManager fontManager, float defaultFontSizePt) {
        this.fontManager = fontManager;
        this.defaultFontSizePt = defaultFontSizePt;
    }

    /**
     * Lays out inline children of a block box within the given available width.
     * Returns the total height consumed by the inline content.
     */
    public float layout(Box container, float availableWidth) {
        List<Box> inlineChildren = container.getChildren();
        if (inlineChildren.isEmpty()) return 0;

        // Collect all leaf inline items (text runs, replaced boxes)
        List<InlineItem> items = new ArrayList<>();
        collectInlineItems(inlineChildren, items);
        if (items.isEmpty()) return 0;

        // Break into lines
        List<Line> lines = breakIntoLines(items, availableWidth);

        // Position each line
        float cursorY = 0;
        String textAlign = container.getStyle() != null ? container.getStyle().getTextAlign() : "left";

        for (Line line : lines) {
            float lineHeight = line.height;
            float xOffset = 0;

            // Text alignment
            float lineWidth = line.width;
            switch (textAlign) {
                case "center" -> xOffset = (availableWidth - lineWidth) / 2f;
                case "right" -> xOffset = availableWidth - lineWidth;
            }

            // Position each item on this line
            float cx = xOffset;
            for (InlineItem item : line.items) {
                item.box.setX(cx);
                item.box.setY(cursorY);
                item.box.setWidth(item.width);
                item.box.setHeight(lineHeight);
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
     * Recursively collects inline items (text runs, replaced boxes) from inline boxes.
     */
    private void collectInlineItems(List<Box> boxes, List<InlineItem> items) {
        for (Box box : boxes) {
            if (box instanceof TextRun textRun) {
                resolveTextMetrics(textRun);
                // Split text into words for line breaking
                String text = textRun.getText();
                if (text == null || text.isEmpty()) continue;

                String[] words = text.split("(?<= )"); // split keeping trailing spaces
                for (String word : words) {
                    if (word.isEmpty()) continue;
                    TextRun wordRun = new TextRun(textRun.getStyle(), word);
                    wordRun.setFont(textRun.getFont());
                    wordRun.setFontSize(textRun.getFontSize());
                    wordRun.setSuperscript(textRun.isSuperscript());
                    float wordWidth = fontManager.measureText(word, textRun.getFont(), textRun.getFontSize());
                    wordRun.setTextWidth(wordWidth);
                    items.add(new InlineItem(wordRun, wordWidth,
                            fontManager.getLineHeight(textRun.getFont(), textRun.getFontSize())));
                }
            } else if (box instanceof ReplacedBox replaced) {
                float w = replaced.getIntrinsicWidth();
                float h = replaced.getIntrinsicHeight();
                items.add(new InlineItem(replaced, w, h));
            } else if (box instanceof InlineBox) {
                // Recurse into inline box children
                collectInlineItems(box.getChildren(), items);
            }
        }
    }

    private void resolveTextMetrics(TextRun textRun) {
        ComputedStyle style = textRun.getStyle();
        if (style == null) {
            textRun.setFont(fontManager.getDefaultFont());
            textRun.setFontSize(defaultFontSizePt);
            return;
        }

        String family = CssValueParser.parsePrimaryFontFamily(style.getFontFamily());
        boolean bold = style.isBold();
        boolean italic = style.isItalic();
        float fontSize = style.getFontSize(defaultFontSizePt);

        // Check for superscript
        String verticalAlign = style.get("vertical-align");
        if ("super".equals(verticalAlign)) {
            textRun.setSuperscript(true);
            fontSize *= 0.7f;
        }

        PDFont font = fontManager.getFont(family, bold, italic);
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

    record InlineItem(Box box, float width, float height) {}
    record Line(List<InlineItem> items, float width, float height) {}
}
