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
import io.ballerina.lib.pdf.util.DomUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CssSelectorTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();

    private Document parse(String html) {
        return preprocessor.preprocess(html);
    }

    private Element findFirst(Document doc, String tag) {
        List<Element> found = DomUtils.findAll(doc, tag);
        assertFalse(found.isEmpty(), "Expected to find <" + tag + ">");
        return found.get(0);
    }

    @Test
    void matchesTagSelector() {
        Document doc = parse("<div><p>text</p></div>");
        Element p = findFirst(doc, "p");
        Element div = findFirst(doc, "div");

        assertTrue(new CssSelector("p").matches(p));
        assertFalse(new CssSelector("p").matches(div));
    }

    @Test
    void matchesClassSelector() {
        Document doc = parse("<div class=\"foo\">a</div><div class=\"bar\">b</div>");
        List<Element> divs = DomUtils.findAll(doc, "div");
        Element foo = divs.get(0);
        Element bar = divs.get(1);

        assertTrue(new CssSelector(".foo").matches(foo));
        assertFalse(new CssSelector(".foo").matches(bar));
    }

    @Test
    void matchesIdSelector() {
        Document doc = parse("<div id=\"main\">x</div>");
        Element div = findFirst(doc, "div");
        assertTrue(new CssSelector("#main").matches(div));
        assertFalse(new CssSelector("#other").matches(div));
    }

    @Test
    void matchesCombinedTagClass() {
        Document doc = parse("<p class=\"highlight\">a</p><div class=\"highlight\">b</div>");
        Element p = findFirst(doc, "p");
        List<Element> divs = DomUtils.findAll(doc, "div");
        Element div = divs.get(0);

        CssSelector selector = new CssSelector("p.highlight");
        assertTrue(selector.matches(p));
        assertFalse(selector.matches(div));
    }

    @Test
    void matchesMultipleClasses() {
        Document doc = parse("<div class=\"a b\">x</div><div class=\"a\">y</div>");
        List<Element> divs = DomUtils.findAll(doc, "div");
        Element ab = divs.get(0);
        Element aOnly = divs.get(1);

        CssSelector selector = new CssSelector(".a.b");
        assertTrue(selector.matches(ab));
        assertFalse(selector.matches(aOnly));
    }

    @Test
    void matchesDescendantCombinator() {
        Document doc = parse("<div><p>inside</p></div><p>outside</p>");
        List<Element> ps = DomUtils.findAll(doc, "p");
        Element inside = ps.get(0);
        Element outside = ps.get(1);

        CssSelector selector = new CssSelector("div p");
        assertTrue(selector.matches(inside));
        assertFalse(selector.matches(outside));
    }

    @Test
    void matchesUniversalSelector() {
        Document doc = parse("<div>x</div><p>y</p>");
        Element div = findFirst(doc, "div");
        Element p = findFirst(doc, "p");

        CssSelector selector = new CssSelector("*");
        assertTrue(selector.matches(div));
        assertTrue(selector.matches(p));
    }

    @Test
    void matchesNthChild() {
        Document doc = parse("<div><p>1</p><p>2</p><p>3</p></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);

        CssSelector second = new CssSelector(":nth-child(2)");
        assertFalse(second.matches(children.get(0)));
        assertTrue(second.matches(children.get(1)));
        assertFalse(second.matches(children.get(2)));
    }

    @Test
    void matchesNthOfType() {
        Document doc = parse("<div><span>x</span><p>1</p><p>2</p></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);
        // children: span, p, p
        Element firstP = children.get(1);
        Element secondP = children.get(2);

        CssSelector selector = new CssSelector("p:nth-of-type(1)");
        assertTrue(selector.matches(firstP));
        assertFalse(selector.matches(secondP));
    }

    @Test
    void matchesAnPlusBEven() {
        assertTrue(CssSelector.matchesAnPlusB("even", 2));
        assertTrue(CssSelector.matchesAnPlusB("even", 4));
        assertFalse(CssSelector.matchesAnPlusB("even", 3));
    }

    @Test
    void matchesAnPlusBOdd() {
        assertTrue(CssSelector.matchesAnPlusB("odd", 1));
        assertTrue(CssSelector.matchesAnPlusB("odd", 3));
        assertFalse(CssSelector.matchesAnPlusB("odd", 2));
    }

    @Test
    void matchesAnPlusBExpression() {
        // 2n+1 matches 1, 3, 5, ...
        assertTrue(CssSelector.matchesAnPlusB("2n+1", 3));
        assertFalse(CssSelector.matchesAnPlusB("2n+1", 4));
    }

    @Test
    void matchesAnPlusBPlainInt() {
        assertTrue(CssSelector.matchesAnPlusB("3", 3));
        assertFalse(CssSelector.matchesAnPlusB("3", 2));
    }

    @Test
    void specificityCounts() {
        CssSpecificity tag = new CssSelector("p").getSpecificity();
        assertEquals(0, tag.ids());
        assertEquals(0, tag.classes());
        assertEquals(1, tag.tags());

        CssSpecificity cls = new CssSelector(".cls").getSpecificity();
        assertEquals(0, cls.ids());
        assertEquals(1, cls.classes());
        assertEquals(0, cls.tags());

        CssSpecificity id = new CssSelector("#id").getSpecificity();
        assertEquals(1, id.ids());
        assertEquals(0, id.classes());
        assertEquals(0, id.tags());

        CssSpecificity combined = new CssSelector("div.cls").getSpecificity();
        assertEquals(0, combined.ids());
        assertEquals(1, combined.classes());
        assertEquals(1, combined.tags());
    }

    @Test
    void specificityOrdering() {
        CssSpecificity tag = new CssSelector("p").getSpecificity();
        CssSpecificity cls = new CssSelector(".cls").getSpecificity();
        CssSpecificity id = new CssSelector("#id").getSpecificity();

        assertTrue(id.compareTo(cls) > 0);
        assertTrue(cls.compareTo(tag) > 0);
        assertTrue(id.compareTo(tag) > 0);
    }

    // --- Child combinator > ---

    @Test
    void matchesChildCombinator() {
        Document doc = parse("<div><p>direct</p></div>");
        Element p = findFirst(doc, "p");

        assertTrue(new CssSelector("div > p").matches(p));
    }

    @Test
    void childCombinatorRejectsNonDirect() {
        Document doc = parse("<div><span><p>nested</p></span></div>");
        Element p = findFirst(doc, "p");

        // p is a grandchild of div, not a direct child
        assertFalse(new CssSelector("div > p").matches(p));
        // but descendant combinator still works
        assertTrue(new CssSelector("div p").matches(p));
    }

    @Test
    void childCombinatorChained() {
        Document doc = parse("<div><ul><li>item</li></ul></div>");
        Element li = findFirst(doc, "li");

        assertTrue(new CssSelector("div > ul > li").matches(li));
        assertFalse(new CssSelector("div > li").matches(li));
    }

    // --- Adjacent sibling combinator + ---

    @Test
    void matchesAdjacentSibling() {
        Document doc = parse("<div><h1>title</h1><p>first</p><p>second</p></div>");
        List<Element> ps = DomUtils.findAll(doc, "p");
        Element firstP = ps.get(0);
        Element secondP = ps.get(1);

        // p immediately after h1
        assertTrue(new CssSelector("h1 + p").matches(firstP));
        // second p is not immediately after h1
        assertFalse(new CssSelector("h1 + p").matches(secondP));
    }

    // --- General sibling combinator ~ ---

    @Test
    void matchesGeneralSibling() {
        Document doc = parse("<div><h1>title</h1><p>first</p><p>second</p></div>");
        List<Element> ps = DomUtils.findAll(doc, "p");
        Element firstP = ps.get(0);
        Element secondP = ps.get(1);

        // both p elements are siblings after h1
        assertTrue(new CssSelector("h1 ~ p").matches(firstP));
        assertTrue(new CssSelector("h1 ~ p").matches(secondP));
    }

    @Test
    void generalSiblingRejectsBeforeSibling() {
        Document doc = parse("<div><p>before</p><h1>title</h1></div>");
        Element p = findFirst(doc, "p");

        // p comes before h1, not after
        assertFalse(new CssSelector("h1 ~ p").matches(p));
    }

    // --- Attribute selectors ---

    @Test
    void matchesAttributePresence() {
        Document doc = parse("<a href=\"http://example.com\">link</a><span>text</span>");
        Element a = findFirst(doc, "a");
        Element span = findFirst(doc, "span");

        assertTrue(new CssSelector("[href]").matches(a));
        assertFalse(new CssSelector("[href]").matches(span));
    }

    @Test
    void matchesAttributeExact() {
        Document doc = parse("<div data-type=\"primary\">a</div><div data-type=\"secondary\">b</div>");
        List<Element> divs = DomUtils.findAll(doc, "div");

        assertTrue(new CssSelector("[data-type=primary]").matches(divs.get(0)));
        assertFalse(new CssSelector("[data-type=primary]").matches(divs.get(1)));
    }

    @Test
    void matchesAttributeExactQuoted() {
        Document doc = parse("<div data-type=\"primary\">a</div>");
        Element div = findFirst(doc, "div");

        assertTrue(new CssSelector("[data-type=\"primary\"]").matches(div));
        assertTrue(new CssSelector("[data-type='primary']").matches(div));
    }

    @Test
    void matchesAttributeStartsWith() {
        Document doc = parse("<div class=\"btn-primary\">a</div><div class=\"label\">b</div>");
        List<Element> divs = DomUtils.findAll(doc, "div");

        assertTrue(new CssSelector("[class^=btn]").matches(divs.get(0)));
        assertFalse(new CssSelector("[class^=btn]").matches(divs.get(1)));
    }

    @Test
    void matchesAttributeEndsWith() {
        Document doc = parse("<a href=\"doc.pdf\">pdf</a><a href=\"doc.html\">html</a>");
        List<Element> links = DomUtils.findAll(doc, "a");

        assertTrue(new CssSelector("[href$=pdf]").matches(links.get(0)));
        assertFalse(new CssSelector("[href$=pdf]").matches(links.get(1)));
    }

    @Test
    void matchesAttributeContains() {
        Document doc = parse("<div class=\"foo-bar-baz\">a</div>");
        Element div = findFirst(doc, "div");

        assertTrue(new CssSelector("[class*=bar]").matches(div));
        assertFalse(new CssSelector("[class*=xyz]").matches(div));
    }

    @Test
    void matchesAttributeWithTag() {
        Document doc = parse("<input type=\"text\"/><input type=\"password\"/>");
        List<Element> inputs = DomUtils.findAll(doc, "input");

        CssSelector selector = new CssSelector("input[type=text]");
        assertTrue(selector.matches(inputs.get(0)));
        assertFalse(selector.matches(inputs.get(1)));
    }

    // --- Combinator + attribute combined ---

    @Test
    void matchesCombinatorWithAttribute() {
        Document doc = parse("<div><a href=\"http://example.com\">link</a></div>");
        Element a = findFirst(doc, "a");

        assertTrue(new CssSelector("div > a[href]").matches(a));
    }

    // --- Specificity with new features ---

    @Test
    void specificityWithCombinators() {
        // Combinators don't add specificity; only the simple selectors on each side do
        CssSpecificity descendant = new CssSelector("div p").getSpecificity();
        CssSpecificity child = new CssSelector("div > p").getSpecificity();
        assertEquals(descendant.tags(), child.tags());
        assertEquals(descendant.classes(), child.classes());
    }

    @Test
    void specificityWithAttributeSelector() {
        // [attr] counts as class-level specificity
        CssSpecificity attrSel = new CssSelector("[href]").getSpecificity();
        assertEquals(1, attrSel.classes());
        assertEquals(0, attrSel.tags());

        CssSpecificity tagAttr = new CssSelector("a[href]").getSpecificity();
        assertEquals(1, tagAttr.classes());
        assertEquals(1, tagAttr.tags());
    }

    // --- Tokenizer ---

    @Test
    void tokenizesDescendant() {
        List<Object> tokens = CssSelector.tokenize("div p");
        assertEquals(3, tokens.size());
        assertEquals("div", tokens.get(0));
        assertEquals(' ', tokens.get(1));
        assertEquals("p", tokens.get(2));
    }

    @Test
    void tokenizesChild() {
        List<Object> tokens = CssSelector.tokenize("div > p");
        assertEquals(3, tokens.size());
        assertEquals("div", tokens.get(0));
        assertEquals('>', tokens.get(1));
        assertEquals("p", tokens.get(2));
    }

    @Test
    void tokenizesAdjacentSibling() {
        List<Object> tokens = CssSelector.tokenize("h1 + p");
        assertEquals(3, tokens.size());
        assertEquals("h1", tokens.get(0));
        assertEquals('+', tokens.get(1));
        assertEquals("p", tokens.get(2));
    }

    @Test
    void tokenizesComplex() {
        List<Object> tokens = CssSelector.tokenize("div > p.foo + span .inner");
        assertEquals(7, tokens.size());
        assertEquals("div", tokens.get(0));
        assertEquals('>', tokens.get(1));
        assertEquals("p.foo", tokens.get(2));
        assertEquals('+', tokens.get(3));
        assertEquals("span", tokens.get(4));
        assertEquals(' ', tokens.get(5));
        assertEquals(".inner", tokens.get(6));
    }

    @Test
    void tokenizesAttributeWithSpaces() {
        // Brackets protect inner content from tokenization
        List<Object> tokens = CssSelector.tokenize("a[href^=\"http\"]");
        assertEquals(1, tokens.size());
        assertEquals("a[href^=\"http\"]", tokens.get(0));
    }

    // --- :first-child ---

    @Test
    void matchesFirstChild() {
        Document doc = parse("<div><p>first</p><p>second</p><p>third</p></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);

        CssSelector selector = new CssSelector("p:first-child");
        assertTrue(selector.matches(children.get(0)));
        assertFalse(selector.matches(children.get(1)));
        assertFalse(selector.matches(children.get(2)));
    }

    // --- :last-child ---

    @Test
    void matchesLastChild() {
        Document doc = parse("<div><p>first</p><p>second</p><p>third</p></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);

        CssSelector selector = new CssSelector("p:last-child");
        assertFalse(selector.matches(children.get(0)));
        assertFalse(selector.matches(children.get(1)));
        assertTrue(selector.matches(children.get(2)));
    }

    @Test
    void lastChildWithMixedTags() {
        Document doc = parse("<div><p>first</p><span>last</span></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);

        // :last-child matches span (the last child element regardless of tag)
        assertFalse(new CssSelector(":last-child").matches(children.get(0)));
        assertTrue(new CssSelector(":last-child").matches(children.get(1)));
    }

    // --- :not() ---

    @Test
    void matchesNotWithClass() {
        Document doc = parse("<div class=\"exclude\">a</div><div class=\"include\">b</div>");
        List<Element> divs = DomUtils.findAll(doc, "div");

        CssSelector selector = new CssSelector("div:not(.exclude)");
        assertFalse(selector.matches(divs.get(0)));
        assertTrue(selector.matches(divs.get(1)));
    }

    @Test
    void matchesNotWithPseudoClass() {
        Document doc = parse("<table><tr><td>1</td></tr><tr><td>2</td></tr><tr><td>3</td></tr></table>");
        List<Element> trs = DomUtils.findAll(doc, "tr");

        CssSelector selector = new CssSelector("tr:not(:first-child)");
        assertFalse(selector.matches(trs.get(0)));
        assertTrue(selector.matches(trs.get(1)));
        assertTrue(selector.matches(trs.get(2)));
    }

    // --- Specificity for :not() ---

    @Test
    void specificityOfNotUsesArgument() {
        // :not(.cls) → specificity of .cls = (0,1,0), :not itself contributes nothing
        CssSpecificity notClass = new CssSelector(":not(.cls)").getSpecificity();
        assertEquals(0, notClass.ids());
        assertEquals(1, notClass.classes());
        assertEquals(0, notClass.tags());

        // :not(#id) → specificity of #id = (1,0,0)
        CssSpecificity notId = new CssSelector(":not(#id)").getSpecificity();
        assertEquals(1, notId.ids());
        assertEquals(0, notId.classes());
        assertEquals(0, notId.tags());

        // div:not(.cls) → tag(1) + class(1) from :not arg
        CssSpecificity combined = new CssSelector("div:not(.cls)").getSpecificity();
        assertEquals(0, combined.ids());
        assertEquals(1, combined.classes());
        assertEquals(1, combined.tags());
    }
}
