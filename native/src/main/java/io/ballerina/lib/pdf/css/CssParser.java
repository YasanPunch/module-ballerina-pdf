package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS from &lt;style&gt; blocks and inline style attributes.
 * Uses regex-based splitting — sufficient for simple embedded CSS stylesheets.
 */
public class CssParser {

    // Matches a CSS rule: selector { declarations }
    private static final Pattern RULE_PATTERN =
            Pattern.compile("([^{}@]+)\\{([^}]*)}", Pattern.DOTALL);

    // Matches @page { declarations }
    private static final Pattern PAGE_RULE_PATTERN =
            Pattern.compile("@page\\s*\\{([^}]*)}", Pattern.DOTALL);

    /**
     * Parses all &lt;style&gt; blocks in the document into a stylesheet.
     */
    public CssStylesheet parse(Document document) {
        CssStylesheet stylesheet = new CssStylesheet();
        int sourceOrder = 0;

        // Find all <style> elements
        List<Element> styleElements = DomUtils.findAll(document, "style");
        for (Element styleEl : styleElements) {
            String cssText = styleEl.getTextContent();
            if (cssText == null || cssText.isBlank()) continue;

            // Parse @page rules first
            Matcher pageMatcher = PAGE_RULE_PATTERN.matcher(cssText);
            while (pageMatcher.find()) {
                List<CssDeclaration> decls = parseDeclarations(pageMatcher.group(1));
                for (CssDeclaration decl : decls) {
                    stylesheet.addPageDeclaration(decl);
                }
            }

            // Remove @page rules before parsing regular rules
            String regularCss = PAGE_RULE_PATTERN.matcher(cssText).replaceAll("");

            // Strip CSS comments (/* ... */) — they confuse the RULE_PATTERN regex
            regularCss = regularCss.replaceAll("(?s)/\\*.*?\\*/", "");

            // Parse regular rules
            Matcher ruleMatcher = RULE_PATTERN.matcher(regularCss);
            while (ruleMatcher.find()) {
                String selectorGroup = ruleMatcher.group(1).trim();
                String declarationBlock = ruleMatcher.group(2).trim();
                List<CssDeclaration> declarations = parseDeclarations(declarationBlock);

                // Handle comma-separated selectors (e.g., "h1, h2, h3 { ... }")
                String[] selectors = selectorGroup.split(",");
                for (String sel : selectors) {
                    sel = sel.trim();
                    if (sel.isEmpty()) continue;
                    CssSelector selector = new CssSelector(sel);
                    stylesheet.addRule(new CssRule(selector, declarations, sourceOrder++));
                }
            }
        }

        return stylesheet;
    }

    /**
     * Parses an inline style attribute into a list of declarations.
     */
    public List<CssDeclaration> parseInlineStyle(String style) {
        if (style == null || style.isBlank()) return List.of();
        return parseDeclarations(style);
    }

    /**
     * Parses a CSS declaration block (semicolon-separated property:value pairs).
     */
    public static List<CssDeclaration> parseDeclarations(String block) {
        List<CssDeclaration> declarations = new ArrayList<>();
        if (block == null || block.isBlank()) return declarations;

        // Split by semicolons but not inside parentheses (for data URLs, rgb(), etc.)
        String[] parts = splitDeclarations(block);

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            int colonIdx = part.indexOf(':');
            if (colonIdx < 0) continue;

            String property = part.substring(0, colonIdx).trim().toLowerCase();
            String value = part.substring(colonIdx + 1).trim();

            // Check for !important
            boolean important = false;
            if (value.toLowerCase().contains("!important")) {
                important = true;
                value = value.replaceAll("!\\s*important", "").trim();
            }

            if (!property.isEmpty() && !value.isEmpty()) {
                declarations.add(new CssDeclaration(property, value, important));
            }
        }

        return declarations;
    }

    /**
     * Splits a declaration block by semicolons, respecting parentheses and quotes.
     */
    private static String[] splitDeclarations(String block) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < block.length(); i++) {
            char ch = block.charAt(i);

            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '(') parenDepth++;
                else if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
                else if (ch == ';' && parenDepth == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }
}
