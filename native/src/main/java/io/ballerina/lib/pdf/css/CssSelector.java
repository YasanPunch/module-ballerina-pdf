package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a CSS selector and can match it against a W3C DOM element.
 * Supports: tag, .class, #id, descendant (space), :nth-of-type(n), :nth-child(n).
 * Compound selectors (e.g. "div.foo") and combinators (space = descendant).
 */
public class CssSelector {

    private static final Pattern NTH_PATTERN = Pattern.compile(":nth-(?:of-type|child)\\(([^)]+)\\)");
    private static final Pattern PSEUDO_SPLIT = Pattern.compile("(:[a-z-]+(?:\\([^)]*\\))?)");

    private final String raw;
    private final CssSpecificity specificity;

    public CssSelector(String raw) {
        this.raw = raw.trim();
        this.specificity = computeSpecificity(this.raw);
    }

    public String getRaw() {
        return raw;
    }

    public CssSpecificity getSpecificity() {
        return specificity;
    }

    /**
     * Tests whether this selector matches the given element.
     */
    public boolean matches(Element element) {
        // Split into parts by whitespace (descendant combinator)
        String[] parts = raw.trim().split("\\s+");
        return matchDescendant(element, parts, parts.length - 1);
    }

    private boolean matchDescendant(Element element, String[] parts, int partIndex) {
        if (partIndex < 0) return true;

        String part = parts[partIndex];
        if (!matchSimple(element, part)) {
            return false;
        }

        if (partIndex == 0) return true;

        // Walk up ancestors looking for a match for the remaining parts
        Node parent = element.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            if (matchDescendant((Element) parent, parts, partIndex - 1)) {
                return true;
            }
            parent = parent.getParentNode();
        }
        return false;
    }

    /**
     * Matches a simple selector (no combinators) against an element.
     * Handles: tag, .class, #id, tag.class, .class1.class2, :nth-of-type(n), :nth-child(n)
     */
    private boolean matchSimple(Element element, String selector) {
        String sel = selector;

        // Extract and check pseudo-classes first
        Matcher nthMatcher = NTH_PATTERN.matcher(sel);
        while (nthMatcher.find()) {
            String arg = nthMatcher.group(1).trim();
            boolean isNthOfType = sel.substring(nthMatcher.start()).startsWith(":nth-of-type");
            int index = isNthOfType ? DomUtils.getNthOfType(element) : getNthChild(element);
            if (!matchesAnPlusB(arg, index)) return false;
        }

        // Remove all pseudo-classes for the rest of matching
        sel = PSEUDO_SPLIT.matcher(sel).replaceAll("");
        if (sel.isEmpty()) return true;

        String tagName = DomUtils.tagName(element);
        String elId = element.getAttribute("id");
        String elClasses = element.getAttribute("class");

        // Parse the selector into tag, id, classes
        String selTag = null;
        String selId = null;
        java.util.List<String> selClasses = new java.util.ArrayList<>();

        // Split by # and . while preserving what comes before
        StringBuilder current = new StringBuilder();
        char type = 't'; // t=tag, i=id, c=class
        for (int i = 0; i < sel.length(); i++) {
            char ch = sel.charAt(i);
            if (ch == '#' || ch == '.') {
                String token = current.toString();
                if (!token.isEmpty()) {
                    switch (type) {
                        case 't' -> selTag = token.toLowerCase();
                        case 'i' -> selId = token;
                        case 'c' -> selClasses.add(token);
                    }
                }
                current = new StringBuilder();
                type = (ch == '#') ? 'i' : 'c';
            } else {
                current.append(ch);
            }
        }
        String lastToken = current.toString();
        if (!lastToken.isEmpty()) {
            switch (type) {
                case 't' -> selTag = lastToken.toLowerCase();
                case 'i' -> selId = lastToken;
                case 'c' -> selClasses.add(lastToken);
            }
        }

        // Match tag
        if (selTag != null && !selTag.equals("*") && !selTag.equals(tagName)) {
            return false;
        }
        // Match id
        if (selId != null && !selId.equals(elId)) {
            return false;
        }
        // Match all classes
        for (String cls : selClasses) {
            if (!DomUtils.hasClass(element, cls)) {
                return false;
            }
        }

        return true;
    }

    private int getNthChild(Element el) {
        Node parent = el.getParentNode();
        if (parent == null) return 1;
        int count = 0;
        for (Element sibling : DomUtils.childElements(parent)) {
            count++;
            if (sibling == el) return count;
        }
        return 1;
    }

    /**
     * Matches the CSS An+B microsyntax against a 1-based index.
     * Handles: "even", "odd", plain integer, and full An+B expressions
     * like "2n+1", "3n", "-n+3", "n+1", "n", "-2n+6".
     */
    static boolean matchesAnPlusB(String arg, int index) {
        arg = arg.trim().toLowerCase();
        if (arg.equals("even")) return index % 2 == 0;
        if (arg.equals("odd")) return index % 2 == 1;

        // Try plain integer
        if (!arg.contains("n")) {
            try {
                return index == Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Parse An+B form
        // Normalize: remove spaces, convert "-" in "- B" to "-B"
        arg = arg.replaceAll("\\s+", "");

        int nPos = arg.indexOf('n');
        // Parse coefficient 'a' (before 'n')
        String aPart = arg.substring(0, nPos);
        int a;
        if (aPart.isEmpty() || aPart.equals("+")) {
            a = 1;
        } else if (aPart.equals("-")) {
            a = -1;
        } else {
            try {
                a = Integer.parseInt(aPart);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Parse constant 'b' (after 'n')
        String bPart = arg.substring(nPos + 1);
        int b;
        if (bPart.isEmpty()) {
            b = 0;
        } else {
            try {
                b = Integer.parseInt(bPart);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        // Match: index = a*k + b for some non-negative integer k
        if (a == 0) {
            return index == b;
        }
        int diff = index - b;
        if (diff % a != 0) return false;
        return diff / a >= 0;
    }

    private static CssSpecificity computeSpecificity(String selector) {
        // Remove pseudo-class arguments
        String sel = selector.replaceAll("\\([^)]*\\)", "");
        int ids = 0, classes = 0, tags = 0;

        // Count #id
        for (int i = 0; i < sel.length(); i++) {
            if (sel.charAt(i) == '#') ids++;
        }

        // Count .class and pseudo-classes (except :nth-*)
        String[] parts = sel.split("\\s+");
        for (String part : parts) {
            for (int i = 0; i < part.length(); i++) {
                if (part.charAt(i) == '.') classes++;
            }
            // Count pseudo-classes
            int pseudoCount = part.split(":").length - 1;
            classes += pseudoCount;

            // Count tag names (non-empty first segment before any . # :)
            String tagPart = part.replaceAll("[.#:].*", "");
            if (!tagPart.isEmpty() && !tagPart.equals("*")) tags++;
        }

        return new CssSpecificity(0, ids, classes, tags);
    }

    @Override
    public String toString() {
        return raw;
    }
}
