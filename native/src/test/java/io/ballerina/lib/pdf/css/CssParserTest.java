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

import io.ballerina.lib.pdf.HtmlPreprocessor;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssParserTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();
    private final CssParser parser = new CssParser();

    private Document parse(String html) {
        return preprocessor.preprocess(html);
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

    // ===== At-rule handling =====

    @Test
    void stripsMediaScreenBlock() {
        Document doc = parse("<html><head><style>"
                + "@media screen { .foo { color: green; } }"
                + "p { color: red; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size(), "@media screen rules should not be included");
        assertEquals("p", rules.get(0).selector().getRaw());
    }

    @Test
    void extractsMediaPrintRules() {
        Document doc = parse("<html><head><style>"
                + "@media print { .foo { color: black; } }"
                + "p { color: red; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(2, rules.size(), "@media print inner rules should be extracted");

        boolean hasFoo = rules.stream().anyMatch(r -> r.selector().getRaw().equals(".foo"));
        boolean hasP = rules.stream().anyMatch(r -> r.selector().getRaw().equals("p"));
        assertTrue(hasFoo, "Should extract .foo from @media print");
        assertTrue(hasP, "Should keep regular p rule");
    }

    @Test
    void stripsKeyframesBlock() {
        Document doc = parse("<html><head><style>"
                + "@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }"
                + "p { color: red; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size(), "@keyframes should not produce style rules");
        assertEquals("p", rules.get(0).selector().getRaw());
    }

    @Test
    void stripsSupportsBlock() {
        Document doc = parse("<html><head><style>"
                + "@supports (display: grid) { .container { display: grid; } }"
                + "p { color: red; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size(), "@supports should not produce style rules");
        assertEquals("p", rules.get(0).selector().getRaw());
    }

    @Test
    void preservesRegularRulesAroundAtRules() {
        Document doc = parse("<html><head><style>"
                + "h1 { font-size: 20px; }"
                + "@media screen { .nav { display: none; } }"
                + "p { color: blue; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(2, rules.size(), "Both regular rules should survive around @media screen");
        assertEquals("h1", rules.get(0).selector().getRaw());
        assertEquals("p", rules.get(1).selector().getRaw());
    }

    @Test
    void handlesNestedBracesInKeyframes() {
        Document doc = parse("<html><head><style>"
                + "@keyframes bounce { 0% { transform: translateY(0); } 50% { transform: translateY(-20px); } 100% { transform: translateY(0); } }"
                + ".result { font-weight: bold; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size(), "Rule after @keyframes should be parsed correctly");
        assertEquals(".result", rules.get(0).selector().getRaw());
        assertEquals("font-weight", rules.get(0).declarations().get(0).property());
    }

    @Test
    void handlesMultipleAtRulesInterspersed() {
        Document doc = parse("<html><head><style>"
                + "h1 { color: red; }"
                + "@media screen { .x { display: none; } }"
                + "h2 { color: green; }"
                + "@keyframes fade { 0% { opacity: 0; } 100% { opacity: 1; } }"
                + "h3 { color: blue; }"
                + "@media print { .printonly { display: block; } }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        // h1, h2, h3 (regular) + .printonly (from @media print) = 4
        assertEquals(4, rules.size(),
                "Should have 3 regular rules + 1 from @media print");
    }

    @Test
    void stripsAtFontFace() {
        Document doc = parse("<html><head><style>"
                + "@font-face { font-family: 'MyFont'; src: url('font.woff2'); }"
                + "p { color: red; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(1, rules.size(), "@font-face should not produce a style rule");
        assertEquals("p", rules.get(0).selector().getRaw());
    }

    @Test
    void handlesMalformedCssGracefully() {
        Document doc = parse("<html><head><style>"
                + "p { color: red; }"
                + "this is not valid css {"
                + "h1 { font-size: 20px; }"
                + "</style></head><body></body></html>");
        CssStylesheet stylesheet = parser.parse(doc);

        // Should not crash; at least some valid rules should be parsed
        List<CssRule> rules = stylesheet.getRules();
        assertFalse(rules.isEmpty(), "Should parse at least some valid rules from malformed CSS");
    }

    // ===== additionalCss overload =====

    @Test
    void additionalCssRulesAppendedAfterDocumentRules() {
        Document doc = parse("<html><head><style>p { color: red; }</style></head><body><p>test</p></body></html>");
        CssStylesheet stylesheet = parser.parse(doc, "p { color: blue; }");

        List<CssRule> rules = stylesheet.getRules();
        assertEquals(2, rules.size(), "Should have document rule + additionalCss rule");

        // additionalCss rule should have strictly higher source order
        assertTrue(rules.get(1).sourceOrder() > rules.get(0).sourceOrder(),
                "additionalCss rule should have higher source order than document rule");
    }

    @Test
    void additionalCssOverridesDocumentStyleAtEqualSpecificity() {
        Document doc = parse("<html><head><style>p { color: red; }</style></head><body><p id=\"t\">test</p></body></html>");
        CssStylesheet stylesheet = parser.parse(doc, "p { color: blue; }");

        // Resolve styles to verify cascade behavior
        StyleResolver resolver = new StyleResolver(stylesheet);
        List<org.w3c.dom.Element> paragraphs = io.ballerina.lib.pdf.util.DomUtils.findAll(doc, "p");
        assertFalse(paragraphs.isEmpty());
        ComputedStyle style = resolver.resolve(paragraphs.get(0));
        assertEquals("blue", style.get("color"), "additionalCss should override document CSS at equal specificity");
    }

    @Test
    void additionalCssNullBehavesLikeSingleArgParse() {
        Document doc = parse("<html><head><style>p { color: red; }</style></head><body></body></html>");
        CssStylesheet withNull = parser.parse(doc, null);
        // Re-parse since parse consumes the doc
        Document doc2 = parse("<html><head><style>p { color: red; }</style></head><body></body></html>");
        CssStylesheet withoutArg = parser.parse(doc2);

        assertEquals(withoutArg.getRules().size(), withNull.getRules().size(),
                "parse(doc, null) should produce same rules as parse(doc)");
    }
}
