package io.ballerina.lib.pdf;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;

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
                    additionalCss, preprocess);

            // Run pipeline
            HtmlPreprocessor preprocessor = new HtmlPreprocessor();
            org.w3c.dom.Document doc;
            if (preprocess) {
                doc = preprocessor.preprocess(html.getValue(), opts);
            } else {
                doc = preprocessor.parseOnly(html.getValue());
            }

            HtmlToPdfConverter converter = new HtmlToPdfConverter();
            byte[] pdf = converter.convertToPdf(doc, opts);

            return ValueCreator.createArrayValue(pdf);
        } catch (Exception e) {
            return createConversionError("PDF conversion failed: " + e.getMessage(), e);
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

    private static final String CONVERSION_ERROR = "ConversionError";

    private static BError createConversionError(String message, Throwable cause) {
        BString errorMessage = StringUtils.fromString(message);
        BError causeError = null;
        if (cause != null) {
            causeError = ErrorCreator.createError(StringUtils.fromString(cause.getMessage()));
        }
        return ErrorCreator.createError(
                ModuleUtils.getModule(), CONVERSION_ERROR, errorMessage, causeError, null);
    }
}
