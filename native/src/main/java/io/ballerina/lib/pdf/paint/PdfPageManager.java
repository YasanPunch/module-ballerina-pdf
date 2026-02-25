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

package io.ballerina.lib.pdf.paint;

import io.ballerina.lib.pdf.layout.LayoutContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages PDFBox PDDocument, PDPage, and PDPageContentStream lifecycle.
 * Creates pages on demand and ensures content streams are properly closed.
 */
public class PdfPageManager {

    private final PDDocument document;
    private final LayoutContext layoutContext;
    private final List<PDPage> pages = new ArrayList<>();
    private PDPageContentStream currentStream;
    private int currentPageIndex = -1;

    public PdfPageManager(PDDocument document, LayoutContext layoutContext) {
        this.document = document;
        this.layoutContext = layoutContext;
    }

    /**
     * Creates a new page and makes it current. Returns the content stream.
     */
    public PDPageContentStream newPage() throws IOException {
        closeCurrentStream();

        PDRectangle rect = new PDRectangle(layoutContext.getPageWidth(), layoutContext.getPageHeight());
        PDPage page = new PDPage(rect);
        document.addPage(page);
        pages.add(page);
        currentPageIndex = pages.size() - 1;

        currentStream = new PDPageContentStream(document, page);
        return currentStream;
    }

    /**
     * Returns the current content stream, creating a page if needed.
     */
    public PDPageContentStream getStream() throws IOException {
        if (currentStream == null) {
            return newPage();
        }
        return currentStream;
    }

    /**
     * Closes the current content stream.
     */
    public void closeCurrentStream() throws IOException {
        if (currentStream != null) {
            currentStream.close();
            currentStream = null;
        }
    }

    /**
     * Finishes all pages (closes streams).
     */
    public void finish() throws IOException {
        closeCurrentStream();
    }

    public PDDocument getDocument() { return document; }
    public int getPageCount() { return pages.size(); }
    public PDPage getCurrentPage() {
        return (currentPageIndex >= 0) ? pages.get(currentPageIndex) : null;
    }

    public PDPage getPage(int index) {
        return (index >= 0 && index < pages.size()) ? pages.get(index) : null;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public float getPageHeight() { return layoutContext.getPageHeight(); }
    public float getMarginTop() { return layoutContext.getMarginTop(); }
    public float getMarginLeft() { return layoutContext.getMarginLeft(); }
}
