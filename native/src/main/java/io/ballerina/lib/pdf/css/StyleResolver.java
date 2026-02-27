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

import io.ballerina.lib.pdf.util.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * Resolves CSS styles for DOM elements using the cascade:
 * 1. Match stylesheet rules by selector
 * 2. Sort by specificity + source order
 * 3. Apply inline styles (highest specificity)
 * 4. Handle !important
 * 5. Inherit inherited properties from parent
 * 6. Expand shorthand properties
 */
public class StyleResolver {

    private final CssStylesheet stylesheet;
    private final CssParser parser = new CssParser();
    // IdentityHashMap is intentional: DOM elements are unique object instances per conversion,
    // so identity comparison (==) is correct and avoids the cost of deep DOM equality checks.
    // This cache is per-conversion, so there is no staleness risk.
    private final Map<Element, ComputedStyle> cache = new IdentityHashMap<>();

    // Default display values for HTML elements
    private static final Map<String, String> DEFAULT_DISPLAY = Map.ofEntries(
            Map.entry("div", "block"), Map.entry("p", "block"), Map.entry("h1", "block"),
            Map.entry("h2", "block"), Map.entry("h3", "block"), Map.entry("h4", "block"),
            Map.entry("h5", "block"), Map.entry("h6", "block"), Map.entry("ul", "block"),
            Map.entry("ol", "block"), Map.entry("li", "list-item"), Map.entry("body", "block"),
            Map.entry("html", "block"), Map.entry("header", "block"), Map.entry("footer", "block"),
            Map.entry("section", "block"), Map.entry("article", "block"), Map.entry("main", "block"),
            Map.entry("nav", "block"), Map.entry("blockquote", "block"), Map.entry("pre", "block"),
            Map.entry("hr", "block"), Map.entry("br", "block"),
            Map.entry("table", "table"), Map.entry("thead", "table-header-group"),
            Map.entry("tbody", "table-row-group"), Map.entry("tfoot", "table-footer-group"),
            Map.entry("tr", "table-row"), Map.entry("td", "table-cell"), Map.entry("th", "table-cell"),
            Map.entry("colgroup", "table-column-group"), Map.entry("col", "table-column"),
            Map.entry("caption", "table-caption"),
            Map.entry("span", "inline"), Map.entry("a", "inline"), Map.entry("strong", "inline"),
            Map.entry("b", "inline"), Map.entry("em", "inline"), Map.entry("i", "inline"),
            Map.entry("u", "inline"), Map.entry("s", "inline"), Map.entry("del", "inline"),
            Map.entry("strike", "inline"), Map.entry("small", "inline"), Map.entry("sub", "inline"),
            Map.entry("sup", "inline"), Map.entry("img", "inline"),
            Map.entry("style", "none"), Map.entry("script", "none"), Map.entry("head", "none"),
            Map.entry("meta", "none"), Map.entry("link", "none"), Map.entry("title", "none")
    );

    // Default styles for HTML elements (UA stylesheet, per CSS 2.1 Appendix D)
    private static final Map<String, Map<String, String>> UA_STYLES = Map.ofEntries(
            Map.entry("body", Map.of(
                    "margin-top", "0", "margin-right", "0", "margin-bottom", "0", "margin-left", "0",
                    "padding-top", "0", "padding-right", "0", "padding-bottom", "0", "padding-left", "0")),
            Map.entry("table", Map.of("width", "100%", "table-layout", "fixed")),
            Map.entry("h1", Map.of("font-size", "24px", "font-weight", "bold", "margin-top", "16px", "margin-bottom", "16px")),
            Map.entry("h2", Map.of("font-size", "20px", "font-weight", "bold", "margin-top", "14px", "margin-bottom", "14px")),
            Map.entry("h3", Map.of("font-size", "16px", "font-weight", "bold", "margin-top", "12px", "margin-bottom", "12px")),
            Map.entry("h4", Map.of("font-size", "14px", "font-weight", "bold", "margin-top", "10px", "margin-bottom", "10px")),
            Map.entry("h5", Map.of("font-size", "12px", "font-weight", "bold", "margin-top", "10px", "margin-bottom", "10px")),
            Map.entry("h6", Map.of("font-size", "10px", "font-weight", "bold", "margin-top", "10px", "margin-bottom", "10px")),
            Map.entry("p", Map.of("margin-top", "8px", "margin-bottom", "8px")),
            Map.entry("hr", Map.of("border-top-width", "1px", "border-top-style", "solid",
                    "border-top-color", "#808080", "margin-top", "8px", "margin-bottom", "8px")),
            Map.entry("ul", Map.of("margin-top", "8px", "margin-bottom", "8px", "padding-left", "30px")),
            Map.entry("ol", Map.of("margin-top", "8px", "margin-bottom", "8px", "padding-left", "30px")),
            Map.entry("blockquote", Map.of("margin-top", "8px", "margin-bottom", "8px",
                    "margin-left", "30px", "margin-right", "30px")),
            Map.entry("pre", Map.of("margin-top", "8px", "margin-bottom", "8px",
                    "font-family", "monospace", "white-space", "pre")),
            Map.entry("strong", Map.of("font-weight", "bold")),
            Map.entry("b", Map.of("font-weight", "bold")),
            Map.entry("em", Map.of("font-style", "italic")),
            Map.entry("i", Map.of("font-style", "italic")),
            Map.entry("u", Map.of("text-decoration", "underline")),
            Map.entry("s", Map.of("text-decoration", "line-through")),
            Map.entry("del", Map.of("text-decoration", "line-through")),
            Map.entry("strike", Map.of("text-decoration", "line-through")),
            Map.entry("th", Map.of("font-weight", "bold", "text-align", "center")),
            Map.entry("sup", Map.of("font-size", "smaller", "vertical-align", "super")),
            Map.entry("sub", Map.of("font-size", "smaller", "vertical-align", "sub")),
            Map.entry("small", Map.of("font-size", "smaller"))
    );

