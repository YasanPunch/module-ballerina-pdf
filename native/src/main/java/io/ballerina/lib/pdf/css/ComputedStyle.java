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

package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.CssValueParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolved CSS styles for a single element after cascade, specificity, and inheritance.
 * Provides typed accessors for commonly used properties.
 */
public class ComputedStyle {

    private final Map<String, String> properties = new HashMap<>();

    // Properties that inherit from parent by default (CSS spec)
    private static final Set<String> INHERITED_PROPERTIES = Set.of(
            "color", "font-family", "font-size", "font-weight", "font-style",
            "text-align", "text-transform", "text-decoration", "line-height",
            "letter-spacing", "word-spacing", "white-space", "visibility",
            "list-style-type", "list-style-position", "border-collapse",
            "border-spacing"
    );

    public void set(String property, String value) {
        properties.put(property, value);
    }

    public String get(String property) {
        return properties.get(property);
    }

    public String get(String property, String defaultValue) {
        return properties.getOrDefault(property, defaultValue);
    }

    public Map<String, String> getAll() {
        return properties;
    }

    /**
     * Returns true if this property should be inherited from the parent.
     */
    public static boolean isInherited(String property) {
        return INHERITED_PROPERTIES.contains(property);
    }

    public static Set<String> getInheritedProperties() {
        return INHERITED_PROPERTIES;
    }

    // --- Typed accessors ---

    public String getDisplay() {
        return get("display", "inline");
    }

    public String getFontFamily() {
        return get("font-family");
    }

    public float getFontSize(float parentFontSize) {
        String val = get("font-size");
        if (val == null) return parentFontSize;
        return CssValueParser.parseFontSize(val, parentFontSize);
    }

    public boolean isBold() {
        return CssValueParser.isBold(get("font-weight"));
    }

    public boolean isItalic() {
        return CssValueParser.isItalic(get("font-style"));
    }

    public String getTextAlign() {
        return get("text-align", "left");
    }

    public String getTextTransform() {
        return get("text-transform", "none");
    }

    public String getVerticalAlign() {
        return get("vertical-align", "baseline");
    }

    public String getColor() {
        return get("color");
    }

    public float getOpacity() {
        String val = get("opacity");
        if (val == null) return 1.0f;
        try { return Math.max(0, Math.min(1, Float.parseFloat(val.trim()))); }
        catch (NumberFormatException e) { return 1.0f; }
    }

    /** Parsed box-shadow values. */
    public record BoxShadow(float offsetX, float offsetY, float blur, float spread, String color) {}

    // Pattern to extract length values from box-shadow
    private static final Pattern SHADOW_LENGTH = Pattern.compile("-?[\\d.]+(?:px|pt|em|rem|%|mm|cm|in)?");

    // Pattern to extract color from a shadow value (hex, rgb/rgba, hsl/hsla)
    private static final Pattern COLOR_IN_SHADOW = Pattern.compile(
            "#[0-9a-fA-F]{3,8}|rgba?\\([^)]+\\)|hsla?\\([^)]+\\)");

    /**
     * Parses the box-shadow property. Returns null if none/unset.
     * Supports a single shadow: offsetX offsetY [blur [spread]] [color].
     * For multiple shadows, use {@link #getBoxShadows(float, float)}.
     */
    public BoxShadow getBoxShadow(float containerWidth, float fontSize) {
        List<BoxShadow> shadows = getBoxShadows(containerWidth, fontSize);
        return shadows.isEmpty() ? null : shadows.get(0);
    }

    /**
     * Parses the box-shadow property into a list of shadows.
     * Supports comma-separated values like "0 0 0 2px #fff, 0 0 0 4px #d69e2e".
     * Returns an empty list if none/unset.
     */
    public List<BoxShadow> getBoxShadows(float containerWidth, float fontSize) {
        String val = get("box-shadow");
        if (val == null || val.equals("none")) return List.of();
        val = val.trim();

        // Split on commas that are not inside parentheses (e.g. rgba(...))
        List<String> parts = splitOnTopLevelCommas(val);
        List<BoxShadow> result = new ArrayList<>();

        for (String part : parts) {
            BoxShadow shadow = parseSingleShadow(part.trim(), containerWidth, fontSize);
            if (shadow != null) {
                result.add(shadow);
            }
        }
        return result;
    }

