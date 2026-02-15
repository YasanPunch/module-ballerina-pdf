package io.ballerina.lib.pdf.paint;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FontManagerTest {

    private static PDDocument document;
    private static FontManager fontManager;

    @BeforeAll
    static void setUp() throws IOException {
        document = new PDDocument();
        fontManager = new FontManager();
        fontManager.loadFonts(document);
    }

    @AfterAll
    static void tearDown() throws IOException {
        document.close();
    }

    @Test
    void loadsFontsWithoutError() {
        assertNotNull(fontManager.getDefaultFont());
    }

    @Test
    void defaultFontIsNonNull() {
        PDFont font = fontManager.getDefaultFont();
        assertNotNull(font);
    }

    @Test
    void resolvesSansSerifFamily() {
        PDFont font = fontManager.getFont("Arial", false, false);
        assertNotNull(font);
    }

    @Test
    void resolvesSerifFamily() {
        PDFont font = fontManager.getFont("Times New Roman", false, false);
        assertNotNull(font);
    }

    @Test
    void resolvesBoldVariant() {
        PDFont regular = fontManager.getFont("Arial", false, false);
        PDFont bold = fontManager.getFont("Arial", true, false);
        assertNotNull(bold);
        assertNotSame(regular, bold);
    }

    @Test
    void resolvesItalicVariant() {
        PDFont regular = fontManager.getFont("Arial", false, false);
        PDFont italic = fontManager.getFont("Arial", false, true);
        assertNotNull(italic);
        assertNotSame(regular, italic);
    }

    @Test
    void fallsBackOnMissingVariant() {
        PDFont font = fontManager.getFont("NonExistentFont", true, true);
        assertNotNull(font);
    }

    @Test
    void measuresTextWidth() {
        PDFont font = fontManager.getDefaultFont();
        float width = fontManager.measureText("Hello", font, 12f);
        assertTrue(width > 0);
    }

    @Test
    void emptyTextMeasuresZero() {
        PDFont font = fontManager.getDefaultFont();
        assertEquals(0, fontManager.measureText("", font, 12f));
    }

    @Test
    void lineHeightIsPositive() {
        PDFont font = fontManager.getDefaultFont();
        float lineHeight = fontManager.getLineHeight(font, 12f);
        assertTrue(lineHeight > 0);
    }
}
