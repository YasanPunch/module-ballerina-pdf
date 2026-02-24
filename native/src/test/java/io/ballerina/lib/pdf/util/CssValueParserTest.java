package io.ballerina.lib.pdf.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CssValueParserTest {

    @Test
    void convertsPixelsToPoints() {
        assertEquals(9.0f, CssValueParser.toPoints("12px"), 0.01f);
    }

    @Test
    void convertsPointsIdentity() {
        assertEquals(10.0f, CssValueParser.toPoints("10pt"), 0.01f);
    }

    @Test
    void convertsMillimeters() {
        assertEquals(28.35f, CssValueParser.toPoints("10mm"), 0.1f);
    }

    @Test
    void convertsCentimeters() {
        assertEquals(28.35f, CssValueParser.toPoints("1cm"), 0.1f);
    }

    @Test
    void convertsInches() {
        assertEquals(72.0f, CssValueParser.toPoints("1in"), 0.01f);
    }

    @Test
    void convertsEmRelativeToFontSize() {
        assertEquals(24.0f, CssValueParser.toPoints("2em", 0, 12f), 0.01f);
    }

    @Test
    void convertsRemRelativeToFontSize() {
        assertEquals(18.0f, CssValueParser.toPoints("1.5rem", 0, 12f), 0.01f);
    }

    @Test
    void convertsPercentage() {
        assertEquals(100.0f, CssValueParser.toPoints("50%", 200f), 0.01f);
    }

    @Test
    void returnsZeroForAutoNoneInherit() {
        assertEquals(0, CssValueParser.toPoints("auto"));
        assertEquals(0, CssValueParser.toPoints("none"));
        assertEquals(0, CssValueParser.toPoints("inherit"));
    }

    @Test
    void returnsZeroForNullOrBlank() {
        assertEquals(0, CssValueParser.toPoints(null));
        assertEquals(0, CssValueParser.toPoints(""));
        assertEquals(0, CssValueParser.toPoints("   "));
    }

    @Test
    void handlesNoUnit() {
        // No unit is treated as px: 16 * 0.75 = 12.0
        assertEquals(12.0f, CssValueParser.toPoints("16"), 0.01f);
    }

    @Test
    void parseFontSizeKeywords() {
        // medium = 16px * 0.75 = 12pt
        assertEquals(12.0f, CssValueParser.parseFontSize("medium", 12f), 0.01f);
        // small = 13px * 0.75 = 9.75pt
        assertEquals(9.75f, CssValueParser.parseFontSize("small", 12f), 0.01f);
        // x-large = 24px * 0.75 = 18pt
        assertEquals(18.0f, CssValueParser.parseFontSize("x-large", 12f), 0.01f);
    }

    @Test
    void parseFontSizeRelative() {
        float parent = 12f;
        assertEquals(parent * 0.85f, CssValueParser.parseFontSize("smaller", parent), 0.01f);
        assertEquals(parent * 1.2f, CssValueParser.parseFontSize("larger", parent), 0.01f);
    }

    @Test
    void isBoldRecognizesWeights() {
        assertTrue(CssValueParser.isBold("bold"));
        assertTrue(CssValueParser.isBold("700"));
        assertTrue(CssValueParser.isBold("900"));
        assertFalse(CssValueParser.isBold("normal"));
        assertFalse(CssValueParser.isBold("400"));
        assertFalse(CssValueParser.isBold(null));
    }

    @Test
    void isItalicRecognizesStyles() {
        assertTrue(CssValueParser.isItalic("italic"));
        assertTrue(CssValueParser.isItalic("oblique"));
        assertFalse(CssValueParser.isItalic("normal"));
        assertFalse(CssValueParser.isItalic(null));
    }

    @Test
    void parsePrimaryFontFamily() {
        assertEquals("Liberation Sans", CssValueParser.parsePrimaryFontFamily("'Liberation Sans', Arial"));
    }

    @Test
    void parsePrimaryFontFamilyUnquoted() {
        assertEquals("Arial", CssValueParser.parsePrimaryFontFamily("Arial, sans-serif"));
    }

    // --- parseFontFamilyList tests ---

    @Test
    void parseFontFamilyListMultiple() {
        assertArrayEquals(
                new String[]{"Arial", "Helvetica", "sans-serif"},
                CssValueParser.parseFontFamilyList("Arial, Helvetica, sans-serif"));
    }

    @Test
    void parseFontFamilyListQuoted() {
        assertArrayEquals(
                new String[]{"Liberation Sans", "Arial", "sans-serif"},
                CssValueParser.parseFontFamilyList("'Liberation Sans', Arial, sans-serif"));
    }

    @Test
    void parseFontFamilyListDoubleQuoted() {
        assertArrayEquals(
                new String[]{"Times New Roman", "Georgia", "serif"},
                CssValueParser.parseFontFamilyList("\"Times New Roman\", Georgia, serif"));
    }

    @Test
    void parseFontFamilyListSingle() {
        assertArrayEquals(
                new String[]{"Arial"},
                CssValueParser.parseFontFamilyList("Arial"));
    }

    @Test
    void parseFontFamilyListWhitespace() {
        assertArrayEquals(
                new String[]{"Arial", "Helvetica"},
                CssValueParser.parseFontFamilyList(" Arial , Helvetica "));
    }

    @Test
    void parseFontFamilyListNull() {
        assertArrayEquals(new String[0], CssValueParser.parseFontFamilyList(null));
    }

    @Test
    void parseFontFamilyListBlank() {
        assertArrayEquals(new String[0], CssValueParser.parseFontFamilyList("  "));
    }

    @Test
    void parseFontFamilyListMixedQuotes() {
        assertArrayEquals(
                new String[]{"Arial", "Times New Roman", "sans-serif"},
                CssValueParser.parseFontFamilyList("'Arial', \"Times New Roman\", sans-serif"));
    }
}
