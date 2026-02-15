package io.ballerina.lib.pdf.css;

/**
 * A single CSS property:value declaration, optionally with !important.
 */
public record CssDeclaration(String property, String value, boolean important) {

    public CssDeclaration(String property, String value) {
        this(property, value, false);
    }

    @Override
    public String toString() {
        return property + ": " + value + (important ? " !important" : "");
    }
}
