package io.ballerina.lib.pdf;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;

/**
 * Centralized error creation utility for the PDF module.
 * <p>
 * Creates typed Ballerina errors that map to the error hierarchy defined in {@code errors.bal}.
 * Error type names must match the Ballerina type names exactly.
 */
public final class DiagnosticLog {

    private DiagnosticLog() {
    }

    private static final String ERROR = "Error";
    private static final String HTML_PARSE_ERROR = "HtmlParseError";
    private static final String RENDER_ERROR = "RenderError";

    public static BError error(String message) {
        return createError(ERROR, message, null);
    }

    public static BError error(String message, Throwable cause) {
        return createError(ERROR, message, cause);
    }

    public static BError htmlParseError(String message, Throwable cause) {
        return createError(HTML_PARSE_ERROR, message, cause);
    }

    public static BError renderError(String message, Throwable cause) {
        return createError(RENDER_ERROR, message, cause);
    }

    private static BError createError(String errorType, String message, Throwable cause) {
        BString errorMessage = StringUtils.fromString(message);
        BError causeError = cause != null
                ? ErrorCreator.createError(StringUtils.fromString(cause.getMessage()))
                : null;
        return ErrorCreator.createError(
                ModuleUtils.getModule(), errorType, errorMessage, causeError, null);
    }
}
