package io.ballerina.lib.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class HtmlToPdfConverterTest {

    private HtmlPreprocessor preprocessor;
    private HtmlToPdfConverter converter;

    @BeforeEach
    void setUp() {
        preprocessor = new HtmlPreprocessor();
        converter = new HtmlToPdfConverter();
    }

    @AfterAll
    static void cleanup() throws IOException {
        Path outputDir = Path.of("output");
        if (Files.exists(outputDir)) {
            try (var files = Files.walk(outputDir)) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    // --- Helper ---

    private String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private int pageCount(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return doc.getNumberOfPages();
        }
    }

    // --- Unit tests ---

    @Test
    void convertsMinimalHtmlToPdf() throws Exception {
        String html = "<html><head><style>body { font-family: 'Liberation Sans'; }</style></head>"
                + "<body><p>Hello World</p></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Hello World"), "PDF should contain rendered text 'Hello World', got: " + text);
    }

    @Test
    void producesValidPdfHeader() throws Exception {
        String html = "<html><body><p>Test</p></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String header = new String(pdf, 0, Math.min(5, pdf.length));
        assertEquals("%PDF-", header, "PDF should start with %PDF- header");
    }

    @Test
    void rendersTableContent() throws Exception {
        String html = "<html><body>"
                + "<table><tr><td>Cell 1</td><td>Cell 2</td></tr></table>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Cell 1"), "PDF should contain 'Cell 1', got: " + text);
        assertTrue(text.contains("Cell 2"), "PDF should contain 'Cell 2', got: " + text);
    }

    @Test
    void rendersBase64Image() throws Exception {
        // 10x10 red pixel PNG
        String html = "<html><body>"
                + "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAIAAAACUFjqAAAAEklEQVR4nGP4z8CAB+GTG8HSALfKY52fTcuYAAAAAElFTkSuQmCC\" />"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        assertTrue(pdf.length > 0, "PDF with base64 image should not be empty");
        assertEquals(1, pageCount(pdf), "PDF with small image should be 1 page");
    }

    @Test
    void rendersMultiplePages() throws Exception {
        StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            html.append("<p>Line ").append(i).append(" of the document to force page breaks.</p>");
        }
        html.append("</body></html>");

        org.w3c.dom.Document doc = preprocessor.preprocess(html.toString());
        byte[] pdf = converter.convertToPdf(doc);

        int pages = pageCount(pdf);
        assertTrue(pages > 1, "200 paragraphs should span multiple pages, got: " + pages);

        String text = extractText(pdf);
        assertTrue(text.contains("Line 0"), "First line should be rendered");
        assertTrue(text.contains("Line 199"), "Last line should be rendered");
    }

    @Test
    void smokeTestBasicHtml() throws Exception {
        Path inputPath = Path.of("src/test/resources/smoke-test.html");
        if (!Files.exists(inputPath)) {
            fail("Smoke test input not found: " + inputPath.toAbsolutePath());
        }

        String rawHtml = Files.readString(inputPath, StandardCharsets.UTF_8);
        org.w3c.dom.Document doc = preprocessor.preprocess(rawHtml);
        byte[] pdf = converter.convertToPdf(doc);

        // Write output for visual inspection
        Path outputPath = Path.of("output/smoke-test.pdf");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, pdf);
        System.out.println("  Wrote: " + outputPath.toAbsolutePath() + " (" + pdf.length + " bytes)");

        String text = extractText(pdf);
        assertTrue(text.contains("SMOKE TEST"), "Should contain header text, got: " + text);
        assertTrue(text.contains("John Doe"), "Should contain table data 'John Doe', got: " + text);
        assertTrue(text.contains("Credit Card"), "Should contain 'Credit Card', got: " + text);
        assertTrue(text.contains("750"), "Should contain score '750', got: " + text);
    }

}
