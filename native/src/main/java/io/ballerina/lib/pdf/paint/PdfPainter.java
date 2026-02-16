package io.ballerina.lib.pdf.paint;

import io.ballerina.lib.pdf.box.*;
import io.ballerina.lib.pdf.css.ComputedStyle;
import io.ballerina.lib.pdf.layout.LayoutContext;
import io.ballerina.lib.pdf.layout.PageBreaker;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * Traverses the laid-out box tree and emits PDFBox drawing commands.
 * Handles coordinate transformation: layout uses top-down Y, PDFBox uses bottom-up Y.
 */
public class PdfPainter {

    private final PdfPageManager pageManager;
    private final ImageDecoder imageDecoder;
    private final FontManager fontManager;
    private final LayoutContext layoutContext;

    public PdfPainter(PdfPageManager pageManager, ImageDecoder imageDecoder,
                      FontManager fontManager, LayoutContext layoutContext) {
        this.pageManager = pageManager;
        this.imageDecoder = imageDecoder;
        this.fontManager = fontManager;
        this.layoutContext = layoutContext;
    }

    /**
     * Paints the entire box tree across pages.
     */
    public void paint(Box root, List<PageBreaker.PageSlice> pages) throws IOException {
        for (int pageIdx = 0; pageIdx < pages.size(); pageIdx++) {
            PageBreaker.PageSlice slice = pages.get(pageIdx);
            PDPageContentStream stream = pageManager.newPage();

            // Paint all boxes, offsetting by the page slice's startY
            paintBox(root, stream, 0, 0, slice.startY(), slice.endY());
        }
        pageManager.finish();
    }

    /**
     * Recursively paints a box and its children.
     *
     * @param box      the box to paint
     * @param stream   the current page's content stream
     * @param parentX  absolute X of parent's content area (in layout coords)
     * @param parentY  absolute Y of parent's content area (in layout coords)
     * @param clipTop  top of visible region (page slice start)
     * @param clipBottom bottom of visible region (page slice end)
     */
    private void paintBox(Box box, PDPageContentStream stream,
                          float parentX, float parentY,
                          float clipTop, float clipBottom) throws IOException {

        // Calculate absolute position in layout coordinates
        float absX = parentX + box.getX() + box.getMarginLeft();
        float absY = parentY + box.getY() + box.getMarginTop();
        float boxBottom = absY + box.getBorderBoxHeight();

        // Skip if entirely outside visible region
        if (boxBottom < clipTop || absY > clipBottom) {
            return;
        }

        // Convert to PDF coordinates (add page margins)
        float pdfX = absX + layoutContext.getMarginLeft();
        float borderBoxW = box.getBorderBoxWidth();
        float borderBoxH = box.getBorderBoxHeight();

        // Paint box-shadow (rendered beneath background)
        paintBoxShadow(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint background
        paintBackground(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint borders
        paintBorders(box, stream, pdfX, absY - clipTop, borderBoxW, borderBoxH);

        // Paint content
        float contentX = absX + box.getBorderLeftWidth() + box.getPaddingLeft();
        float contentY = absY + box.getBorderTopWidth() + box.getPaddingTop();

        if (box instanceof TextRun textRun) {
            paintText(textRun, stream, pdfX + box.getBorderLeftWidth() + box.getPaddingLeft(),
                    contentY - clipTop);
        } else if (box instanceof ReplacedBox replaced) {
            paintImage(replaced, stream, pdfX + box.getBorderLeftWidth() + box.getPaddingLeft(),
                    contentY - clipTop);
        }

        // Recurse into children (use effectiveChildren to pick up inline layout results)
        for (Box child : box.getEffectiveChildren()) {
            paintBox(child, stream, contentX, contentY, clipTop, clipBottom);
        }
    }

    private void paintBackground(Box box, PDPageContentStream stream,
                                  float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) return;

        float fontSize = style.getFontSize(layoutContext.getDefaultFontSizePt());
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        String bgColor = style.getBackgroundColor();
        Color color = ColorParser.parse(bgColor);
        if (color != null && color.getAlpha() > 0) {
            float alpha = (color.getAlpha() / 255f) * style.getOpacity();
            float pdfY = toPdfY(layoutY, height);
            if (alpha < 1.0f) {
                stream.saveGraphicsState();
                applyAlpha(stream, alpha, false);
            }
            stream.setNonStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
            if (hasRadius) {
                addRoundedRectPath(stream, pdfX, pdfY, width, height, tlr, trr, brr, blr);
            } else {
                stream.addRect(pdfX, pdfY, width, height);
            }
            stream.fill();
            if (alpha < 1.0f) {
                stream.restoreGraphicsState();
            }
        }

        // Background image (base64 data URL via CSS)
        String bgImage = style.getBackgroundImage();
        if (bgImage != null && bgImage.contains("data:image")) {
            PDImageXObject image = imageDecoder.decode(bgImage);
            if (image != null) {
                float pdfY = toPdfY(layoutY, height);
                stream.drawImage(image, pdfX, pdfY, width, height);
            }
        }
    }

    /**
     * Paints a box-shadow approximation using concentric filled shapes with decreasing opacity.
     * True Gaussian blur is not available in PDF; this uses layered rects as an approximation.
     */
    private void paintBoxShadow(Box box, PDPageContentStream stream,
                                 float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) return;

        float fontSize = style.getFontSize(layoutContext.getDefaultFontSizePt());
        ComputedStyle.BoxShadow shadow = style.getBoxShadow(width, fontSize);
        if (shadow == null) return;

        Color shadowColor = ColorParser.parse(shadow.color());
        if (shadowColor == null) shadowColor = new Color(0, 0, 0, 128);

        float baseAlpha = (shadowColor.getAlpha() / 255f) * style.getOpacity();
        float pdfY = toPdfY(layoutY, height);

        // Resolve border-radius for shadow shape
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        // Approximate blur with concentric layers
        int layers = shadow.blur() > 0 ? 4 : 1;
        float blurStep = shadow.blur() / layers;

        for (int i = layers; i >= 1; i--) {
            float expand = shadow.spread() + (blurStep * i);
            float layerAlpha = baseAlpha * ((float) (layers - i + 1) / (layers + 1));

            float sx = pdfX + shadow.offsetX() - expand;
            float sy = pdfY - shadow.offsetY() - expand;
            float sw = width + expand * 2;
            float sh = height + expand * 2;

            stream.saveGraphicsState();
            applyAlpha(stream, layerAlpha, false);
            stream.setNonStrokingColor(shadowColor.getRed() / 255f,
                    shadowColor.getGreen() / 255f, shadowColor.getBlue() / 255f);

            if (hasRadius) {
                addRoundedRectPath(stream, sx, sy, sw, sh,
                        tlr + expand, trr + expand, brr + expand, blr + expand);
            } else {
                stream.addRect(sx, sy, sw, sh);
            }
            stream.fill();
            stream.restoreGraphicsState();
        }
    }

