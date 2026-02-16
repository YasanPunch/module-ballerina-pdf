package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a CSS selector and can match it against a W3C DOM element.
 * Supports: tag, .class, #id, [attr], [attr=val], [attr^=val], [attr$=val], [attr*=val],
 * descendant (space), child (&gt;), adjacent sibling (+), general sibling (~),
 * :nth-of-type(An+B), :nth-child(An+B). Compound selectors (e.g. "div.foo[href]").
 */
public class CssSelector {

    private static final Pattern NTH_PATTERN = Pattern.compile(":nth-(?:of-type|child)\\(([^)]+)\\)");
    private static final Pattern NOT_PATTERN = Pattern.compile(":not\\(([^)]+)\\)");
    private static final Pattern PSEUDO_SPLIT = Pattern.compile("(:[a-z-]+(?:\\([^)]*\\))?)");
    private static final Pattern ATTR_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

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
        List<Object> tokens = tokenize(raw);
        if (tokens.isEmpty()) return false;
        int lastSelIdx = tokens.size() - 1;
        return matchFromRight(element, tokens, lastSelIdx);
    }

    /**
     * Tokenizes a selector string into alternating simple selectors (String) and
     * combinators (Character). Handles brackets, quotes, and combinator operators.
     * Result always starts and ends with a String (simple selector).
     */
    static List<Object> tokenize(String selector) {
        List<Object> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        boolean inQuote = false;
        char quoteChar = 0;

        String s = selector.trim();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // Handle content inside brackets (attribute selectors)
            if (bracketDepth > 0) {
                if (inQuote) {
                    current.append(c);
                    if (c == quoteChar) inQuote = false;
                    continue;
                }
                if (c == '\'' || c == '"') {
                    inQuote = true;
                    quoteChar = c;
                    current.append(c);
                    continue;
                }
                if (c == ']') bracketDepth--;
                current.append(c);
                continue;
            }

            if (c == '[') {
                bracketDepth++;
                current.append(c);
                continue;
            }

            // Combinator operators
            if (c == '>' || c == '+' || c == '~') {
                String sel = current.toString().trim();
                if (!sel.isEmpty()) {
                    tokens.add(sel);
                }
                tokens.add(c);
                current = new StringBuilder();
                continue;
            }

            // Whitespace: could be descendant combinator or padding around operator
            if (Character.isWhitespace(c)) {
                // Skip whitespace after a combinator operator (only if no new selector content has accumulated)
                if (current.toString().trim().isEmpty() && !tokens.isEmpty() && tokens.get(tokens.size() - 1) instanceof Character) {
                    continue;
                }
                // Peek ahead: if next non-whitespace is a combinator operator, skip
                int j = i + 1;
                while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
                if (j < s.length() && (s.charAt(j) == '>' || s.charAt(j) == '+' || s.charAt(j) == '~')) {
                    i = j - 1; // skip to just before the operator
                    continue;
                }
                // It's a descendant combinator (space)
                String sel = current.toString().trim();
                if (!sel.isEmpty()) {
                    tokens.add(sel);
                    tokens.add(' ');
                    current = new StringBuilder();
                }
                continue;
            }

            current.append(c);
        }

        String sel = current.toString().trim();
        if (!sel.isEmpty()) {
            tokens.add(sel);
        }

        return tokens;
    }

    /**
     * Matches the token list from right to left against the element and its context.
     *
     * @param element  the element to test
     * @param tokens   alternating simple selectors and combinators
     * @param selIndex index of the current simple selector in tokens
     */
    private boolean matchFromRight(Element element, List<Object> tokens, int selIndex) {
        if (selIndex < 0) return true;

        String selector = (String) tokens.get(selIndex);
        if (!matchSimple(element, selector)) return false;

        if (selIndex == 0) return true;

        // combinator is at selIndex - 1
        char combinator = (char) tokens.get(selIndex - 1);
        int nextSelIndex = selIndex - 2;

        switch (combinator) {
            case ' ': // descendant
                Node parent = element.getParentNode();
                while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                    if (matchFromRight((Element) parent, tokens, nextSelIndex)) return true;
                    parent = parent.getParentNode();
                }
                return false;

            case '>': // child
                Node p = element.getParentNode();
                if (p != null && p.getNodeType() == Node.ELEMENT_NODE) {
                    return matchFromRight((Element) p, tokens, nextSelIndex);
                }
                return false;

            case '+': // adjacent sibling
                Element prev = getPreviousElementSibling(element);
                if (prev != null) {
                    return matchFromRight(prev, tokens, nextSelIndex);
                }
                return false;

            case '~': // general sibling
                Element sib = getPreviousElementSibling(element);
                while (sib != null) {
                    if (matchFromRight(sib, tokens, nextSelIndex)) return true;
                    sib = getPreviousElementSibling(sib);
                }
                return false;

            default:
                return false;
        }
    }

    private static Element getPreviousElementSibling(Element el) {
        Node prev = el.getPreviousSibling();
        while (prev != null) {
            if (prev.getNodeType() == Node.ELEMENT_NODE) return (Element) prev;
            prev = prev.getPreviousSibling();
        }
        return null;
    }

    /**
     * Matches a simple selector (no combinators) against an element.
     * Handles: tag, .class, #id, [attr], [attr=val], :nth-of-type(n), :nth-child(n)
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

        String selWithoutNot = NOT_PATTERN.matcher(sel).replaceAll("");
        if (selWithoutNot.contains(":first-child") && getNthChild(element) != 1) return false;
        if (selWithoutNot.contains(":last-child") && !isLastChild(element)) return false;

        Matcher notMatcher = NOT_PATTERN.matcher(sel);
        while (notMatcher.find()) {
            String inner = notMatcher.group(1).trim();
            if (new CssSelector(inner).matches(element)) return false;
        }

        // Remove all pseudo-classes
        sel = PSEUDO_SPLIT.matcher(sel).replaceAll("");

        // Extract and check attribute selectors
        Matcher attrMatcher = ATTR_PATTERN.matcher(sel);
        while (attrMatcher.find()) {
            String attrExpr = attrMatcher.group(1);
            if (!matchAttribute(element, attrExpr)) return false;
        }

        // Remove attribute selectors for remaining parsing
        sel = ATTR_PATTERN.matcher(sel).replaceAll("");

        if (sel.isEmpty()) return true;

        String tagName = DomUtils.tagName(element);
        String elId = element.getAttribute("id");

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

    /**
     * Matches a single attribute selector expression (content between brackets).
     * Supports: attr, attr=val, attr^=val, attr$=val, attr*=val, attr~=val, attr|=val
     */
    private boolean matchAttribute(Element element, String expr) {
        // Find the operator
        int opIdx = -1;
        String operator = null;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '=') {
                if (i > 0 && "^$*~|".indexOf(expr.charAt(i - 1)) >= 0) {
                    operator = expr.substring(i - 1, i + 1);
                    opIdx = i - 1;
                } else {
                    operator = "=";
                    opIdx = i;
                }
                break;
            }
        }

        if (operator == null) {
            // [attr] — check existence
            return element.hasAttribute(expr.trim());
        }

        String attrName = expr.substring(0, opIdx).trim();
        String expected = expr.substring(opIdx + operator.length()).trim();
        // Remove quotes
        if (expected.length() >= 2 &&
                ((expected.startsWith("\"") && expected.endsWith("\"")) ||
                        (expected.startsWith("'") && expected.endsWith("'")))) {
            expected = expected.substring(1, expected.length() - 1);
        }

        if (!element.hasAttribute(attrName)) return false;
        String actual = element.getAttribute(attrName);

        return switch (operator) {
            case "=" -> actual.equals(expected);
            case "^=" -> actual.startsWith(expected);
            case "$=" -> actual.endsWith(expected);
            case "*=" -> actual.contains(expected);
            case "~=" -> {
                for (String word : actual.split("\\s+")) {
                    if (word.equals(expected)) {
                        yield true;
                    }
                }
                yield false;
            }
            case "|=" -> actual.equals(expected) || actual.startsWith(expected + "-");
            default -> false;
        };
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

    private boolean isLastChild(Element el) {
        Node parent = el.getParentNode();
        if (parent == null) return true;
        List<Element> siblings = DomUtils.childElements(parent);
        return !siblings.isEmpty() && siblings.get(siblings.size() - 1) == el;
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
        // Extract :not() arguments and compute their specificity separately.
        // :not() itself has zero specificity; only its argument contributes.
        int ids = 0, classes = 0, tags = 0;
        String sel = selector;

        Matcher notMatcher = NOT_PATTERN.matcher(sel);
        while (notMatcher.find()) {
            String inner = notMatcher.group(1).trim();
            CssSpecificity innerSpec = computeSpecificity(inner);
            ids += innerSpec.ids();
            classes += innerSpec.classes();
            tags += innerSpec.tags();
        }
        // Remove :not(...) entirely so its content isn't double-counted
        sel = NOT_PATTERN.matcher(sel).replaceAll("");

        // Remove pseudo-class arguments (e.g., nth-child(2n+1))
        sel = sel.replaceAll("\\([^)]*\\)", "");

        // Count attribute selectors (each counts as class-level specificity)
        Matcher attrMatcher = ATTR_PATTERN.matcher(sel);
        while (attrMatcher.find()) classes++;
        sel = ATTR_PATTERN.matcher(sel).replaceAll("");

        // Remove combinator characters for specificity counting
        sel = sel.replaceAll("\\s*[>+~]\\s*", " ");

        // Count #id
        for (int i = 0; i < sel.length(); i++) {
            if (sel.charAt(i) == '#') ids++;
        }

        // Count .class and pseudo-classes
        String[] parts = sel.trim().split("\\s+");
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
