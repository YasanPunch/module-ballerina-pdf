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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * W3C DOM traversal helpers.
 */
public class DomUtils {

    /**
     * Returns direct child elements of a node.
     */
    public static List<Element> childElements(Node parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) n);
            }
        }
        return children;
    }

    /**
     * Returns all direct child nodes (elements + text) of a node.
     */
    public static List<Node> childNodes(Node parent) {
        List<Node> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            children.add(nodes.item(i));
        }
        return children;
    }

    /**
     * Gets the text content of a text node, collapsing whitespace per CSS rules.
     */
    public static String getCollapsedText(Node textNode) {
        String raw = textNode.getTextContent();
        if (raw == null) return "";
        // Collapse runs of whitespace to single space (CSS default white-space: normal)
        return raw.replaceAll("\\s+", " ");
    }

    /**
     * Returns an element's attribute value, or null if not present.
     */
    public static String attr(Element el, String name) {
        String val = el.getAttribute(name);
        return (val == null || val.isEmpty()) ? null : val;
    }

    /**
     * Checks if an element has a specific CSS class.
     */
    public static boolean hasClass(Element el, String className) {
        String classes = el.getAttribute("class");
        if (classes == null || classes.isEmpty()) return false;
        for (String cls : classes.split("\\s+")) {
            if (cls.equals(className)) return true;
        }
        return false;
    }

    /**
     * Returns the tag name of a node in lowercase.
     */
    public static String tagName(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE) return "";
        return node.getLocalName() != null ? node.getLocalName().toLowerCase() : node.getNodeName().toLowerCase();
    }

    /**
     * Finds the first child element with the given tag name (direct children only).
     */
    public static Element findChild(Element parent, String tagName) {
        for (Element child : childElements(parent)) {
            if (tagName(child).equals(tagName)) return child;
        }
        return null;
    }

    /**
     * Finds all descendant elements matching a tag name.
     */
    public static List<Element> findAll(Node root, String tagName) {
        List<Element> results = new ArrayList<>();
        findAllRecursive(root, tagName, results);
        return results;
    }

    private static void findAllRecursive(Node node, String tagName, List<Element> results) {
        if (node.getNodeType() == Node.ELEMENT_NODE && tagName(node).equals(tagName)) {
            results.add((Element) node);
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            findAllRecursive(children.item(i), tagName, results);
        }
    }

    /**
     * Gets the index of an element among its siblings of the same type (1-based).
     */
    public static int getNthOfType(Element el) {
        String tag = tagName(el);
        Node parent = el.getParentNode();
        if (parent == null) return 1;
        int count = 0;
        for (Element sibling : childElements(parent)) {
            if (tagName(sibling).equals(tag)) {
                count++;
                if (sibling == el) return count;
            }
        }
        return 1;
    }
}
