package io.ballerina.lib.pdf;

import io.ballerina.lib.pdf.box.BlockBox;
import io.ballerina.lib.pdf.box.Box;
import io.ballerina.lib.pdf.box.BoxTreeBuilder;
import io.ballerina.lib.pdf.css.CssParser;
import io.ballerina.lib.pdf.css.CssStylesheet;
import io.ballerina.lib.pdf.css.StyleResolver;
import io.ballerina.lib.pdf.layout.BlockFormattingContext;
import io.ballerina.lib.pdf.layout.LayoutContext;
import io.ballerina.lib.pdf.layout.PageBreaker;
import io.ballerina.lib.pdf.paint.*;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a preprocessed W3C DOM Document to PDF using Apache PDFBox.
 * <p>
 * Pipeline: DOM → CSS parsing → style resolution → box tree → layout → page breaking → painting.
 * <p>
 * A new PDDocument is created for every conversion (single-use, not thread-safe).
 */
public class HtmlToPdfConverter {

    /**
     * Convert a preprocessed W3C DOM Document to PDF bytes.
     */
    public byte[] parseHtml(org.w3c.dom.Document document) throws Exception {
        return parseHtml(document, new ConverterOptions());
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF bytes with custom options.
     */
    public byte[] parseHtml(org.w3c.dom.Document document, ConverterOptions options) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parseHtml(document, baos, options);
        return baos.toByteArray();
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF, writing to the given stream.
     */
    public void parseHtml(org.w3c.dom.Document document, OutputStream outputStream) throws Exception {
        parseHtml(document, outputStream, new ConverterOptions());
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF with custom options, writing to the given stream.
     */
    public void parseHtml(org.w3c.dom.Document document, OutputStream outputStream,
                              ConverterOptions options) throws Exception {
        try (PDDocument pdfDoc = new PDDocument()) {

            // 1. Load fonts
            FontManager fontManager = new FontManager();
            fontManager.loadFonts(pdfDoc);
            if (options.getCustomFonts() != null && !options.getCustomFonts().isEmpty()) {
                fontManager.loadCustomFonts(pdfDoc, options.getCustomFonts());
            }

            // 2. Parse CSS from <style> blocks + additionalCss
            CssParser cssParser = new CssParser();
            CssStylesheet stylesheet = cssParser.parse(document, options.getAdditionalCss());

            // 3. Create layout context with page dimensions from @page rule
            LayoutContext layoutContext = new LayoutContext(fontManager, options);
            layoutContext.configureFromPageRule(stylesheet.getPageSize(), stylesheet.getPageMargin());

            // 4. Resolve styles for all elements
            StyleResolver styleResolver = new StyleResolver(stylesheet);

            // 5. Build box tree from DOM + styles
            BoxTreeBuilder boxTreeBuilder = new BoxTreeBuilder(styleResolver);
            BlockBox root = boxTreeBuilder.build(document);

            // 6. Layout: compute positions and sizes
            BlockFormattingContext bfc = new BlockFormattingContext(layoutContext);
            bfc.layout(root);

            // 7. Page breaking
            PageBreaker pageBreaker = new PageBreaker();
            List<PageBreaker.PageSlice> pages = pageBreaker.computePages(
                    root, layoutContext.getContentHeight());

            // 7b. Scale to fit maxPages if needed
            float scale = 1.0f;
            Integer maxPages = options.getMaxPages();
            if (maxPages != null && pages.size() > maxPages) {
                float totalHeight = computeVisualHeight(root);
                float targetHeight = maxPages * layoutContext.getContentHeight();
                scale = targetHeight / totalHeight;
                // Re-slice into maxPages even pages
                float sliceHeight = totalHeight / maxPages;
                pages = new ArrayList<>();
                for (int i = 0; i < maxPages; i++) {
                    pages.add(new PageBreaker.PageSlice(i * sliceHeight, (i + 1) * sliceHeight));
                }
            }

            // 7c. Build anchor map for internal links (#fragment → page position)
            Map<String, float[]> anchorMap = new HashMap<>();
            collectAnchors(root, pages, layoutContext, anchorMap);

            // 8. Paint to PDF
            ImageDecoder imageDecoder = new ImageDecoder(pdfDoc);
            PdfPageManager pageManager = new PdfPageManager(pdfDoc, layoutContext);
            PdfPainter painter = new PdfPainter(pageManager, imageDecoder, fontManager, layoutContext);
            painter.setAnchorMap(anchorMap);
            painter.paint(root, pages, scale);

            // 8b. Resolve internal anchor links (requires all pages to exist)
            painter.resolveInternalLinks(pageManager);

            // 9. Write PDF to output stream
            pdfDoc.save(outputStream);
        }
    }

    /**
     * Walks the box tree collecting elements with id attributes and their final page positions.
     * Used to resolve internal anchor links (href="#id") to PDF destinations.
     * Each entry maps id → {pageIndex, pdfTop} where pdfTop is in PDF coordinates (bottom-up).
     */
    private void collectAnchors(Box box, List<PageBreaker.PageSlice> pages,
                                 LayoutContext ctx, Map<String, float[]> map) {
        if (box.getId() != null) {
            float absY = box.getAbsoluteY();
            for (int i = 0; i < pages.size(); i++) {
                if (absY < pages.get(i).endY() || i == pages.size() - 1) {
                    float layoutYOnPage = absY - pages.get(i).startY();
                    float pdfTop = ctx.getPageHeight() - ctx.getMarginTop() - layoutYOnPage;
                    map.put(box.getId(), new float[]{i, pdfTop});
                    break;
                }
            }
        }
        for (Box child : box.getEffectiveChildren()) {
            collectAnchors(child, pages, ctx, map);
        }
    }

    /**
     * Computes the actual visual height of the root box by examining child positions.
     * When CSS parent-child margin collapsing is active, the root's content height may not
     * include the first child's collapsed top margin, but the painter still offsets children
     * by their margin values. This method computes the true extent that the painter will render.
     */
    private float computeVisualHeight(Box root) {
        float maxChildBottom = 0;
        for (Box child : root.getEffectiveChildren()) {
            float childTop = child.getY() + child.getMarginTop();
            float childBottom = childTop + child.getBorderBoxHeight() + child.getMarginBottom();
            maxChildBottom = Math.max(maxChildBottom, childBottom);
        }
        return root.getBorderTopWidth() + root.getPaddingTop()
                + maxChildBottom
                + root.getPaddingBottom() + root.getBorderBottomWidth();
    }
}
