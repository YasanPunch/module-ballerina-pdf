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

package io.ballerina.lib.pdf;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.ImageIO;

/**
 * PDF reading operations: text extraction and page-to-image conversion.
 * <p>
 * Provides core logic for loading, validating, and processing PDF documents.
 * Bridge methods in {@link Native} delegate to these methods.
 */
final class PdfReader {

    private static final int RENDER_DPI = 400;

    private PdfReader() {
    }

    // --- Conversion operations ---

    /**
     * Converts each page of a PDF document to a Base64-encoded PNG image.
     */
    static BString[] toImages(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        BString[] result = new BString[pageCount];

        for (int i = 0; i < pageCount; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            result[i] = StringUtils.fromString(Base64.getEncoder().encodeToString(baos.toByteArray()));
        }
        return result;
    }

    /**
     * Extracts text content from each page of a PDF document.
     */
    static BString[] extractText(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        int pageCount = document.getNumberOfPages();
        BString[] result = new BString[pageCount];

        for (int i = 1; i <= pageCount; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);
            result[i - 1] = StringUtils.fromString(stripper.getText(document));
        }
        return result;
    }

    // --- Input loading + validation ---

    /**
     * Loads a PDF from a file path after validating existence and extension.
     */
    static PDDocument loadFromFile(String filePath) throws IOException {
        if (!filePath.toLowerCase().endsWith(".pdf")) {
            throw new IOException("Unsupported file type. Expected .pdf file: " + filePath);
        }
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }
        return Loader.loadPDF(file);
    }

    /**
     * Loads a PDF from a URL after validating accessibility and content type.
     */
    static PDDocument loadFromUrl(String url) throws IOException {
        try {
            URI uri = new URI(url);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                throw new IOException("URL not accessible (HTTP " + responseCode + "): " + url);
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.contains("application/pdf")) {
                connection.disconnect();
                throw new IOException("URL does not point to a PDF (content-type: " + contentType + "): " + url);
            }

            try (InputStream in = connection.getInputStream()) {
                return Loader.loadPDF(new RandomAccessReadBuffer(in));
            } finally {
                connection.disconnect();
            }
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + url, e);
        }
    }

    /**
     * Loads a PDF from raw bytes after validating the %PDF header.
     */
    static PDDocument loadFromBytes(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 4) {
            throw new IOException("Invalid PDF data: input is empty or too short");
        }
        String header = new String(bytes, 0, Math.min(bytes.length, 4), StandardCharsets.UTF_8);
        if (!header.startsWith("%PDF")) {
            throw new IOException("Invalid PDF data: missing %PDF header");
        }
        return Loader.loadPDF(bytes);
    }
}