    private void paintBorders(Box box, PDPageContentStream stream,
                               float pdfX, float layoutY, float width, float height) throws IOException {
        ComputedStyle style = box.getStyle();
        if (style == null) return;

        float pdfY = toPdfY(layoutY, height);
        float opacity = style.getOpacity();

        // Check for border-radius — if present, stroke the rounded rect as a whole
        float fontSize = style.getFontSize(layoutContext.getDefaultFontSizePt());
        float tlr = style.getBorderTopLeftRadius(width, fontSize);
        float trr = style.getBorderTopRightRadius(width, fontSize);
        float brr = style.getBorderBottomRightRadius(width, fontSize);
        float blr = style.getBorderBottomLeftRadius(width, fontSize);
        boolean hasRadius = tlr > 0 || trr > 0 || brr > 0 || blr > 0;

        if (hasRadius) {
            // Use average border width for the stroked rounded rect path
            float avgBorderWidth = (box.getBorderTopWidth() + box.getBorderRightWidth()
                    + box.getBorderBottomWidth() + box.getBorderLeftWidth()) / 4f;
            if (avgBorderWidth <= 0) return;

            // Determine border color (use top border color as primary)
            boolean hasBorder = !isNoneBorderStyle(style.getBorderTopStyle())
                    || !isNoneBorderStyle(style.getBorderRightStyle())
                    || !isNoneBorderStyle(style.getBorderBottomStyle())
                    || !isNoneBorderStyle(style.getBorderLeftStyle());
            if (!hasBorder) return;

            Color color = ColorParser.parse(style.getBorderTopColor());
            if (color == null) color = ColorParser.parse(style.getBorderRightColor());
            if (color == null) color = ColorParser.parse(style.getBorderBottomColor());
            if (color == null) color = ColorParser.parse(style.getBorderLeftColor());
            if (color == null) color = Color.BLACK;

            float alpha = (color.getAlpha() / 255f) * opacity;
            if (alpha < 1.0f) { stream.saveGraphicsState(); applyAlpha(stream, alpha, true); }
            stream.setStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
            stream.setLineWidth(avgBorderWidth);
            addRoundedRectPath(stream, pdfX, pdfY, width, height, tlr, trr, brr, blr);
            stream.stroke();
            if (alpha < 1.0f) stream.restoreGraphicsState();
            return;
        }

        // Standard per-side border drawing (no radius)

        // Top border
        if (box.getBorderTopWidth() > 0 && !isNoneBorderStyle(style.getBorderTopStyle())) {
            Color color = ColorParser.parse(style.getBorderTopColor());
            if (color == null) color = Color.BLACK;
            float alpha = (color.getAlpha() / 255f) * opacity;
            if (alpha < 1.0f) { stream.saveGraphicsState(); applyAlpha(stream, alpha, true); }
            drawLine(stream, pdfX, pdfY + height, pdfX + width, pdfY + height,
                    box.getBorderTopWidth(), color);
            if (alpha < 1.0f) stream.restoreGraphicsState();
        }

        // Bottom border
        if (box.getBorderBottomWidth() > 0 && !isNoneBorderStyle(style.getBorderBottomStyle())) {
            Color color = ColorParser.parse(style.getBorderBottomColor());
            if (color == null) color = Color.BLACK;
            float alpha = (color.getAlpha() / 255f) * opacity;
            if (alpha < 1.0f) { stream.saveGraphicsState(); applyAlpha(stream, alpha, true); }
            drawLine(stream, pdfX, pdfY, pdfX + width, pdfY,
                    box.getBorderBottomWidth(), color);
            if (alpha < 1.0f) stream.restoreGraphicsState();
        }

        // Left border
        if (box.getBorderLeftWidth() > 0 && !isNoneBorderStyle(style.getBorderLeftStyle())) {
            Color color = ColorParser.parse(style.getBorderLeftColor());
            if (color == null) color = Color.BLACK;
            float alpha = (color.getAlpha() / 255f) * opacity;
            if (alpha < 1.0f) { stream.saveGraphicsState(); applyAlpha(stream, alpha, true); }
            drawLine(stream, pdfX, pdfY, pdfX, pdfY + height,
                    box.getBorderLeftWidth(), color);
            if (alpha < 1.0f) stream.restoreGraphicsState();
        }

        // Right border
        if (box.getBorderRightWidth() > 0 && !isNoneBorderStyle(style.getBorderRightStyle())) {
            Color color = ColorParser.parse(style.getBorderRightColor());
            if (color == null) color = Color.BLACK;
            float alpha = (color.getAlpha() / 255f) * opacity;
            if (alpha < 1.0f) { stream.saveGraphicsState(); applyAlpha(stream, alpha, true); }
            drawLine(stream, pdfX + width, pdfY, pdfX + width, pdfY + height,
                    box.getBorderRightWidth(), color);
            if (alpha < 1.0f) stream.restoreGraphicsState();
        }
    }

