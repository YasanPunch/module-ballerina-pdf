package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.HtmlPreprocessor;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssParserTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();
    private final CssParser parser = new CssParser();

    private Document parse(String html) {
        return preprocessor.parseOnly(html);
    }

    @Test
    void parsesSingleRule() {
        Document doc = parse("<html><head><style>p { color: red; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size());

        CssRule rule = rules.get(0);
        assertEquals("p", rule.selector().getRaw());
        assertEquals(1, rule.declarations().size());
        assertEquals("color", rule.declarations().get(0).property());
        assertEquals("red", rule.declarations().get(0).value());
    }

    @Test
    void parsesMultipleRules() {
        Document doc = parse("<html><head><style>p { color: red; } div { margin: 0; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);
        assertEquals(2, stylesheet.getRules().size());
    }

    @Test
    void parsesCommaSeparatedSelectors() {
        Document doc = parse("<html><head><style>h1, h2 { color: blue; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(2, rules.size());
        assertEquals("h1", rules.get(0).selector().getRaw());
        assertEquals("h2", rules.get(1).selector().getRaw());
        // Both should share same declaration
        assertEquals("color", rules.get(0).declarations().get(0).property());
        assertEquals("color", rules.get(1).declarations().get(0).property());
    }

    @Test
    void parsesPageRule() {
        Document doc = parse("<html><head><style>@page { size: A4; margin: 10mm; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssDeclaration> pageDecls = stylesheet.getPageDeclarations();
        assertFalse(pageDecls.isEmpty());

        boolean hasSize = pageDecls.stream().anyMatch(d -> d.property().equals("size"));
        boolean hasMargin = pageDecls.stream().anyMatch(d -> d.property().equals("margin"));
        assertTrue(hasSize);
        assertTrue(hasMargin);
    }

    @Test
    void parsesImportantDeclaration() {
        Document doc = parse("<html><head><style>p { color: red !important; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        CssDeclaration decl = stylesheet.getRules().get(0).declarations().get(0);
        assertEquals("color", decl.property());
        assertEquals("red", decl.value());
        assertTrue(decl.important());
    }

    @Test
    void stripsCssComments() {
        Document doc = parse("<html><head><style>/* comment */ p { color: red; }</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        assertEquals(1, stylesheet.getRules().size());
        assertEquals("color", stylesheet.getRules().get(0).declarations().get(0).property());
    }

    @Test
    void parsesInlineStyle() {
        List<CssDeclaration> decls = parser.parseInlineStyle("color: red; font-size: 14px");
        assertEquals(2, decls.size());
        assertEquals("color", decls.get(0).property());
        assertEquals("red", decls.get(0).value());
        assertEquals("font-size", decls.get(1).property());
        assertEquals("14px", decls.get(1).value());
    }

    @Test
    void splitsDeclarationsRespectingParens() {
        List<CssDeclaration> decls = CssParser.parseDeclarations(
                "background: url(data:image/png;base64,abc); color: red");
        assertEquals(2, decls.size());
        assertEquals("background", decls.get(0).property());
        assertTrue(decls.get(0).value().contains("url("));
        assertEquals("color", decls.get(1).property());
    }

    @Test
    void handlesEmptyStyleBlock() {
        Document doc = parse("<html><head><style></style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);
        assertTrue(stylesheet.getRules().isEmpty());
    }
}
