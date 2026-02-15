package io.ballerina.lib.pdf.css;

import java.util.List;

/**
 * A CSS rule: selector + list of declarations.
 */
public record CssRule(CssSelector selector, List<CssDeclaration> declarations, int sourceOrder) {
}
