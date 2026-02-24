package io.ballerina.lib.pdf.paint;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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

    // --- Font-family chain resolution tests ---

    @Test
    void fontChainResolvesFirstKnownFamily() {
        PDFont chained = fontManager.getFont(new String[]{"NonExistent", "Arial"}, false, false);
        PDFont direct = fontManager.getFont("Arial", false, false);
        assertSame(direct, chained);
    }

    @Test
    void fontChainFirstFamilyWins() {
        PDFont chained = fontManager.getFont(new String[]{"Arial", "Times New Roman"}, false, false);
        PDFont arial = fontManager.getFont("Arial", false, false);
        PDFont times = fontManager.getFont("Times New Roman", false, false);
        assertSame(arial, chained);
        assertNotSame(times, chained);
    }

    @Test
    void fontChainEmptyArrayReturnsDefault() {
        PDFont result = fontManager.getFont(new String[]{}, false, false);
        assertSame(fontManager.getDefaultFont(), result);
    }

    @Test
    void fontChainAllUnknownReturnsDefault() {
        PDFont result = fontManager.getFont(new String[]{"Foo", "Bar", "Baz"}, false, false);
        assertSame(fontManager.getDefaultFont(), result);
    }

    @Test
    void fontChainSingleElementMatchesSingle() {
        PDFont chained = fontManager.getFont(new String[]{"Arial"}, false, false);
        PDFont direct = fontManager.getFont("Arial", false, false);
        assertSame(direct, chained);
    }

    // --- tryResolveFamily behavior (tested via getFont) ---

    @Test
    void resolvesGenericSansSerif() {
        PDFont sansSerif = fontManager.getFont("sans-serif", false, false);
        PDFont arial = fontManager.getFont("Arial", false, false);
        assertSame(arial, sansSerif);
    }

    @Test
    void resolvesGenericSerif() {
        PDFont serif = fontManager.getFont("serif", false, false);
        PDFont times = fontManager.getFont("Times New Roman", false, false);
        assertSame(times, serif);
    }

    @Test
    void serifNotConfusedWithSansSerif() {
        PDFont serif = fontManager.getFont("serif", false, false);
        PDFont sansSerif = fontManager.getFont("sans-serif", false, false);
        assertNotSame(serif, sansSerif);
    }

    @Test
    void resolvesHelvetica() {
        PDFont helvetica = fontManager.getFont("Helvetica", false, false);
        PDFont arial = fontManager.getFont("Arial", false, false);
        assertSame(arial, helvetica);
    }

    // --- Custom font tests ---

    @Nested
    class CustomFontTests {
        private PDDocument customDoc;
        private FontManager customFm;

        @BeforeEach
        void setUp() throws IOException {
            customDoc = new PDDocument();
            customFm = new FontManager();
            customFm.loadFonts(customDoc);
            byte[] fontBytes;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("fonts/LiberationSerif-Regular.ttf")) {
                assertNotNull(is, "LiberationSerif-Regular.ttf must be on classpath");
                fontBytes = is.readAllBytes();
            }
            customFm.loadCustomFonts(customDoc, Map.of("MyCustom", fontBytes));
        }

        @AfterEach
        void tearDown() throws IOException {
            customDoc.close();
        }

        @Test
        void customFontResolvesViaGetFont() {
            PDFont custom = customFm.getFont("MyCustom", false, false);
            PDFont defaultFont = customFm.getDefaultFont();
            assertNotNull(custom);
            assertNotSame(defaultFont, custom);
        }

        @Test
        void customFontTakesPriorityOverBuiltin() {
            PDFont chained = customFm.getFont(new String[]{"MyCustom", "Arial"}, false, false);
            PDFont arial = customFm.getFont("Arial", false, false);
            assertNotSame(arial, chained);
        }

        @Test
        void customFontAddedToFallbackList() {
            // Liberation Serif can encode 'A', so findFallbackFont should return it
            PDFont fallback = customFm.findFallbackFont('A');
            assertNotNull(fallback, "Fallback font should be found for 'A'");
        }

        @Test
        void customFontFallbackInsertedBeforeSymbol() {
            // Custom fonts are inserted at index 0 in the fallback list,
            // so for a Latin char like 'A' the custom font (Liberation Serif) should be returned
            // before Noto Symbols (which also can't encode standard Latin in many builds).
            PDFont fallback = customFm.findFallbackFont('A');
            assertNotNull(fallback);
            // The custom font (Liberation Serif) can encode 'A' and should be first in fallback
            assertTrue(FontManager.canEncode(fallback, 'A'));
        }
    }
}
