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
     * Called from Ballerina: {@code convertToPdf(string html, *ConversionOptions options)}
     *
     * @param html    HTML content as a Ballerina string
     * @param options ConversionOptions record as a BMap
     * @return byte[] (as BArray) on success, BError on failure
     */
    @SuppressWarnings("unchecked")
    public static Object convertToPdf(BString html, BMap<BString, Object> options) {
        try {
            // Read options from BMap
            float fontSize = getFloat(options, "defaultFontSizePt", ConverterOptions.DEFAULT_FONT_SIZE_PT);
            String pageSize = getString(options, "pageSize", "A4");
            boolean preprocess = getBoolean(options, "preprocess", true);
            String additionalCss = getNullableString(options, "additionalCss");
            int maxPages = getInt(options, "maxPages", 0);

            // Read custom fonts from nested map
            Map<String, byte[]> customFonts = null;
            Object customFontsObj = options.get(StringUtils.fromString("customFonts"));
            if (customFontsObj instanceof BMap) {
                @SuppressWarnings("unchecked")
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
                    additionalCss, preprocess, customFonts, maxPages);

            // Phase 1: Preprocess HTML
            HtmlPreprocessor preprocessor = new HtmlPreprocessor();
            org.w3c.dom.Document doc;
            try {
                if (preprocess) {
                    doc = preprocessor.preprocess(html.getValue(), opts);
                } else {
                    doc = preprocessor.parseOnly(html.getValue());
                }
            } catch (Exception e) {
                return DiagnosticLog.htmlParseError(
                        "HTML parsing failed: " + e.getMessage(), e);
            }

            // Phase 2: Convert to PDF
            HtmlToPdfConverter converter = new HtmlToPdfConverter();
            byte[] pdf = converter.convertToPdf(doc, opts);

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
    public static Object toImagesFromFile(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.toImages(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF image conversion failed: " + e.getMessage(), e);
        }
    }

    /**
     * Converts each page of a PDF at the given URL to Base64-encoded PNG images.
     */
    public static Object toImagesFromUrl(BString url) {
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
    public static Object extractTextFromFile(BString filePath) {
        try (PDDocument doc = PdfReader.loadFromFile(filePath.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts text content from each page of a PDF at the given URL.
     */
    public static Object extractTextFromUrl(BString url) {
        try (PDDocument doc = PdfReader.loadFromUrl(url.getValue())) {
            return ValueCreator.createArrayValue(PdfReader.extractText(doc));
        } catch (Exception e) {
            return DiagnosticLog.readError("PDF text extraction failed: " + e.getMessage(), e);
        }
    }

    // --- BMap helper methods ---

    private static int getInt(BMap<BString, Object> map, String key, int defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }

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

    private static boolean getBoolean(BMap<BString, Object> map, String key, boolean defaultValue) {
        Object value = map.get(StringUtils.fromString(key));
        if (value instanceof Boolean b) {
            return b;
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
