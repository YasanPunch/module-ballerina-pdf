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
        return preprocessor.parseOnly(html);
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
}
