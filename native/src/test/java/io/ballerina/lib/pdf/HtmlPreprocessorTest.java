package io.ballerina.lib.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HtmlPreprocessorTest {

    private HtmlPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new HtmlPreprocessor();
    }

    @Test
    void removesTableWidthAttributes() {
        String html = "<html><body><table width=\"1000\"><tr><td>Cell</td></tr></table></body></html>";
        String result = preprocessor.preprocessToString(html);
        assertFalse(result.contains("width=\"1000\""), "Hard-coded table width should be removed");
    }

    @Test
    void outputsValidXhtml() throws Exception {
        String html = "<html><body><div /><br><p>Test<p>Another</body></html>";
        String result = preprocessor.preprocessToString(html);

        // Valid XML should parse without exceptions
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        assertDoesNotThrow(() ->
                builder.parse(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8))),
                "Preprocessed output should be valid XML"
        );
    }

    @Test
    void producesW3cDomDocument() {
        String html = "<html><body><p>Hello</p></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        assertNotNull(doc, "W3C DOM document should not be null");
        assertNotNull(doc.getDocumentElement(), "Document should have a root element");
    }

    @Test
    void handlesEmptyInput() {
        String html = "";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        assertNotNull(doc, "Should handle empty input without exceptions");
    }
}