    /**
     * Splits a string on commas that are not nested inside parentheses.
     */
    private static List<String> splitOnTopLevelCommas(String value) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ',' && depth == 0) {
                parts.add(value.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(value.substring(start));
        return parts;
    }

    private BoxShadow parseSingleShadow(String val, float containerWidth, float fontSize) {
        if (val.isEmpty()) return null;

        // Extract color first — hex colors like #d69e2e contain digits that the
        // length regex would otherwise match as spurious length values
        Matcher cm = COLOR_IN_SHADOW.matcher(val);
        String shadowColor = null;
        String lengthPart = val;
        if (cm.find()) {
            shadowColor = cm.group();
            lengthPart = val.substring(0, cm.start()) + val.substring(cm.end());
        }

        // Extract lengths from the color-free string
        Matcher m = SHADOW_LENGTH.matcher(lengthPart);
        List<String> lengths = new ArrayList<>();
        while (m.find()) {
            lengths.add(m.group());
        }

        if (lengths.size() < 2) return null;

        float ox = CssValueParser.toPoints(lengths.get(0), containerWidth, fontSize);
        float oy = CssValueParser.toPoints(lengths.get(1), containerWidth, fontSize);
        float blur = lengths.size() > 2 ? CssValueParser.toPoints(lengths.get(2), containerWidth, fontSize) : 0;
        float spread = lengths.size() > 3 ? CssValueParser.toPoints(lengths.get(3), containerWidth, fontSize) : 0;
        if (shadowColor == null) shadowColor = "rgba(0,0,0,1)";

        return new BoxShadow(ox, oy, blur, spread, shadowColor);
    }

    /**
     * Returns the computed line-height in points.
     * Supports: "normal" (returns -1), unitless multiplier (1.8),
     * px, em, pt, %, and "inherit"/"initial" (returns -1).
     * A return value of -1 means the caller should use font metrics.
     */
    public float getLineHeight(float fontSize) {
        String val = get("line-height");
        if (val == null || "normal".equals(val) || "inherit".equals(val) || "initial".equals(val)) {
            return -1;
        }
        val = val.trim();
        if (val.endsWith("%")) {
            try { return Float.parseFloat(val.replace("%", "")) / 100f * fontSize; }
            catch (NumberFormatException e) { return -1; }
        }
        if (val.endsWith("px") || val.endsWith("pt") || val.endsWith("em")) {
            return CssValueParser.toPoints(val, 0, fontSize);
        }
        // Unitless number: "1.8" → 1.8 * fontSize
        try { return Float.parseFloat(val) * fontSize; }
        catch (NumberFormatException e) { return -1; }
    }

    public float getLetterSpacing(float fontSize) {
        String val = get("letter-spacing");
        if (val == null || val.equals("normal")) return 0;
        return CssValueParser.toPoints(val, 0, fontSize);
    }

    public float getWordSpacing(float fontSize) {
        String val = get("word-spacing");
        if (val == null || val.equals("normal")) return 0;
        return CssValueParser.toPoints(val, 0, fontSize);
    }

    public String getBackgroundColor() {
        return get("background-color");
    }

    public String getBackgroundImage() {
        return get("background-image");
    }

    public float getWidth(float containerWidth, float fontSize) {
        String val = get("width");
        if (val == null || val.equals("auto")) return -1;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getHeight(float containerHeight, float fontSize) {
        String val = get("height");
        if (val == null || val.equals("auto")) return -1;
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    public float getMaxWidth(float containerWidth, float fontSize) {
        String val = get("max-width");
        if (val == null || val.equals("none")) return Float.MAX_VALUE;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getMinWidth(float containerWidth, float fontSize) {
        String val = get("min-width");
        if (val == null || val.equals("auto")) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getMaxHeight(float containerHeight, float fontSize) {
        String val = get("max-height");
        if (val == null || val.equals("none")) return Float.MAX_VALUE;
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    public float getMinHeight(float containerHeight, float fontSize) {
        String val = get("min-height");
        if (val == null || val.equals("auto")) return 0;
        return CssValueParser.toPoints(val, containerHeight, fontSize);
    }

    public float getMarginTop(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-top"), containerWidth, fontSize);
    }

    public float getMarginRight(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-right"), containerWidth, fontSize);
    }

    public float getMarginBottom(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-bottom"), containerWidth, fontSize);
    }

    public float getMarginLeft(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("margin-left"), containerWidth, fontSize);
    }

    public boolean isMarginLeftAuto() {
        String val = get("margin-left");
        return val != null && val.trim().equalsIgnoreCase("auto");
    }

    public boolean isMarginRightAuto() {
        String val = get("margin-right");
        return val != null && val.trim().equalsIgnoreCase("auto");
    }

    public float getPaddingTop(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-top"), containerWidth, fontSize);
    }

    public float getPaddingRight(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-right"), containerWidth, fontSize);
    }

    public float getPaddingBottom(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-bottom"), containerWidth, fontSize);
    }

    public float getPaddingLeft(float containerWidth, float fontSize) {
        return CssValueParser.toPoints(get("padding-left"), containerWidth, fontSize);
    }

    public float getBorderTopWidth(float containerWidth, float fontSize) {
        String style = getBorderTopStyle();
        if ("none".equals(style) || "hidden".equals(style)) return 0;
        return parseBorderWidth(get("border-top-width"), containerWidth, fontSize);
    }

    public float getBorderRightWidth(float containerWidth, float fontSize) {
        String style = getBorderRightStyle();
        if ("none".equals(style) || "hidden".equals(style)) return 0;
        return parseBorderWidth(get("border-right-width"), containerWidth, fontSize);
    }

    public float getBorderBottomWidth(float containerWidth, float fontSize) {
        String style = getBorderBottomStyle();
        if ("none".equals(style) || "hidden".equals(style)) return 0;
        return parseBorderWidth(get("border-bottom-width"), containerWidth, fontSize);
    }

    public float getBorderLeftWidth(float containerWidth, float fontSize) {
        String style = getBorderLeftStyle();
        if ("none".equals(style) || "hidden".equals(style)) return 0;
        return parseBorderWidth(get("border-left-width"), containerWidth, fontSize);
    }

    public String getBorderTopColor() {
        return get("border-top-color");
    }

    public String getBorderRightColor() {
        return get("border-right-color");
    }

    public String getBorderBottomColor() {
        return get("border-bottom-color");
    }

    public String getBorderLeftColor() {
        return get("border-left-color");
    }

    public String getBorderTopStyle() {
        return get("border-top-style", "none");
    }

    public String getBorderRightStyle() {
        return get("border-right-style", "none");
    }

    public String getBorderBottomStyle() {
        return get("border-bottom-style", "none");
    }

    public String getBorderLeftStyle() {
        return get("border-left-style", "none");
    }

    public String getBorderCollapse() {
        return get("border-collapse", "separate");
    }

    public float getBorderSpacing(float containerWidth, float fontSize) {
        String val = get("border-spacing");
        if (val == null) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getBorderTopLeftRadius(float containerWidth, float fontSize) {
        String val = get("border-top-left-radius");
        if (val == null || val.equals("0")) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getBorderTopRightRadius(float containerWidth, float fontSize) {
        String val = get("border-top-right-radius");
        if (val == null || val.equals("0")) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getBorderBottomRightRadius(float containerWidth, float fontSize) {
        String val = get("border-bottom-right-radius");
        if (val == null || val.equals("0")) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public float getBorderBottomLeftRadius(float containerWidth, float fontSize) {
        String val = get("border-bottom-left-radius");
        if (val == null || val.equals("0")) return 0;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }

    public String getPosition() {
        return get("position", "static");
    }

    public String getFloat() {
        return get("float", "none");
    }

    public String getClear() {
        return get("clear", "none");
    }

    public float getTop(float containerSize, float fontSize) {
        String val = get("top");
        if (val == null || val.equals("auto")) return Float.NaN;
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    public float getLeft(float containerSize, float fontSize) {
        String val = get("left");
        if (val == null || val.equals("auto")) return Float.NaN;
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    public float getRight(float containerSize, float fontSize) {
        String val = get("right");
        if (val == null || val.equals("auto")) return Float.NaN;
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    public float getBottom(float containerSize, float fontSize) {
        String val = get("bottom");
        if (val == null || val.equals("auto")) return Float.NaN;
        return CssValueParser.toPoints(val, containerSize, fontSize);
    }

    private float parseBorderWidth(String val, float containerWidth, float fontSize) {
        if (val == null || val.equals("none") || val.equals("0")) return 0;
        if (val.equals("thin")) return 0.75f;
        if (val.equals("medium")) return 1.5f;
        if (val.equals("thick")) return 2.25f;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }
}
