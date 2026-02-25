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
    private static final String READ_ERROR = "ReadError";

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

    public static BError readError(String message, Throwable cause) {
        return createError(READ_ERROR, message, cause);
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
