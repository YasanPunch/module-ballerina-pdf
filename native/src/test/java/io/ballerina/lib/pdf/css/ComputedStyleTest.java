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
        style.set("border-top-style", "solid");
        assertEquals(0.75f, style.getBorderTopWidth(500f, 12f), 0.01f);

        style.set("border-right-width", "medium");
        style.set("border-right-style", "solid");
        assertEquals(1.5f, style.getBorderRightWidth(500f, 12f), 0.01f);

        style.set("border-bottom-width", "thick");
        style.set("border-bottom-style", "solid");
        assertEquals(2.25f, style.getBorderBottomWidth(500f, 12f), 0.01f);
    }

    @Test
    void parseBorderWidthPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("border-left-width", "2px");
        style.set("border-left-style", "solid");
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

    // --- Opacity ---

    @Test
    void getOpacityDefaultsToOne() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(1.0f, style.getOpacity());
    }

    @Test
    void getOpacityParsesFloat() {
        ComputedStyle style = new ComputedStyle();
        style.set("opacity", "0.5");
        assertEquals(0.5f, style.getOpacity(), 0.001f);
    }

    @Test
    void getOpacityClampsRange() {
        ComputedStyle style = new ComputedStyle();

        style.set("opacity", "-0.5");
        assertEquals(0f, style.getOpacity());

        style.set("opacity", "1.5");
        assertEquals(1.0f, style.getOpacity());
    }

    @Test
    void getOpacityHandlesInvalid() {
        ComputedStyle style = new ComputedStyle();
        style.set("opacity", "abc");
        assertEquals(1.0f, style.getOpacity());
    }

    // --- Letter-spacing / Word-spacing ---

    @Test
    void getLetterSpacingDefaultsToZero() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(0f, style.getLetterSpacing(12f));
    }

    @Test
    void getLetterSpacingParsesPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("letter-spacing", "2px");
        // 2px * 0.75 = 1.5pt
        assertEquals(1.5f, style.getLetterSpacing(12f), 0.01f);
    }

    @Test
    void getLetterSpacingNormalReturnsZero() {
        ComputedStyle style = new ComputedStyle();
        style.set("letter-spacing", "normal");
        assertEquals(0f, style.getLetterSpacing(12f));
    }

    @Test
    void getWordSpacingDefaultsToZero() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(0f, style.getWordSpacing(12f));
    }

    @Test
    void getWordSpacingParsesPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("word-spacing", "5px");
        // 5px * 0.75 = 3.75pt
        assertEquals(3.75f, style.getWordSpacing(12f), 0.01f);
    }

    // --- Border-radius ---

    @Test
    void getBorderRadiusDefaultsToZero() {
        ComputedStyle style = new ComputedStyle();
        assertEquals(0f, style.getBorderTopLeftRadius(500f, 12f));
        assertEquals(0f, style.getBorderTopRightRadius(500f, 12f));
        assertEquals(0f, style.getBorderBottomRightRadius(500f, 12f));
        assertEquals(0f, style.getBorderBottomLeftRadius(500f, 12f));
    }

    @Test
    void getBorderRadiusParsesPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("border-top-left-radius", "10px");
        // 10px * 0.75 = 7.5pt
        assertEquals(7.5f, style.getBorderTopLeftRadius(500f, 12f), 0.01f);
    }

    @Test
    void getBorderRadiusParsesPercentage() {
        ComputedStyle style = new ComputedStyle();
        style.set("border-top-left-radius", "50%");
        // 50% of 200pt container = 100pt
        assertEquals(100f, style.getBorderTopLeftRadius(200f, 12f), 0.01f);
    }

    // --- Line-height ---

    @Test
    void getLineHeightNormalReturnsNegative() {
        ComputedStyle style = new ComputedStyle();
        // No line-height set → -1
        assertEquals(-1f, style.getLineHeight(12f));

        style.set("line-height", "normal");
        assertEquals(-1f, style.getLineHeight(12f));
    }

    @Test
    void getLineHeightInheritReturnsNegative() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "inherit");
        assertEquals(-1f, style.getLineHeight(12f));
    }

    @Test
    void getLineHeightUnitless() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "1.8");
        // 1.8 * 10pt = 18pt
        assertEquals(18f, style.getLineHeight(10f), 0.01f);
    }

    @Test
    void getLineHeightPixels() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "24px");
        // 24px * 0.75 = 18pt
        assertEquals(18f, style.getLineHeight(12f), 0.01f);
    }

    @Test
    void getLineHeightPercentage() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "150%");
        // 150% of 12pt = 18pt
        assertEquals(18f, style.getLineHeight(12f), 0.01f);
    }

    @Test
    void getLineHeightEm() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "1.5em");
        // 1.5 * 12pt = 18pt
        assertEquals(18f, style.getLineHeight(12f), 0.01f);
    }

    @Test
    void getLineHeightPt() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "18pt");
        assertEquals(18f, style.getLineHeight(12f), 0.01f);
    }

    @Test
    void getLineHeightInvalidReturnsNegative() {
        ComputedStyle style = new ComputedStyle();
        style.set("line-height", "abc");
        assertEquals(-1f, style.getLineHeight(12f));
    }

    // --- Box-shadow ---

    @Test
    void getBoxShadowDefaultsToNull() {
        ComputedStyle style = new ComputedStyle();
        assertNull(style.getBoxShadow(500f, 12f));
    }

    @Test
    void getBoxShadowNoneReturnsNull() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "none");
        assertNull(style.getBoxShadow(500f, 12f));
    }

    @Test
    void getBoxShadowParsesBasic() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "2px 3px");
        ComputedStyle.BoxShadow shadow = style.getBoxShadow(500f, 12f);
        assertNotNull(shadow);
        assertEquals(1.5f, shadow.offsetX(), 0.01f);   // 2px * 0.75
        assertEquals(2.25f, shadow.offsetY(), 0.01f);  // 3px * 0.75
        assertEquals(0f, shadow.blur(), 0.01f);
        assertEquals(0f, shadow.spread(), 0.01f);
    }

    @Test
    void getBoxShadowParsesFullValue() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "2px 3px 5px 1px rgba(0,0,0,0.3)");
        ComputedStyle.BoxShadow shadow = style.getBoxShadow(500f, 12f);
        assertNotNull(shadow);
        assertEquals(1.5f, shadow.offsetX(), 0.01f);   // 2px * 0.75
        assertEquals(2.25f, shadow.offsetY(), 0.01f);  // 3px * 0.75
        assertEquals(3.75f, shadow.blur(), 0.01f);     // 5px * 0.75
        assertEquals(0.75f, shadow.spread(), 0.01f);   // 1px * 0.75
        assertTrue(shadow.color().contains("rgba"), "Color should be rgba, got: " + shadow.color());
    }

    @Test
    void getBoxShadowsMultipleValues() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "0 0 0 2px #ffffff, 0 0 0 4px #d69e2e");
        var shadows = style.getBoxShadows(500f, 12f);
        assertEquals(2, shadows.size());

        // First shadow: 0 0 0 2px #ffffff
        assertEquals(0f, shadows.get(0).offsetX(), 0.01f);
        assertEquals(0f, shadows.get(0).offsetY(), 0.01f);
        assertEquals(0f, shadows.get(0).blur(), 0.01f);
        assertEquals(1.5f, shadows.get(0).spread(), 0.01f);   // 2px * 0.75
        assertEquals("#ffffff", shadows.get(0).color());

        // Second shadow: 0 0 0 4px #d69e2e
        assertEquals(0f, shadows.get(1).offsetX(), 0.01f);
        assertEquals(0f, shadows.get(1).offsetY(), 0.01f);
        assertEquals(0f, shadows.get(1).blur(), 0.01f);
        assertEquals(3f, shadows.get(1).spread(), 0.01f);     // 4px * 0.75
        assertEquals("#d69e2e", shadows.get(1).color());
    }

    @Test
    void getBoxShadowsWithRgba() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "2px 3px 5px rgba(0,0,0,0.3), 0 0 10px rgba(255,0,0,0.5)");
        var shadows = style.getBoxShadows(500f, 12f);
        assertEquals(2, shadows.size());
        assertTrue(shadows.get(0).color().contains("rgba"));
        assertTrue(shadows.get(1).color().contains("rgba"));
    }

    @Test
    void getBoxShadowsSingleValue() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "2px 3px 5px rgba(0,0,0,0.3)");
        var shadows = style.getBoxShadows(500f, 12f);
        assertEquals(1, shadows.size());
    }

    @Test
    void getBoxShadowsNoneReturnsEmptyList() {
        ComputedStyle style = new ComputedStyle();
        style.set("box-shadow", "none");
        var shadows = style.getBoxShadows(500f, 12f);
        assertTrue(shadows.isEmpty());
    }
}
