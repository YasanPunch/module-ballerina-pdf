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

package io.ballerina.lib.pdf.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS length values (px, pt, mm, %, em) and converts them to points.
 * 1px = 0.75pt, 1mm = 2.8346pt, 1in = 72pt, 1cm = 28.346pt
 */
public class CssValueParser {

    private static final float PX_TO_PT = 0.75f;
    private static final float MM_TO_PT = 2.8346457f;
    private static final float CM_TO_PT = 28.346457f;
    private static final float IN_TO_PT = 72f;

    private static final Pattern LENGTH_PATTERN =
            Pattern.compile("(-?[\\d.]+)\\s*(px|pt|mm|cm|in|em|rem|%)?", Pattern.CASE_INSENSITIVE);

    /**
     * Parses a CSS length string and converts to points.
     * Percentages are resolved relative to the given containerSize (in points).
     * em/rem are resolved relative to the given fontSize (in points).
     *
     * @param value         the CSS value string (e.g. "12px", "10mm", "50%")
     * @param containerSize the reference size for percentage resolution (in pt)
     * @param fontSize      the current font size for em resolution (in pt)
     * @return the length in points, or defaultValue if parsing fails
     */
    public static float toPoints(String value, float containerSize, float fontSize) {
        if (value == null || value.isBlank()) return 0;
        value = value.trim().toLowerCase();

        // Handle "auto" or "none"
        if (value.equals("auto") || value.equals("none") || value.equals("inherit")) return 0;

        Matcher m = LENGTH_PATTERN.matcher(value);
        if (!m.find()) return 0;

        float number = Float.parseFloat(m.group(1));
        String unit = m.group(2);

        if (unit == null || unit.equals("px")) {
            return number * PX_TO_PT;
        }
        return switch (unit) {
            case "pt" -> number;
            case "mm" -> number * MM_TO_PT;
            case "cm" -> number * CM_TO_PT;
            case "in" -> number * IN_TO_PT;
            case "em", "rem" -> number * fontSize;
            case "%" -> number / 100f * containerSize;
            default -> number * PX_TO_PT;
        };
    }

    /**
     * Convenience: parse a length with only a container reference (no em context).
     */
    public static float toPoints(String value, float containerSize) {
        return toPoints(value, containerSize, 16f * PX_TO_PT);
    }

    /**
     * Convenience: parse an absolute length with no container context.
     */
    public static float toPoints(String value) {
        return toPoints(value, 0, 16f * PX_TO_PT);
    }

    /**
     * Parses a CSS font-size value, returning the result in points.
     * Handles keywords like "small", "medium", "large", etc.
     */
    public static float parseFontSize(String value, float parentFontSize) {
        if (value == null || value.isBlank()) return parentFontSize;
        value = value.trim().toLowerCase();
        // CSS spec absolute-size keywords (based on medium = 16px)
        return switch (value) {
            case "xx-small" -> 9f * PX_TO_PT;
            case "x-small" -> 10f * PX_TO_PT;
            case "small" -> 13f * PX_TO_PT;
            case "medium" -> 16f * PX_TO_PT;
            case "large" -> 18f * PX_TO_PT;
            case "x-large" -> 24f * PX_TO_PT;
            case "xx-large" -> 32f * PX_TO_PT;
            case "smaller" -> parentFontSize * 0.85f;
            case "larger" -> parentFontSize * 1.2f;
            default -> toPoints(value, 0, parentFontSize);
        };
    }

    /**
     * Parses a font-weight value to determine if it's bold.
     */
    public static boolean isBold(String fontWeight) {
        if (fontWeight == null) return false;
        fontWeight = fontWeight.trim().toLowerCase();
        return fontWeight.equals("bold") || fontWeight.equals("bolder")
                || fontWeight.equals("700") || fontWeight.equals("800") || fontWeight.equals("900");
    }

    /**
     * Parses a font-style value to determine if it's italic.
     */
    public static boolean isItalic(String fontStyle) {
        if (fontStyle == null) return false;
        fontStyle = fontStyle.trim().toLowerCase();
        return fontStyle.equals("italic") || fontStyle.equals("oblique");
    }

    /**
     * Extracts the primary font family from a CSS font-family declaration.
     * E.g. "'Liberation Sans', Arial, sans-serif" → "Liberation Sans"
     */
    public static String parsePrimaryFontFamily(String fontFamily) {
        if (fontFamily == null || fontFamily.isBlank()) return null;
        String[] parts = fontFamily.split(",");
        String first = parts[0].trim();
        // Strip quotes
        first = first.replace("'", "").replace("\"", "");
        return first;
    }

    /**
     * Parses a CSS font-family declaration into its full fallback chain.
     * Splits on commas, strips quotes (single and double) from each entry, trims whitespace.
     * E.g. "'NonexistentFont', Arial, sans-serif" → ["NonexistentFont", "Arial", "sans-serif"]
     *
     * @param fontFamily the CSS font-family value
     * @return array of family names in fallback order; empty array for null/blank input
     */
    public static String[] parseFontFamilyList(String fontFamily) {
        if (fontFamily == null || fontFamily.isBlank()) return new String[0];
        String[] parts = fontFamily.split(",");
        String[] result = new String[parts.length];
        int count = 0;
        for (String part : parts) {
            String trimmed = part.trim().replace("'", "").replace("\"", "");
            if (!trimmed.isEmpty()) {
                result[count++] = trimmed;
            }
        }
        if (count == parts.length) {
            return result;
        }
        String[] compact = new String[count];
        System.arraycopy(result, 0, compact, 0, count);
        return compact;
    }
}