    private void paintText(TextRun textRun, PDPageContentStream stream,
                            float pdfX, float layoutY) throws IOException {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) return;

        PDFont font = textRun.getFont();
        float fontSize = textRun.getFontSize();
        if (font == null) {
            font = fontManager.getDefaultFont();
        }
        if (fontSize <= 0) fontSize = layoutContext.getDefaultFontSizePt();

        // Sanitize text for PDFBox (remove characters the font can't encode)
        text = sanitizeText(text, font);
        if (text.isEmpty()) return;

        // Text color
        ComputedStyle style = textRun.getStyle();
        Color textColor = Color.BLACK;
        if (style != null && style.getColor() != null) {
            Color parsed = ColorParser.parse(style.getColor());
            if (parsed != null) textColor = parsed;
        }

        // Calculate baseline position
        float ascent = fontManager.getAscent(font, fontSize);
        float baselineY = layoutY + ascent;

        // Superscript: shift baseline up
        if (textRun.isSuperscript()) {
            baselineY -= fontSize * 0.4f;
        }
        // Subscript: shift baseline down
        if (textRun.isSubscript()) {
            baselineY += fontSize * 0.2f;
        }

        float pdfY = toPdfYBaseline(baselineY);

        // Resolve letter-spacing and word-spacing
        float letterSpacing = 0;
        float wordSpacing = 0;
        if (style != null) {
            letterSpacing = style.getLetterSpacing(fontSize);
            wordSpacing = style.getWordSpacing(fontSize);
        }

        // Apply opacity
        float textAlpha = (textColor.getAlpha() / 255f) * (style != null ? style.getOpacity() : 1.0f);
        if (textAlpha < 1.0f) {
            stream.saveGraphicsState();
            applyAlpha(stream, textAlpha, false);
        }

        stream.beginText();
        stream.setFont(font, fontSize);
        stream.setNonStrokingColor(textColor.getRed() / 255f, textColor.getGreen() / 255f, textColor.getBlue() / 255f);
        if (letterSpacing != 0) stream.setCharacterSpacing(letterSpacing);
        if (wordSpacing != 0) stream.setWordSpacing(wordSpacing);
        stream.newLineAtOffset(pdfX, pdfY);
        stream.showText(text);
        if (letterSpacing != 0) stream.setCharacterSpacing(0);
        if (wordSpacing != 0) stream.setWordSpacing(0);
        stream.endText();

        if (textAlpha < 1.0f) {
            stream.restoreGraphicsState();
        }

