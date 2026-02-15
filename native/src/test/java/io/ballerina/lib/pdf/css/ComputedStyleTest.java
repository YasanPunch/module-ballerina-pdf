package io.ballerina.lib.pdf.css;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComputedStyleTest {

    // --- Display ---

    @Test
    void getDisplayDefaultsToInline() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("inline", style.getDisplay());
    }

    @Test
    void getDisplayReturnsSetValue() {
        ComputedStyle style = new ComputedStyle();
        style.set("display", "block");
        assertEquals("block", style.getDisplay());
    }

    // --- Position ---

    @Test
    void getPositionDefaultsToStatic() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("static", style.getPosition());
    }

    @Test
    void getPositionReturnsSetValue() {
        ComputedStyle style = new ComputedStyle();
        style.set("position", "relative");
        assertEquals("relative", style.getPosition());
    }

    // --- Width ---

    @Test
    void getWidthReturnsNegativeForAuto() {
        ComputedStyle style = new ComputedStyle();
        // No width set → auto → -1
        assertEquals(-1f, style.getWidth(500f, 12f));
    }

    @Test
    void getWidthReturnsNegativeForExplicitAuto() {
        ComputedStyle style = new ComputedStyle();
        style.set("width", "auto");
        assertEquals(-1f, style.getWidth(500f, 12f));
    }

    @Test
    void getWidthParsesPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("width", "200px");
        // 200px * 0.75 = 150pt
        assertEquals(150f, style.getWidth(500f, 12f), 0.01f);
    }

    @Test
    void getWidthParsesPercentage() {
        ComputedStyle style = new ComputedStyle();
        style.set("width", "50%");
        // 50% of 500pt container = 250pt
        assertEquals(250f, style.getWidth(500f, 12f), 0.01f);
    }

    // --- Height ---

    @Test
    void getHeightReturnsNegativeForAuto() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(-1f, style.getHeight(500f, 12f));
    }

    // --- Offset properties (top, left, right, bottom) ---

    @Test
    void getTopReturnsNaNForAuto() {
        ComputedStyle style = new ComputedStyle();
        // No top set → auto → NaN
        assertTrue(Float.isNaN(style.getTop(500f, 12f)));
    }

    @Test
    void getTopParsesPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("top", "20px");
        // 20px * 0.75 = 15pt
        assertEquals(15f, style.getTop(500f, 12f), 0.01f);
    }

    @Test
    void getLeftRightBottomParsePixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("left", "10px");
        style.set("right", "30px");
        style.set("bottom", "40px");

        assertEquals(7.5f, style.getLeft(500f, 12f), 0.01f);   // 10 * 0.75
        assertEquals(22.5f, style.getRight(500f, 12f), 0.01f);  // 30 * 0.75
        assertEquals(30f, style.getBottom(500f, 12f), 0.01f);   // 40 * 0.75
    }

    // --- Max/Min width ---

    @Test
    void getMaxWidthDefaultsToMaxValue() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(Float.MAX_VALUE, style.getMaxWidth(500f, 12f));
    }

    @Test
    void getMinWidthDefaultsToZero() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(0f, style.getMinWidth(500f, 12f));
    }

    // --- Border width parsing ---

    @Test
    void parseBorderWidthKeywords() {
        ComputedStyle style = new ComputedStyle();

        style.set("border-top-width", "thin");
        assertEquals(0.75f, style.getBorderTopWidth(500f, 12f), 0.01f);

        style.set("border-right-width", "medium");
        assertEquals(1.5f, style.getBorderRightWidth(500f, 12f), 0.01f);

        style.set("border-bottom-width", "thick");
        assertEquals(2.25f, style.getBorderBottomWidth(500f, 12f), 0.01f);
    }

    @Test
    void parseBorderWidthPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("border-left-width", "2px");
        // 2px * 0.75 = 1.5pt
        assertEquals(1.5f, style.getBorderLeftWidth(500f, 12f), 0.01f);
    }

    // --- Text properties ---

    @Test
    void getTextAlignDefaultsToLeft() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("left", style.getTextAlign());
    }

    @Test
    void getTextTransformDefaultsToNone() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("none", style.getTextTransform());
    }

    @Test
    void getVerticalAlignDefaultsToBaseline() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("baseline", style.getVerticalAlign());
    }

    // --- Border style ---

    @Test
    void getBorderStyleDefaultsToNone() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("none", style.getBorderTopStyle());
        assertEquals("none", style.getBorderRightStyle());
        assertEquals("none", style.getBorderBottomStyle());
        assertEquals("none", style.getBorderLeftStyle());
    }

    // --- Border collapse ---

    @Test
    void getBorderCollapseDefaultsSeparate() {
        ComputedStyle style = new ComputedStyle();
        assertEquals("separate", style.getBorderCollapse());
    }

    // --- Inherited property identification ---

    @Test
    void isInheritedIdentifiesCorrectProperties() {
        // Inherited
        assertTrue(ComputedStyle.isInherited("color"));
        assertTrue(ComputedStyle.isInherited("font-size"));
        assertTrue(ComputedStyle.isInherited("font-family"));
        assertTrue(ComputedStyle.isInherited("text-align"));
        assertTrue(ComputedStyle.isInherited("line-height"));
        assertTrue(ComputedStyle.isInherited("border-collapse"));
        assertTrue(ComputedStyle.isInherited("border-spacing"));

        // Not inherited
        assertFalse(ComputedStyle.isInherited("margin-top"));
        assertFalse(ComputedStyle.isInherited("padding-left"));
        assertFalse(ComputedStyle.isInherited("border-top-width"));
        assertFalse(ComputedStyle.isInherited("display"));
        assertFalse(ComputedStyle.isInherited("width"));
        assertFalse(ComputedStyle.isInherited("height"));
    }
}
