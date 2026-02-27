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

import io.ballerina.lib.pdf.HtmlPreprocessor;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomUtilsTest {

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
    void childElementsFiltersTextNodes() {
        Document doc = parse("<div>text<p>para</p>more<span>s</span></div>");
        Element div = findFirst(doc, "div");
        List<Element> elements = DomUtils.childElements(div);
        assertEquals(2, elements.size());
        assertEquals("p", DomUtils.tagName(elements.get(0)));
        assertEquals("span", DomUtils.tagName(elements.get(1)));
    }

    @Test
    void childNodesReturnsAll() {
        Document doc = parse("<div>text<p>para</p></div>");
        Element div = findFirst(doc, "div");
        List<Node> nodes = DomUtils.childNodes(div);
        assertTrue(nodes.size() >= 2, "Expected at least text + element nodes");
    }

    @Test
    void collapsesWhitespace() {
        Document doc = parse("<p>  hello   world  </p>");
        Element p = findFirst(doc, "p");
        List<Node> nodes = DomUtils.childNodes(p);
        // Find the text node
        for (Node n : nodes) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                String collapsed = DomUtils.getCollapsedText(n);
                assertEquals(" hello world ", collapsed);
                return;
            }
        }
        fail("No text node found");
    }

    @Test
    void hasClassMatchesExact() {
        Document doc = parse("<div class=\"foo bar\">x</div>");
        Element div = findFirst(doc, "div");
        assertTrue(DomUtils.hasClass(div, "foo"));
        assertTrue(DomUtils.hasClass(div, "bar"));
        assertFalse(DomUtils.hasClass(div, "baz"));
    }

    @Test
    void tagNameReturnsLowercase() {
        Document doc = parse("<DIV>x</DIV>");
        Element div = findFirst(doc, "div");
        assertEquals("div", DomUtils.tagName(div));
    }

    @Test
    void findChildFindsDirectChild() {
        Document doc = parse("<div><p>hello</p><span>world</span></div>");
        Element div = findFirst(doc, "div");
        Element p = DomUtils.findChild(div, "p");
        assertNotNull(p);
        assertEquals("p", DomUtils.tagName(p));

        Element missing = DomUtils.findChild(div, "h1");
        assertNull(missing);
    }

    @Test
    void findAllFindsDescendants() {
        Document doc = parse("<div><p>a</p><div><p>b</p></div></div>");
        Element body = findFirst(doc, "body");
        List<Element> paragraphs = DomUtils.findAll(body, "p");
        assertEquals(2, paragraphs.size());
    }

    @Test
    void getNthOfTypeReturns1Based() {
        Document doc = parse("<div><p>a</p><span>x</span><p>b</p><p>c</p></div>");
        Element div = findFirst(doc, "div");
        List<Element> children = DomUtils.childElements(div);

        // Filter only <p> elements
        List<Element> paragraphs = children.stream()
                .filter(e -> DomUtils.tagName(e).equals("p"))
                .toList();
        assertEquals(3, paragraphs.size());

        assertEquals(1, DomUtils.getNthOfType(paragraphs.get(0)));
        assertEquals(2, DomUtils.getNthOfType(paragraphs.get(1)));
        assertEquals(3, DomUtils.getNthOfType(paragraphs.get(2)));
    }
}