    public StyleResolver(CssStylesheet stylesheet) {
        this.stylesheet = stylesheet;
    }

    /**
     * Resolves the computed style for an element, using cascade + inheritance.
     */
    public ComputedStyle resolve(Element element) {
        ComputedStyle cached = cache.get(element);
        if (cached != null) return cached;

        ComputedStyle style = new ComputedStyle();
        String tagName = DomUtils.tagName(element);

        // 1. Set default display for the tag
        String defaultDisplay = DEFAULT_DISPLAY.getOrDefault(tagName, "inline");
        style.set("display", defaultDisplay);

        // 2. Apply UA (user-agent) styles
        Map<String, String> uaStyles = UA_STYLES.get(tagName);
        if (uaStyles != null) {
            for (var entry : uaStyles.entrySet()) {
                style.set(entry.getKey(), entry.getValue());
            }
        }

        // 3. Collect matching stylesheet rules
        List<MatchedRule> matchedRules = new ArrayList<>();
        for (CssRule rule : stylesheet.getRules()) {
            if (rule.selector().matches(element)) {
                matchedRules.add(new MatchedRule(rule.selector().getSpecificity(), rule.sourceOrder(), rule.declarations()));
            }
        }

        // Sort by specificity then source order
        matchedRules.sort(Comparator
                .comparing(MatchedRule::specificity)
                .thenComparingInt(MatchedRule::sourceOrder));

        // 4. Apply matched rules (lower specificity first, higher overwrites)
        for (MatchedRule matched : matchedRules) {
            for (CssDeclaration decl : matched.declarations()) {
                if (!decl.important()) {
                    applyDeclaration(style, decl.property(), decl.value());
                }
            }
        }

        // 5. Apply inline styles (specificity: 1,0,0,0)
        String inlineStyle = element.getAttribute("style");
        if (inlineStyle != null && !inlineStyle.isEmpty()) {
            List<CssDeclaration> inlineDecls = parser.parseInlineStyle(inlineStyle);
            for (CssDeclaration decl : inlineDecls) {
                if (!decl.important()) {
                    applyDeclaration(style, decl.property(), decl.value());
                }
            }
        }

        // 6. Apply !important declarations (override everything)
        for (MatchedRule matched : matchedRules) {
            for (CssDeclaration decl : matched.declarations()) {
                if (decl.important()) {
                    applyDeclaration(style, decl.property(), decl.value());
                }
            }
        }
        if (inlineStyle != null && !inlineStyle.isEmpty()) {
            List<CssDeclaration> inlineDecls = parser.parseInlineStyle(inlineStyle);
            for (CssDeclaration decl : inlineDecls) {
                if (decl.important()) {
                    applyDeclaration(style, decl.property(), decl.value());
                }
            }
        }

        // 7. Apply HTML presentational attributes
        applyHtmlAttributes(element, style, tagName);

        // 8. Inherit from parent
        Node parent = element.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            ComputedStyle parentStyle = resolve((Element) parent);
            for (String prop : ComputedStyle.getInheritedProperties()) {
                if (style.get(prop) == null || "inherit".equals(style.get(prop))) {
                    String parentVal = parentStyle.get(prop);
                    if (parentVal != null) {
                        style.set(prop, parentVal);
                    }
                }
            }
        }

