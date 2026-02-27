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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.util.HashMap;
import java.util.Map;

/**
 * Entry point for Ballerina Java interop.
 * <p>
 * Static methods in this class are called directly from Ballerina via {@code @java:Method} bindings.
 */
public final class Native {

    private Native() {
    }

    /**
     * Converts HTML to PDF bytes.
     * <p>
     * Called from Ballerina: {@code parseHtml(string html, *ConversionOptions options)}
     *
     * @param html    HTML content as a Ballerina string
     * @param options ConversionOptions record as a BMap
     * @return byte[] (as BArray) on success, BError on failure
     */
    @SuppressWarnings("unchecked")
    public static Object parseHtml(BString html, BMap<BString, Object> options) {
        try {
            // Read options from BMap
            float fontSize = getFloat(options, "fontSizePt", ConverterOptions.DEFAULT_FONT_SIZE_PT);
            String pageSize = getString(options, "pageSize", "A4");
            String additionalCss = getNullableString(options, "additionalCss");

            // maxPages is optional (nil = no limit)
            Integer maxPages = null;
            Object maxPagesObj = options.get(StringUtils.fromString("maxPages"));
            if (maxPagesObj instanceof Number num) {
                maxPages = num.intValue();
                if (maxPages <= 0) {
                    return DiagnosticLog.renderError(
                            "maxPages must be greater than 0, got: " + maxPages, null);
                }
            }

            // Read custom fonts from nested map
            Map<String, byte[]> customFonts = null;
            Object customFontsObj = options.get(StringUtils.fromString("customFonts"));
            if (customFontsObj instanceof BMap) {
                BMap<BString, Object> fontsMap = (BMap<BString, Object>) customFontsObj;
                customFonts = new HashMap<>();
                for (BString key : fontsMap.getKeys()) {
                    Object val = fontsMap.get(key);
                    if (val instanceof BArray bArray) {
                        customFonts.put(key.getValue(), bArray.getBytes());
                    }
                }
            }

            // Read margins from nested PageMargins record
            float marginTop = ConverterOptions.DEFAULT_MARGIN;
            float marginRight = ConverterOptions.DEFAULT_MARGIN;
            float marginBottom = ConverterOptions.DEFAULT_MARGIN;
            float marginLeft = ConverterOptions.DEFAULT_MARGIN;

            Object marginsObj = options.get(StringUtils.fromString("margins"));
            if (marginsObj instanceof BMap) {
                BMap<BString, Object> margins = (BMap<BString, Object>) marginsObj;
                marginTop = getFloat(margins, "top", ConverterOptions.DEFAULT_MARGIN);
                marginRight = getFloat(margins, "right", ConverterOptions.DEFAULT_MARGIN);
                marginBottom = getFloat(margins, "bottom", ConverterOptions.DEFAULT_MARGIN);
                marginLeft = getFloat(margins, "left", ConverterOptions.DEFAULT_MARGIN);
            }

            // Resolve page dimensions from size name
            float[] dims = ConverterOptions.pageDimensions(pageSize);

            // Build ConverterOptions
            ConverterOptions opts = new ConverterOptions(
                    fontSize, dims[0], dims[1],
                    marginTop, marginRight, marginBottom, marginLeft,
                    additionalCss, customFonts, maxPages);

            // Phase 1: Parse HTML
            HtmlPreprocessor preprocessor = new HtmlPreprocessor();
            org.w3c.dom.Document doc;
            try {
                doc = preprocessor.preprocess(html.getValue());
            } catch (Exception e) {
                return DiagnosticLog.htmlParseError(
                        "HTML parsing failed: " + e.getMessage(), e);
            }

            // Phase 2: Convert to PDF
            HtmlToPdfConverter converter = new HtmlToPdfConverter();
            byte[] pdf = converter.convert(doc, opts);

            return ValueCreator.createArrayValue(pdf);
        } catch (Exception e) {
            return DiagnosticLog.renderError(
                    "PDF rendering failed: " + e.getMessage(), e);
        }
    }

    // --- PDF reading methods ---

    /**
     * Converts each page of a PDF (as byte[]) to Base64-encoded PNG images.
     */
    public static Object toImages(BArray pdf) {
        try (PDDocument doc = PdfReader.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF file to Base64-encoded PNG images.
     */
    public static Object fileToImages(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF at the given URL to Base64-encoded PNG images.
     */
    public static Object urlToImages(BString url) {
        try (PDDocument doc = PdfReader.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF (as byte[]).
     */
    public static Object extractText(BArray pdf) {
        try (PDDocument doc = PdfReader.loadFromBytes(pdf.getBytes())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF file.
     */
    public static Object fileExtractText(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF at the given URL.
     */
    public static Object urlExtractText(BString url) {
        try (PDDocument doc = PdfReader.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    // --- BMap helper methods ---

    private static float getFloat(BMap<BString, Object> map, String key, float defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof Number num) {
            return num.floatValue();
        }
        return defaultValue;
    }

    private static String getString(BMap<BString, Object> map, String key, String defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof BString bStr) {
            return bStr.getValue();
        }
        return defaultValue;
    }

    private static String getNullableString(BMap<BString, Object> map, String key) {
        Object value = map.get(StringUtils.fromString(key));
        if (value == null) {
            return null;
        }
        if (value instanceof BString bStr) {
            return bStr.getValue();
        }
        return null;
    }

}
