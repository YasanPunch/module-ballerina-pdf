// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package io.ballerina.lib.pdf;

import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfReaderTest {

    private HtmlPreprocessor preprocessor;
    private HtmlToPdfConverter converter;

    @BeforeEach
    void setUp() {
        preprocessor = new HtmlPreprocessor();
        converter = new HtmlToPdfConverter();
    }

    private byte[] generatePdf(String html) throws Exception {
        ConverterOptions opts = new ConverterOptions(
                ConverterOptions.DEFAULT_FONT_SIZE_PT,
                ConverterOptions.A4_WIDTH, ConverterOptions.A4_HEIGHT,
                ConverterOptions.DEFAULT_MARGIN, ConverterOptions.DEFAULT_MARGIN,
                ConverterOptions.DEFAULT_MARGIN, ConverterOptions.DEFAULT_MARGIN,
                null, true, null);
        org.w3c.dom.Document doc = preprocessor.preprocess(html, opts);
        return converter.convertToPdf(doc, opts);
    }

    // --- Text extraction tests ---

    @Test
    void extractTextFromGeneratedPdf() throws Exception {
        byte[] pdf = generatePdf("<p>Hello World</p>");
        try (PDDocument doc = PdfReader.loadFromBytes(pdf)) {
            BString[] pages = PdfReader.extractText(doc);
            assertEquals(1, pages.length, "Expected one page");
            assertTrue(pages[0].getValue().contains("Hello World"),
                    "Extracted text should contain 'Hello World'");
        }
    }

    @Test
    void extractTextPreservesContent() throws Exception {
        byte[] pdf = generatePdf("<p>First paragraph</p><p>Second paragraph</p>");
        try (PDDocument doc = PdfReader.loadFromBytes(pdf)) {
            BString[] pages = PdfReader.extractText(doc);
            assertTrue(pages.length >= 1);
            String text = pages[0].getValue();
            assertTrue(text.contains("First paragraph"), "Should contain first paragraph");
            assertTrue(text.contains("Second paragraph"), "Should contain second paragraph");
        }
    }

    // --- Image conversion tests ---

    @Test
    void toImagesFromGeneratedPdf() throws Exception {
        byte[] pdf = generatePdf("<h1>Image Test</h1>");
        try (PDDocument doc = PdfReader.loadFromBytes(pdf)) {
            BString[] images = PdfReader.toImages(doc);
            assertEquals(1, images.length, "Expected one image (one page)");
            String base64 = images[0].getValue();
            assertTrue(base64.length() > 100, "Base64 image should be non-trivial");
        }
    }

    // --- Validation tests ---

    @Test
    void loadFromBytesRejectsNonPdf() {
        byte[] notPdf = "This is not a PDF".getBytes();
        IOException ex = assertThrows(IOException.class, () -> PdfReader.loadFromBytes(notPdf));
        assertTrue(ex.getMessage().contains("%PDF"), "Error should mention %PDF header");
    }

    @Test
    void loadFromBytesRejectsEmptyInput() {
        IOException ex = assertThrows(IOException.class, () -> PdfReader.loadFromBytes(new byte[0]));
        assertTrue(ex.getMessage().contains("empty or too short"));
    }

    @Test
    void loadFromFileRejectsNonExistent() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.loadFromFile("/nonexistent/path/file.pdf"));
        assertTrue(ex.getMessage().contains("File not found"));
    }

    @Test
    void loadFromFileRejectsNonPdfExtension() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.loadFromFile("test.txt"));
        assertTrue(ex.getMessage().contains("Unsupported file type"));
    }

    @Test
    void loadFromUrlRejectsInvalidUrl() {
        assertThrows(IOException.class,
                () -> PdfReader.loadFromUrl("not a valid url"));
    }
}