        cache.put(element, style);
        return style;
    }

    /**
     * Applies a CSS declaration, expanding shorthand properties.
     */
    private void applyDeclaration(ComputedStyle style, String property, String value) {
        switch (property) {
            case "margin" -> expandFourSided(style, "margin", value);
            case "padding" -> expandFourSided(style, "padding", value);
            case "border" -> expandBorder(style, value, "top", "right", "bottom", "left");
            case "border-top" -> expandBorderSide(style, "top", value);
            case "border-right" -> expandBorderSide(style, "right", value);
            case "border-bottom" -> expandBorderSide(style, "bottom", value);
            case "border-left" -> expandBorderSide(style, "left", value);
            case "border-width" -> expandFourSidedBorderWidth(style, value);
            case "border-style" -> expandFourSidedBorderStyle(style, value);
            case "border-color" -> expandFourSidedBorderColor(style, value);
            case "border-radius" -> expandBorderRadius(style, value);
            case "background" -> expandBackground(style, value);
            case "font" -> expandFont(style, value);
            default -> style.set(property, value);
        }
    }

    private void expandFourSided(ComputedStyle style, String prefix, String value) {
        expandFourSidedProperty(style, value, prefix + "-top", prefix + "-right",
                prefix + "-bottom", prefix + "-left");
    }

    /**
     * Generic four-sided expansion: splits value on whitespace and assigns to 4 property names
     * using CSS shorthand rules (1→all, 2→vert/horiz, 3→top/horiz/bottom, 4→each).
     */
    private void expandFourSidedProperty(ComputedStyle style, String value,
                                          String topProp, String rightProp,
                                          String bottomProp, String leftProp) {
        String[] parts = value.trim().split("\\s+");
        String top, right, bottom, left;
        switch (parts.length) {
            case 1 -> { top = right = bottom = left = parts[0]; }
            case 2 -> { top = bottom = parts[0]; right = left = parts[1]; }
            case 3 -> { top = parts[0]; right = left = parts[1]; bottom = parts[2]; }
            default -> { top = parts[0]; right = parts[1]; bottom = parts[2]; left = parts[3]; }
        }
        style.set(topProp, top);
        style.set(rightProp, right);
        style.set(bottomProp, bottom);
        style.set(leftProp, left);
    }

    private void expandBorder(ComputedStyle style, String value, String... sides) {
        for (String side : sides) {
            expandBorderSide(style, side, value);
        }
    }

    // Matches parenthesized CSS color functions (rgb, rgba, hsl, hsla) that contain spaces
    private static final Pattern PAREN_COLOR = Pattern.compile(
            "(?:rgba?|hsla?)\\s*\\([^)]+\\)", CASE_INSENSITIVE);

    private void expandBorderSide(ComputedStyle style, String side, String value) {
        // border shorthand: [width] [style] [color]
        // Color functions like rgb(183, 121, 31) contain spaces, so extract them first
        String v = value.trim();
        String color = null;

        Matcher m = PAREN_COLOR.matcher(v);
        if (m.find()) {
            color = m.group();
            v = (v.substring(0, m.start()) + " " + v.substring(m.end())).trim();
        }

        String[] parts = v.split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (isBorderWidth(part)) {
                style.set("border-" + side + "-width", part);
            } else if (isBorderStyle(part)) {
                style.set("border-" + side + "-style", part);
            } else if (color == null) {
                color = part; // hex or named color
            }
        }
        if (color != null) {
            style.set("border-" + side + "-color", color);
        }
    }

    private void expandFourSidedBorderWidth(ComputedStyle style, String value) {
        expandFourSidedProperty(style, value, "border-top-width", "border-right-width",
                "border-bottom-width", "border-left-width");
    }

    private void expandFourSidedBorderStyle(ComputedStyle style, String value) {
        expandFourSidedProperty(style, value, "border-top-style", "border-right-style",
                "border-bottom-style", "border-left-style");
    }

    private void expandFourSidedBorderColor(ComputedStyle style, String value) {
        // Color values may contain spaces (e.g. "rgb(1, 2, 3) rgb(4, 5, 6)"),
        // so split on top-level whitespace outside parentheses
        List<String> colors = splitColorValues(value.trim());
        String top, right, bottom, left;
        switch (colors.size()) {
            case 1 -> { top = right = bottom = left = colors.get(0); }
            case 2 -> { top = bottom = colors.get(0); right = left = colors.get(1); }
            case 3 -> { top = colors.get(0); right = left = colors.get(1); bottom = colors.get(2); }
            default -> { top = colors.get(0); right = colors.get(1); bottom = colors.get(2); left = colors.get(3); }
        }
        style.set("border-top-color", top);
        style.set("border-right-color", right);
        style.set("border-bottom-color", bottom);
        style.set("border-left-color", left);
    }

    /**
     * Splits a CSS value containing multiple color tokens, respecting parentheses.
     * E.g. "rgb(1, 2, 3) #fff" → ["rgb(1, 2, 3)", "#fff"]
     */
    private List<String> splitColorValues(String value) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if ((c == ' ' || c == '\t') && depth == 0) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private void expandBorderRadius(ComputedStyle style, String value) {
        // CSS border-radius: 1-4 values (top-left, top-right, bottom-right, bottom-left)
        // Does not handle the "/" syntax for elliptical radii — uses circular radii only.
        String[] parts = value.trim().split("\\s+");
        String tl, tr, br, bl;
        switch (parts.length) {
            case 1 -> { tl = tr = br = bl = parts[0]; }
            case 2 -> { tl = br = parts[0]; tr = bl = parts[1]; }
            case 3 -> { tl = parts[0]; tr = bl = parts[1]; br = parts[2]; }
            default -> { tl = parts[0]; tr = parts[1]; br = parts[2]; bl = parts[3]; }
        }
        style.set("border-top-left-radius", tl);
        style.set("border-top-right-radius", tr);
        style.set("border-bottom-right-radius", br);
        style.set("border-bottom-left-radius", bl);
    }

    private void expandBackground(ComputedStyle style, String value) {
        String v = value.trim();
        // Check for url() or data: — that's a background-image
        if (v.contains("url(")) {
            style.set("background-image", v);
        } else {
            // Simple color value
            style.set("background-color", v);
        }
    }

    private void expandFont(ComputedStyle style, String value) {
        // CSS font shorthand: [style] [weight] size[/line-height] family[, family...]
        // Grammar-aware parser: find font-size token first, then classify before/after.
        List<String> tokens = tokenizeFont(value.trim());
        if (tokens.isEmpty()) return;

        // Find the font-size token: first token that looks like a CSS length or size keyword
        int sizeIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            String t = tokens.get(i);
            if (isFontSizeToken(t)) {
                sizeIdx = i;
                break;
            }
        }

        if (sizeIdx == -1) {
            // No size found — can't parse this shorthand meaningfully
            return;
        }

        // Everything before size: font-style, font-weight, font-variant
        for (int i = 0; i < sizeIdx; i++) {
            String t = tokens.get(i);
            if (t.equals("italic") || t.equals("oblique")) {
                style.set("font-style", t);
            } else if (t.equals("bold") || t.equals("bolder") || t.equals("lighter")
                    || t.matches("\\d{1,3}")) {
                style.set("font-weight", t);
            }
            // "normal" and "small-caps" are silently consumed
        }

        // Size token: may contain /line-height (e.g., "12px/1.5")
        String sizeToken = tokens.get(sizeIdx);
        String[] sizeParts = sizeToken.split("/");
        style.set("font-size", sizeParts[0]);
        if (sizeParts.length > 1) {
            style.set("line-height", sizeParts[1]);
        }

        // Handle separated slash format: "12px / 1.5" (CSS parser canonicalization)
        int familyStart = sizeIdx + 1;
        if (familyStart < tokens.size() && tokens.get(familyStart).equals("/")) {
            if (familyStart + 1 < tokens.size()) {
                style.set("line-height", tokens.get(familyStart + 1));
                familyStart += 2;
            }
        }

        // Everything after size (and optional /line-height): font-family
        if (familyStart < tokens.size()) {
            StringBuilder family = new StringBuilder();
            for (int i = familyStart; i < tokens.size(); i++) {
                if (family.length() > 0) family.append(' ');
                family.append(tokens.get(i));
            }
            style.set("font-family", family.toString());
        }
    }

    /**
     * Tokenizes a CSS font shorthand value, keeping quoted strings as single tokens
     * and preserving commas as part of adjacent tokens (for font-family lists).
     */
    private List<String> tokenizeFont(String value) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (inQuote) {
                current.append(c);
                if (c == quoteChar) {
                    inQuote = false;
                }
            } else if (c == '\'' || c == '"') {
                current.append(c);
                inQuote = true;
                quoteChar = c;
            } else if (c == ' ' || c == '\t') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private boolean isFontSizeToken(String token) {
        // Remove /line-height suffix for checking
        String check = token.contains("/") ? token.substring(0, token.indexOf('/')) : token;
        // CSS length units
        if (check.matches("-?[\\d.]+(?:px|pt|em|rem|%|mm|cm|in|ex|ch|vw|vh)")) return true;
        // Absolute size keywords
        return Set.of("xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large",
                "smaller", "larger").contains(check);
    }

    private void applyHtmlAttributes(Element element, ComputedStyle style, String tagName) {
        // bgcolor attribute
        String bgcolor = DomUtils.attr(element, "bgcolor");
        if (bgcolor != null && style.getBackgroundColor() == null) {
            style.set("background-color", bgcolor);
        }

        // align attribute — only map to text-align for elements where it means text alignment.
        // On <table>, align="center" maps to auto margins (CSS centering).
        // On <img>, align means float positioning.
        String align = DomUtils.attr(element, "align");
        if (align != null) {
            if ("center".equalsIgnoreCase(align) && tagName.equals("table")) {
                if (style.get("margin-left") == null) style.set("margin-left", "auto");
                if (style.get("margin-right") == null) style.set("margin-right", "auto");
            } else if (!tagName.equals("table") && !tagName.equals("img")
                    && style.get("text-align") == null) {
                style.set("text-align", align.toLowerCase());
            }
        }

        // cellpadding on tables (applied to cells during box tree building)
        // cellspacing on tables
        String cellspacing = DomUtils.attr(element, "cellspacing");
        if (cellspacing != null && tagName.equals("table")) {
            style.set("border-spacing", cellspacing + "px");
        }

        // border attribute on tables (e.g. border="0" suppresses default borders)
        String border = DomUtils.attr(element, "border");
        if (border != null && tagName.equals("table")) {
            float borderWidth = 0;
            try { borderWidth = Float.parseFloat(border.trim()); } catch (NumberFormatException ignored) {}
            String bw = borderWidth + "px";
            // Only set if CSS hasn't already specified borders
            if (style.get("border-top-width") == null) style.set("border-top-width", bw);
            if (style.get("border-right-width") == null) style.set("border-right-width", bw);
            if (style.get("border-bottom-width") == null) style.set("border-bottom-width", bw);
            if (style.get("border-left-width") == null) style.set("border-left-width", bw);
            if (borderWidth > 0) {
                if (style.get("border-top-style") == null) style.set("border-top-style", "solid");
                if (style.get("border-right-style") == null) style.set("border-right-style", "solid");
                if (style.get("border-bottom-style") == null) style.set("border-bottom-style", "solid");
                if (style.get("border-left-style") == null) style.set("border-left-style", "solid");
            }
        }

        // valign attribute
        String valign = DomUtils.attr(element, "valign");
        if (valign != null) {
            style.set("vertical-align", valign.toLowerCase());
        }

        // height attribute (on td, tr, div, etc.)
        String height = DomUtils.attr(element, "height");
        if (height != null && style.get("height") == null) {
            // Normalize bare numbers to px (e.g., height="8" → "8px")
            if (height.matches("\\d+(\\.\\d+)?")) {
                height = height + "px";
            }
            style.set("height", height);
        }

        // width attribute on td/th — maps to CSS width (HTML presentational attribute).
        // Browsers respect this as a column sizing hint; without it, column widths are
        // computed purely from max-content text, producing misaligned columns between
        // sibling tables that share a visual layout (e.g. header+data rows in separate tables).
        if ((tagName.equals("td") || tagName.equals("th")) && style.get("width") == null) {
            String widthAttr = DomUtils.attr(element, "width");
            if (widthAttr != null) {
                // Normalize bare numbers to px (e.g., width="150" → "150px")
                if (widthAttr.matches("\\d+(\\.\\d+)?")) widthAttr = widthAttr + "px";
                style.set("width", widthAttr);
            }
        }

        // Default table cells to left-align (HTML5 UA stylesheet: td { text-align: start }).
        // Without this, td inherits text-align from ancestors (e.g., body { text-align: center }).
        if (tagName.equals("td") && style.get("text-align") == null) {
            style.set("text-align", "left");
        }
    }

    private boolean isBorderWidth(String value) {
        return value.equals("thin") || value.equals("medium") || value.equals("thick")
                || value.matches("[\\d.]+(px|pt|em|mm)?");
    }

    private boolean isBorderStyle(String value) {
        return Set.of("none", "solid", "dashed", "dotted", "double", "groove", "ridge", "inset", "outset", "hidden")
                .contains(value.toLowerCase());
    }

    private record MatchedRule(CssSpecificity specificity, int sourceOrder, List<CssDeclaration> declarations) {}
}
