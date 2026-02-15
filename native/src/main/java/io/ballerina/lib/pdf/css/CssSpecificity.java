package io.ballerina.lib.pdf.css;

/**
 * CSS specificity tuple: (inline, ids, classes, tags).
 * Higher specificity wins in the cascade.
 */
public record CssSpecificity(int inline, int ids, int classes, int tags) implements Comparable<CssSpecificity> {

    public static final CssSpecificity ZERO = new CssSpecificity(0, 0, 0, 0);
    public static final CssSpecificity INLINE = new CssSpecificity(1, 0, 0, 0);

    @Override
    public int compareTo(CssSpecificity other) {
        if (this.inline != other.inline) return Integer.compare(this.inline, other.inline);
        if (this.ids != other.ids) return Integer.compare(this.ids, other.ids);
        if (this.classes != other.classes) return Integer.compare(this.classes, other.classes);
        return Integer.compare(this.tags, other.tags);
    }
}
