package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.CssValueParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public static Set<String> isInheritedProperties() {
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

    public String getColor() {
        return get("color");
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
        return parseBorderWidth(get("border-top-width"), containerWidth, fontSize);
    }

    public float getBorderRightWidth(float containerWidth, float fontSize) {
        return parseBorderWidth(get("border-right-width"), containerWidth, fontSize);
    }

    public float getBorderBottomWidth(float containerWidth, float fontSize) {
        return parseBorderWidth(get("border-bottom-width"), containerWidth, fontSize);
    }

    public float getBorderLeftWidth(float containerWidth, float fontSize) {
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

    private float parseBorderWidth(String val, float containerWidth, float fontSize) {
        if (val == null || val.equals("none") || val.equals("0")) return 0;
        if (val.equals("thin")) return 0.75f;
        if (val.equals("medium")) return 1.5f;
        if (val.equals("thick")) return 2.25f;
        return CssValueParser.toPoints(val, containerWidth, fontSize);
    }
}
