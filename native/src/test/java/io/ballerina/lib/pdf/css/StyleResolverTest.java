package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.HtmlPreprocessor;
import io.ballerina.lib.pdf.util.DomUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StyleResolverTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();

    /**
     * Builds a DOM from HTML, parses CSS, resolves styles, and returns the
     * ComputedStyle for the element with the given ID.
     */
    private ComputedStyle resolveStyle(String html, String targetId) {
        Document doc = preprocessor.parseOnly(html);
        CssStylesheet sheet = new CssParser().parse(doc);
        StyleResolver resolver = new StyleResolver(sheet);
        Element target = findById(doc, targetId);
        assertNotNull(target, "Element with id='" + targetId + "' not found");
        return resolver.resolve(target);
    }

    /**
     * Resolves style for a specific tag (first occurrence).
     */
    private ComputedStyle resolveStyleByTag(String html, String tagName) {
        Document doc = preprocessor.parseOnly(html);
        CssStylesheet sheet = new CssParser().parse(doc);
        StyleResolver resolver = new StyleResolver(sheet);
        List<Element> found = DomUtils.findAll(doc, tagName);
        assertFalse(found.isEmpty(), "Expected to find <" + tagName + ">");
        return resolver.resolve(found.get(0));
    }

    private Element findById(Document doc, String id) {
        return findByIdRecursive(doc.getDocumentElement(), id);
    }

    private Element findByIdRecursive(Element el, String id) {
        if (id.equals(el.getAttribute("id"))) return el;
        for (Element child : DomUtils.childElements(el)) {
            Element found = findByIdRecursive(child, id);
            if (found != null) return found;
        }
        return null;
    }

    // ===== Cascade & Specificity =====

    @Test
    void uaStylesApplied() {
        String html = "<html><body><h1 id=\"h\">Title</h1><strong id=\"s\">Bold</strong></body></html>";
        ComputedStyle h1Style = resolveStyle(html, "h");
        assertEquals("bold", h1Style.get("font-weight"));
        assertEquals("24px", h1Style.get("font-size"));

        ComputedStyle strongStyle = resolveStyle(html, "s");
        assertEquals("bold", strongStyle.get("font-weight"));
    }

    @Test
    void stylesheetOverridesUaStyles() {
        String html = "<html><head><style>h1 { font-size: 10px; }</style></head>"
                + "<body><h1 id=\"h\">Title</h1></body></html>";
        ComputedStyle style = resolveStyle(html, "h");
        assertEquals("10px", style.get("font-size"));
    }

    @Test
    void higherSpecificityWins() {
        String html = "<html><head><style>"
                + "p { color: green; }"
                + ".cls { color: blue; }"
                + "#target { color: red; }"
                + "</style></head>"
                + "<body><p id=\"target\" class=\"cls\">text</p></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("red", style.get("color"));
    }

    @Test
    void laterSourceOrderWinsAtEqualSpecificity() {
        String html = "<html><head><style>"
                + ".a { color: red; }"
                + ".b { color: blue; }"
                + "</style></head>"
                + "<body><p id=\"target\" class=\"a b\">text</p></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("blue", style.get("color"));
    }

    @Test
    void inlineStyleOverridesStylesheet() {
        String html = "<html><head><style>"
                + "p { color: blue; }"
                + "</style></head>"
                + "<body><p id=\"target\" style=\"color: red;\">text</p></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("red", style.get("color"));
    }

    @Test
    void importantOverridesInlineStyle() {
        String html = "<html><head><style>"
                + "p { color: blue !important; }"
                + "</style></head>"
                + "<body><p id=\"target\" style=\"color: red;\">text</p></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("blue", style.get("color"));
    }

    // ===== Shorthand Expansion: margin =====

    @Test
    void expandsMarginOneSide() {
        String html = "<html><head><style>"
                + "#target { margin: 10px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("10px", style.get("margin-top"));
        assertEquals("10px", style.get("margin-right"));
        assertEquals("10px", style.get("margin-bottom"));
        assertEquals("10px", style.get("margin-left"));
    }

    @Test
    void expandsMarginTwoSides() {
        String html = "<html><head><style>"
                + "#target { margin: 10px 20px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("10px", style.get("margin-top"));
        assertEquals("20px", style.get("margin-right"));
        assertEquals("10px", style.get("margin-bottom"));
        assertEquals("20px", style.get("margin-left"));
    }

    @Test
    void expandsMarginThreeSides() {
        String html = "<html><head><style>"
                + "#target { margin: 10px 20px 30px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("10px", style.get("margin-top"));
        assertEquals("20px", style.get("margin-right"));
        assertEquals("30px", style.get("margin-bottom"));
        assertEquals("20px", style.get("margin-left"));
    }

    @Test
    void expandsMarginFourSides() {
        String html = "<html><head><style>"
                + "#target { margin: 10px 20px 30px 40px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("10px", style.get("margin-top"));
        assertEquals("20px", style.get("margin-right"));
        assertEquals("30px", style.get("margin-bottom"));
        assertEquals("40px", style.get("margin-left"));
    }

    // ===== Shorthand Expansion: padding =====

    @Test
    void expandsPaddingSameAsMargin() {
        String html = "<html><head><style>"
                + "#target { padding: 5px 10px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("5px", style.get("padding-top"));
        assertEquals("10px", style.get("padding-right"));
        assertEquals("5px", style.get("padding-bottom"));
        assertEquals("10px", style.get("padding-left"));
    }

    // ===== Shorthand Expansion: border =====

    @Test
    void expandsBorderShorthand() {
        String html = "<html><head><style>"
                + "#target { border: 1px solid black; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        // All 4 sides should have width, style, color
        assertEquals("1px", style.get("border-top-width"));
        assertEquals("solid", style.get("border-top-style"));
        assertEquals("black", style.get("border-top-color"));
        assertEquals("1px", style.get("border-bottom-width"));
        assertEquals("solid", style.get("border-bottom-style"));
        assertEquals("black", style.get("border-bottom-color"));
        assertEquals("1px", style.get("border-left-width"));
        assertEquals("1px", style.get("border-right-width"));
    }

    @Test
    void expandsBorderSide() {
        String html = "<html><head><style>"
                + "#target { border-top: 2px dashed red; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("2px", style.get("border-top-width"));
        assertEquals("dashed", style.get("border-top-style"));
        assertEquals("red", style.get("border-top-color"));
        // Other sides should not be set by this shorthand
        assertNull(style.get("border-bottom-width"));
    }

    @Test
    void expandsBorderWidth() {
        String html = "<html><head><style>"
                + "#target { border-width: 1px 2px; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("1px", style.get("border-top-width"));
        assertEquals("2px", style.get("border-right-width"));
        assertEquals("1px", style.get("border-bottom-width"));
        assertEquals("2px", style.get("border-left-width"));
    }

    // ===== Shorthand Expansion: background =====

    @Test
    void expandsBackgroundColor() {
        String html = "<html><head><style>"
                + "#target { background: red; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("red", style.get("background-color"));
    }

    @Test
    void expandsBackgroundImage() {
        String html = "<html><head><style>"
                + "#target { background: url(data:image/png;base64,abc); }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("url(data:image/png;base64,abc)", style.get("background-image"));
    }

    // ===== Shorthand Expansion: font =====

    @Test
    void expandsFontShorthand() {
        String html = "<html><head><style>"
                + "#target { font: bold 14px Arial; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("bold", style.get("font-weight"));
        assertEquals("14px", style.get("font-size"));
        assertEquals("Arial", style.get("font-family"));
    }

    @Test
    void expandsFontFullShorthand() {
        String html = "<html><head><style>"
                + "#target { font: italic bold 12px/1.5 'Liberation Sans', serif; }"
                + "</style></head><body><div id=\"target\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("italic", style.get("font-style"));
        assertEquals("bold", style.get("font-weight"));
        assertEquals("12px", style.get("font-size"));
        assertEquals("1.5", style.get("line-height"));
        assertEquals("'Liberation Sans', serif", style.get("font-family"));
    }

    // ===== Inheritance =====

    @Test
    void inheritsColor() {
        String html = "<html><head><style>"
                + "#parent { color: red; }"
                + "</style></head>"
                + "<body><div id=\"parent\"><p id=\"child\">text</p></div></body></html>";
        ComputedStyle style = resolveStyle(html, "child");
        assertEquals("red", style.get("color"));
    }

    @Test
    void inheritsFontFamily() {
        String html = "<html><head><style>"
                + "#parent { font-family: Arial; }"
                + "</style></head>"
                + "<body><div id=\"parent\"><span id=\"child\">text</span></div></body></html>";
        ComputedStyle style = resolveStyle(html, "child");
        assertEquals("Arial", style.get("font-family"));
    }

    @Test
    void doesNotInheritMargin() {
        String html = "<html><head><style>"
                + "#parent { margin-top: 20px; }"
                + "</style></head>"
                + "<body><div id=\"parent\"><p id=\"child\">text</p></div></body></html>";
        ComputedStyle style = resolveStyle(html, "child");
        // p gets UA margin, but not the parent's explicit margin-top
        assertNotEquals("20px", style.get("margin-top"));
    }

    @Test
    void doesNotInheritPadding() {
        String html = "<html><head><style>"
                + "#parent { padding-left: 15px; }"
                + "</style></head>"
                + "<body><div id=\"parent\"><span id=\"child\">text</span></div></body></html>";
        ComputedStyle style = resolveStyle(html, "child");
        assertNull(style.get("padding-left"));
    }

    @Test
    void explicitValueOverridesInherited() {
        String html = "<html><head><style>"
                + "#parent { color: red; }"
                + "#child { color: blue; }"
                + "</style></head>"
                + "<body><div id=\"parent\"><p id=\"child\">text</p></div></body></html>";
        ComputedStyle style = resolveStyle(html, "child");
        assertEquals("blue", style.get("color"));
    }

    // ===== HTML Attribute Mapping =====

    @Test
    void bgcolorMapsToBackgroundColor() {
        String html = "<html><body><div id=\"target\" bgcolor=\"#ff0000\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("#ff0000", style.get("background-color"));
    }

    @Test
    void alignMapsToTextAlign() {
        String html = "<html><body><p id=\"target\" align=\"center\">x</p></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("center", style.get("text-align"));
    }

    @Test
    void valignMapsToVerticalAlign() {
        String html = "<html><body><table><tr><td id=\"target\" valign=\"middle\">x</td></tr></table></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("middle", style.get("vertical-align"));
    }

    @Test
    void tableBorderMapsToAllSides() {
        String html = "<html><body><table id=\"target\" border=\"1\"><tr><td>x</td></tr></table></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("1.0px", style.get("border-top-width"));
        assertEquals("solid", style.get("border-top-style"));
        assertEquals("1.0px", style.get("border-right-width"));
        assertEquals("solid", style.get("border-right-style"));
        assertEquals("1.0px", style.get("border-bottom-width"));
        assertEquals("solid", style.get("border-bottom-style"));
        assertEquals("1.0px", style.get("border-left-width"));
        assertEquals("solid", style.get("border-left-style"));
    }

    @Test
    void cellspacingMapsToBorderSpacing() {
        String html = "<html><body><table id=\"target\" cellspacing=\"5\"><tr><td>x</td></tr></table></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("5px", style.get("border-spacing"));
    }

    @Test
    void heightAttributeMapsToHeight() {
        String html = "<html><body><div id=\"target\" height=\"100\">x</div></body></html>";
        ComputedStyle style = resolveStyle(html, "target");
        assertEquals("100px", style.get("height"));
    }
}
