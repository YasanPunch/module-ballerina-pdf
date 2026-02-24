package io.ballerina.lib.pdf.css;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed stylesheet: collection of rules + @page declarations.
 */
public class CssStylesheet {

    private final List<CssRule> rules = new ArrayList<>();
    private final List<CssDeclaration> pageDeclarations = new ArrayList<>();

    public void addRule(CssRule rule) {
        rules.add(rule);
    }

    public void addPageDeclaration(CssDeclaration decl) {
        pageDeclarations.add(decl);
    }

    public List<CssRule> getRules() {
        return rules;
    }

    public List<CssDeclaration> getPageDeclarations() {
        return pageDeclarations;
    }

    /**
     * Gets the @page size (returns "a4" by default).
     */
    public String getPageSize() {
        for (CssDeclaration decl : pageDeclarations) {
            if (decl.property().equals("size")) {
                return decl.value();
            }
        }
        return "a4";
    }

    /**
     * Gets the @page margin values as [top, right, bottom, left] in the raw CSS value.
     */
    public String getPageMargin() {
        for (CssDeclaration decl : pageDeclarations) {
            if (decl.property().equals("margin")) {
                return decl.value();
            }
        }
        return "15mm 10mm";
    }
}
