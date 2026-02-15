package io.ballerina.lib.pdf;

import io.ballerina.lib.pdf.box.BlockBox;
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
import java.util.List;

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
    public byte[] convertToPdf(org.w3c.dom.Document document) throws Exception {
        return convertToPdf(document, new ConverterOptions());
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF bytes with custom options.
     */
    public byte[] convertToPdf(org.w3c.dom.Document document, ConverterOptions options) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        convertToPdf(document, baos, options);
        return baos.toByteArray();
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF, writing to the given stream.
     */
    public void convertToPdf(org.w3c.dom.Document document, OutputStream outputStream) throws Exception {
        convertToPdf(document, outputStream, new ConverterOptions());
    }

    /**
     * Convert a preprocessed W3C DOM Document to PDF with custom options, writing to the given stream.
     */
    public void convertToPdf(org.w3c.dom.Document document, OutputStream outputStream,
                              ConverterOptions options) throws Exception {
        try (PDDocument pdfDoc = new PDDocument()) {

            // 1. Load fonts
            FontManager fontManager = new FontManager();
            fontManager.loadFonts(pdfDoc);
            if (options.getCustomFonts() != null && !options.getCustomFonts().isEmpty()) {
                fontManager.loadCustomFonts(pdfDoc, options.getCustomFonts());
            }

            // 2. Parse CSS from <style> blocks
            CssParser cssParser = new CssParser();
            CssStylesheet stylesheet = cssParser.parse(document);

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

            // 8. Paint to PDF
            ImageDecoder imageDecoder = new ImageDecoder(pdfDoc);
            PdfPageManager pageManager = new PdfPageManager(pdfDoc, layoutContext);
            PdfPainter painter = new PdfPainter(pageManager, imageDecoder, fontManager, layoutContext);
            painter.paint(root, pages);

            // 9. Write PDF to output stream
            pdfDoc.save(outputStream);
        }
    }
}