        // Text decoration (underline, line-through)
        if (style != null) {
            String decoration = style.get("text-decoration");
            if (decoration != null && !"none".equals(decoration)) {
                float textWidth = fontManager.measureText(text, font, fontSize);
                float thickness = Math.max(fontSize / 20f, 0.5f);
                float descent = fontManager.getDescent(font, fontSize);

                if (decoration.contains("underline")) {
                    float lineY = toPdfYBaseline(baselineY + descent * 0.3f);
                    drawLine(stream, pdfX, lineY, pdfX + textWidth, lineY, thickness, textColor);
                }
                if (decoration.contains("line-through")) {
                    float lineY = toPdfYBaseline(baselineY - ascent * 0.35f);
                    drawLine(stream, pdfX, lineY, pdfX + textWidth, lineY, thickness, textColor);
                }
            }
        }
    }

    private void paintImage(ReplacedBox replaced, PDPageContentStream stream,
                             float pdfX, float layoutY) throws IOException {
        String src = replaced.getSrc();
        if (src == null) return;

        PDImageXObject image = imageDecoder.decode(src);
        if (image == null) return;

        float width = replaced.getIntrinsicWidth();
        float height = replaced.getIntrinsicHeight();
        float pdfY = toPdfY(layoutY, height);

        stream.drawImage(image, pdfX, pdfY, width, height);
    }

    /**
     * Converts a layout Y coordinate (top-down) to PDFBox Y (bottom-up).
     * For rectangles: pdfY is the bottom-left corner.
     */
    private float toPdfY(float layoutY, float height) {
        return layoutContext.getPageHeight() - layoutContext.getMarginTop() - layoutY - height;
    }

    /**
     * Converts a layout baseline Y coordinate (top-down) to PDFBox Y (bottom-up).
     */
    private float toPdfYBaseline(float layoutBaselineY) {
        return layoutContext.getPageHeight() - layoutContext.getMarginTop() - layoutBaselineY;
    }

    private void drawLine(PDPageContentStream stream, float x1, float y1, float x2, float y2,
                           float lineWidth, Color color) throws IOException {
        stream.setStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        stream.setLineWidth(lineWidth);
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private boolean isNoneBorderStyle(String style) {
        return style == null || "none".equals(style) || "hidden".equals(style);
    }

    /**
     * Adds a rounded rectangle path using Bezier curves.
     * Kappa constant approximates a quarter circle with a cubic Bezier.
     * PDFBox Y is bottom-up: (x, y) is the bottom-left corner.
     */
    private void addRoundedRectPath(PDPageContentStream stream, float x, float y,
                                     float w, float h, float tlr, float trr,
                                     float brr, float blr) throws IOException {
        float k = 0.552284749831f; // kappa for circle approximation

        // Clamp radii to half the box dimension
        float maxR = Math.min(w, h) / 2f;
        tlr = Math.min(tlr, maxR);
        trr = Math.min(trr, maxR);
        brr = Math.min(brr, maxR);
        blr = Math.min(blr, maxR);

        // Start at bottom-left, just after the BL radius (moving clockwise in PDF coords)
        stream.moveTo(x + blr, y);

        // Bottom edge → bottom-right corner
        stream.lineTo(x + w - brr, y);
        if (brr > 0) {
            stream.curveTo(x + w - brr + brr * k, y,
                           x + w, y + brr - brr * k,
                           x + w, y + brr);
        }

        // Right edge → top-right corner
        stream.lineTo(x + w, y + h - trr);
        if (trr > 0) {
            stream.curveTo(x + w, y + h - trr + trr * k,
                           x + w - trr + trr * k, y + h,
                           x + w - trr, y + h);
        }

        // Top edge → top-left corner
        stream.lineTo(x + tlr, y + h);
        if (tlr > 0) {
            stream.curveTo(x + tlr - tlr * k, y + h,
                           x, y + h - tlr + tlr * k,
                           x, y + h - tlr);
        }

        // Left edge → bottom-left corner
        stream.lineTo(x, y + blr);
        if (blr > 0) {
            stream.curveTo(x, y + blr - blr * k,
                           x + blr - blr * k, y,
                           x + blr, y);
        }

        stream.closePath();
    }

    private void applyAlpha(PDPageContentStream stream, float alpha, boolean stroking) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        if (stroking) {
            gs.setStrokingAlphaConstant(alpha);
        } else {
            gs.setNonStrokingAlphaConstant(alpha);
        }
        stream.setGraphicsStateParameters(gs);
    }

    /**
     * Removes characters that the font can't encode (e.g. control chars, missing glyphs).
     */
    private String sanitizeText(String text, PDFont font) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (Exception e) {
                // Skip unencodable characters; replace with space for readability
                if (!Character.isISOControl(c)) {
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }
}
