package io.ballerina.lib.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

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
    void rendersTextDecoration() throws Exception {
        String html = "<html><body>"
                + "<p><u>underlined text</u></p>"
                + "<p><s>struck through</s></p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("underlined text"), "PDF should contain underlined text");
        assertTrue(text.contains("struck through"), "PDF should contain struck-through text");
    }

    @Test
    void rendersSubscript() throws Exception {
        String html = "<html><body>"
                + "<p>H<sub>2</sub>O is water</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("2"), "PDF should contain subscript '2'");
        assertTrue(text.contains("water"), "PDF should contain 'water'");
    }

    @Test
    void rendersTextTransform() throws Exception {
        String html = "<html><head><style>"
                + ".upper { text-transform: uppercase; }"
                + ".lower { text-transform: lowercase; }"
                + ".cap { text-transform: capitalize; }"
                + "</style></head><body>"
                + "<p class=\"upper\">hello world</p>"
                + "<p class=\"lower\">HELLO WORLD</p>"
                + "<p class=\"cap\">hello world</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("HELLO WORLD"), "uppercase transform should produce 'HELLO WORLD'");
        assertTrue(text.contains("hello world"), "lowercase transform should produce 'hello world'");
        assertTrue(text.contains("Hello World"), "capitalize transform should produce 'Hello World'");
    }

    @Test
    void rendersTableVerticalAlign() throws Exception {
        String html = "<html><body>"
                + "<table>"
                + "<tr style=\"height: 100px;\">"
                + "<td style=\"vertical-align: top;\">Top</td>"
                + "<td style=\"vertical-align: middle;\">Middle</td>"
                + "<td style=\"vertical-align: bottom;\">Bottom</td>"
                + "</tr>"
                + "</table>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Top"), "Should contain 'Top'");
        assertTrue(text.contains("Middle"), "Should contain 'Middle'");
        assertTrue(text.contains("Bottom"), "Should contain 'Bottom'");
    }

    @Test
    void rendersTableWithValignAttribute() throws Exception {
        String html = "<html><body>"
                + "<table>"
                + "<tr style=\"height: 80px;\">"
                + "<td valign=\"top\">Top aligned</td>"
                + "<td valign=\"middle\">Middle aligned</td>"
                + "</tr>"
                + "</table>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Top aligned"), "Should render valign=top text");
        assertTrue(text.contains("Middle aligned"), "Should render valign=middle text");
    }

    @Test
    void rendersInlineBlock() throws Exception {
        String html = "<html><body>"
                + "<p>Before <span style=\"display: inline-block; border: 1px solid black; padding: 5px;\">"
                + "inline block content</span> after</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Before"), "Should contain 'Before'");
        assertTrue(text.contains("inline block content"), "Should contain inline-block content");
        assertTrue(text.contains("after"), "Should contain 'after'");
    }

    @Test
    void rendersMultipleInlineBlocks() throws Exception {
        String html = "<html><head><style>"
                + ".box { display: inline-block; padding: 10px; border: 1px solid #999; }"
                + "</style></head><body>"
                + "<div><span class=\"box\">Box A</span> <span class=\"box\">Box B</span> "
                + "<span class=\"box\">Box C</span></div>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Box A"), "Should contain 'Box A'");
        assertTrue(text.contains("Box B"), "Should contain 'Box B'");
        assertTrue(text.contains("Box C"), "Should contain 'Box C'");
    }

    @Test
    void rendersWithChildCombinatorCss() throws Exception {
        String html = "<html><head><style>"
                + "div > p { font-size: 20px; }"
                + "</style></head>"
                + "<body><div><p>Direct child</p><span><p>Nested child</p></span></div></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Direct child"), "PDF should contain styled direct child text");
        assertTrue(text.contains("Nested child"), "PDF should contain nested text");
    }

    @Test
    void rendersWithCustomFont() throws Exception {
        // Load a bundled Liberation font as if it were a user-supplied custom font
        byte[] fontBytes;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/LiberationSans-Regular.ttf")) {
            assertNotNull(is, "Liberation Sans Regular should be on classpath");
            fontBytes = is.readAllBytes();
        }

        Map<String, byte[]> customFonts = Map.of("TestFont", fontBytes);
        ConverterOptions options = new ConverterOptions(
                12f, ConverterOptions.A4_WIDTH, ConverterOptions.A4_HEIGHT,
                36f, 36f, 36f, 36f, null, true, customFonts);

        String html = "<html><head><style>body { font-family: 'TestFont'; }</style></head>"
                + "<body><p>Custom font text</p></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc, options);

        String text = extractText(pdf);
        assertTrue(text.contains("Custom font text"),
                "PDF should contain text rendered with custom font, got: " + text);
    }

    @Test
    void rendersPositionRelative() throws Exception {
        String html = "<html><body>"
                + "<p>First paragraph</p>"
                + "<p style=\"position: relative; top: 20px; left: 10px;\">Offset paragraph</p>"
                + "<p>Third paragraph</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("First paragraph"), "Should contain 'First paragraph'");
        assertTrue(text.contains("Offset paragraph"), "Should contain 'Offset paragraph'");
        assertTrue(text.contains("Third paragraph"), "Should contain 'Third paragraph'");
    }

    @Test
    void rendersPositionAbsolute() throws Exception {
        String html = "<html><body>"
                + "<div style=\"position: relative; height: 100px;\">"
                + "<p>Container text</p>"
                + "<div style=\"position: absolute; top: 10px; right: 10px;\">Badge</div>"
                + "</div>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Container text"), "Should contain 'Container text'");
        assertTrue(text.contains("Badge"), "Should contain absolute-positioned 'Badge'");
    }

    @Test
    void absolutePositionDoesNotAffectFlow() throws Exception {
        String html = "<html><body>"
                + "<div style=\"position: relative;\">"
                + "<p>Before absolute</p>"
                + "<div style=\"position: absolute; top: 0; left: 0;\">Floating overlay</div>"
                + "<p>After absolute</p>"
                + "</div>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Before absolute"), "Should contain 'Before absolute'");
        assertTrue(text.contains("Floating overlay"), "Should contain 'Floating overlay'");
        assertTrue(text.contains("After absolute"), "Should contain 'After absolute'");
    }

    @Test
    void rendersNestedTable() throws Exception {
        String html = "<html><body>"
                + "<table><tr><td>"
                + "<table><tr><td>Inner Cell 1</td><td>Inner Cell 2</td></tr></table>"
                + "</td><td>Outer Cell</td></tr></table>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Inner Cell 1"), "Should contain inner table 'Inner Cell 1', got: " + text);
        assertTrue(text.contains("Inner Cell 2"), "Should contain inner table 'Inner Cell 2', got: " + text);
        assertTrue(text.contains("Outer Cell"), "Should contain 'Outer Cell', got: " + text);
    }

    @Test
    void rendersOrderedList() throws Exception {
        String html = "<html><body>"
                + "<ol><li>First item</li><li>Second item</li><li>Third item</li></ol>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("1."), "Should contain '1.' numbering, got: " + text);
        assertTrue(text.contains("First item"), "Should contain 'First item', got: " + text);
        assertTrue(text.contains("2."), "Should contain '2.' numbering, got: " + text);
        assertTrue(text.contains("Second item"), "Should contain 'Second item', got: " + text);
        assertTrue(text.contains("3."), "Should contain '3.' numbering, got: " + text);
        assertTrue(text.contains("Third item"), "Should contain 'Third item', got: " + text);
    }

    @Test
    void rendersUnorderedList() throws Exception {
        String html = "<html><body>"
                + "<ul><li>Bullet A</li><li>Bullet B</li></ul>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Bullet A"), "Should contain 'Bullet A', got: " + text);
        assertTrue(text.contains("Bullet B"), "Should contain 'Bullet B', got: " + text);
    }

    @Test
    void rendersCenterAlignedText() throws Exception {
        String html = "<html><head><style>"
                + ".center { text-align: center; }"
                + "</style></head><body>"
                + "<p class=\"center\">Centered paragraph</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Centered paragraph"),
                "Should contain centered text 'Centered paragraph', got: " + text);
    }

    @Test
    void rendersMinWidth() throws Exception {
        String html = "<html><head><style>"
                + ".minbox { min-width: 200px; background-color: #eee; }"
                + "</style></head><body>"
                + "<div class=\"minbox\">Min width box content</div>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Min width box content"),
                "Should render min-width constrained element, got: " + text);
    }

    @Test
    void rendersFontShorthand() throws Exception {
        String html = "<html><head><style>"
                + "#styled { font: bold 14px 'Liberation Sans'; }"
                + "</style></head><body>"
                + "<p id=\"styled\">Bold fourteen pixel text</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Bold fourteen pixel text"),
                "Should render font shorthand styled text, got: " + text);
    }

    @Test
    void rendersBackgroundImageElement() throws Exception {
        // 1x1 red PNG as base64 background-image
        String html = "<html><head><style>"
                + "#bgimg { background-image: url(data:image/png;base64,"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAH"
                + "ggJ/PchI7wAAAABJRU5ErkJggg==); width: 300px; height: 100px; }"
                + "</style></head><body>"
                + "<div id=\"bgimg\">Over image</div>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        assertTrue(pdf.length > 0, "PDF with background-image should not be empty");
        String text = extractText(pdf);
        assertTrue(text.contains("Over image"),
                "Should render text over background image, got: " + text);
    }

    @Test
    void handlesEmptyBody() throws Exception {
        String html = "<html><body></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        assertTrue(pdf.length > 0, "Empty body should still produce a valid PDF");
        String header = new String(pdf, 0, Math.min(5, pdf.length));
        assertEquals("%PDF-", header, "Empty body PDF should have valid header");
        assertEquals(1, pageCount(pdf), "Empty body should produce exactly 1 page");
    }

    @Test
    void rendersCssSpecificityPrecedence() throws Exception {
        String html = "<html><head><style>"
                + "p { color: green; }"
                + ".highlight { color: blue; }"
                + "#unique { color: red; }"
                + "</style></head><body>"
                + "<p id=\"unique\" class=\"highlight\">Specificity test</p>"
                + "</body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        byte[] pdf = converter.convertToPdf(doc);

        String text = extractText(pdf);
        assertTrue(text.contains("Specificity test"),
                "Should render text regardless of specificity resolution, got: " + text);
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
