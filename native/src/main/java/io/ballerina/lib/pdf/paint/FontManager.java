package io.ballerina.lib.pdf.paint;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and manages Liberation TTF fonts for PDFBox rendering.
 * Maps CSS font-family + weight + style to the correct PDType0Font.
 */
public class FontManager {

    private static final String[] FONT_FILES = {
            "fonts/LiberationSans-Regular.ttf",
            "fonts/LiberationSans-Bold.ttf",
            "fonts/LiberationSans-Italic.ttf",
            "fonts/LiberationSans-BoldItalic.ttf",
            "fonts/LiberationSerif-Regular.ttf",
            "fonts/LiberationSerif-Bold.ttf",
            "fonts/LiberationSerif-Italic.ttf",
            "fonts/LiberationSerif-BoldItalic.ttf",
    };

    private static final String SYMBOL_FONT_FILE = "fonts/NotoSansSymbols2-Regular.ttf";

    // Key format: "family|bold|italic" e.g. "liberation sans|true|false"
    private final Map<String, PDFont> fontMap = new HashMap<>();
    private final Set<String> customFamilies = new HashSet<>();
    // Fallback fonts tried when primary font can't encode a character
    private final List<PDFont> fallbackFonts = new ArrayList<>();
    private PDFont defaultFont;

    public void loadFonts(PDDocument document) throws IOException {
        for (String fontFile : FONT_FILES) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fontFile)) {
                if (is == null) {
                    System.err.println("WARNING: Font not found on classpath: " + fontFile);
                    continue;
                }
                PDType0Font font = PDType0Font.load(document, is);
                String key = buildKey(fontFile);
                fontMap.put(key, font);
            }
        }
        // Default font is Liberation Sans Regular
        defaultFont = fontMap.get("liberation sans|false|false");
        if (defaultFont == null && !fontMap.isEmpty()) {
            defaultFont = fontMap.values().iterator().next();
        }

        // Load symbol font for glyph fallback (Dingbats, Math, Arrows, etc.)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SYMBOL_FONT_FILE)) {
            if (is != null) {
                PDType0Font symbolFont = PDType0Font.load(document, is);
                fallbackFonts.add(symbolFont);
            }
        }
    }

    /**
     * Loads custom TTF fonts supplied by the user.
     * <p>
     * Each map entry has a key like "MyFont", "MyFont Bold", "MyFont Italic", or "MyFont BoldItalic".
     * Bold/italic is detected from font metadata first, then from the key name as fallback.
     * The family name is derived by stripping weight/style suffixes from the key.
     */
    public void loadCustomFonts(PDDocument document, Map<String, byte[]> customFonts) throws IOException {
        for (Map.Entry<String, byte[]> entry : customFonts.entrySet()) {
            String name = entry.getKey();
            byte[] fontBytes = entry.getValue();

            PDType0Font font = PDType0Font.load(document, new ByteArrayInputStream(fontBytes));

            // Detect bold/italic from font metadata first, then fall back to key name
            var descriptor = font.getFontDescriptor();
            boolean bold;
            boolean italic;
            if (descriptor != null && (descriptor.getFontWeight() > 0 || descriptor.getItalicAngle() != 0)) {
                bold = descriptor.getFontWeight() >= 700;
                italic = descriptor.getItalicAngle() != 0;
            } else {
                String nameLower = name.toLowerCase();
                bold = nameLower.contains("bold");
                italic = nameLower.contains("italic");
            }

            // Derive family name by stripping suffixes
            String family = name
                    .replaceAll("(?i)\\s*BoldItalic$", "")
                    .replaceAll("(?i)\\s*Bold$", "")
                    .replaceAll("(?i)\\s*Italic$", "")
                    .toLowerCase()
                    .trim();

            String key = family + "|" + bold + "|" + italic;
            fontMap.put(key, font);
            customFamilies.add(family);
        }
    }

    /**
     * Resolves a CSS font-family, weight, and style to a loaded PDFont.
     */
    public PDFont getFont(String family, boolean bold, boolean italic) {
        String normalizedFamily = normalizeFamily(family);
        String key = normalizedFamily + "|" + bold + "|" + italic;
        PDFont font = fontMap.get(key);
        if (font != null) return font;

        // Try without italic
        if (italic) {
            font = fontMap.get(normalizedFamily + "|" + bold + "|false");
            if (font != null) return font;
        }
        // Try without bold
        if (bold) {
            font = fontMap.get(normalizedFamily + "|false|" + italic);
            if (font != null) return font;
        }
        // Try regular variant of the family
        font = fontMap.get(normalizedFamily + "|false|false");
        if (font != null) return font;

        return defaultFont;
    }

    public PDFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * Measures the width of a string in points at the given font size.
     */
    public float measureText(String text, PDFont font, float fontSize) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            return text.length() * fontSize * 0.5f; // rough fallback
        }
    }

    /**
     * Returns the line height (ascent + descent + leading) for a font at a given size.
     */
    public float getLineHeight(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            float ascent = desc.getAscent() / 1000f * fontSize;
            float descent = Math.abs(desc.getDescent() / 1000f * fontSize);
            return ascent + descent + fontSize * 0.1f; // small leading
        }
        return fontSize * 1.2f;
    }

    public float getAscent(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            return desc.getAscent() / 1000f * fontSize;
        }
        return fontSize * 0.8f;
    }

    public float getDescent(PDFont font, float fontSize) {
        var desc = font.getFontDescriptor();
        if (desc != null) {
            return Math.abs(desc.getDescent() / 1000f * fontSize);
        }
        return fontSize * 0.2f;
    }

    /**
     * Finds a fallback font that can encode the given character.
     * Returns null if no loaded font supports the character.
     */
    public PDFont findFallbackFont(char c) {
        for (PDFont font : fallbackFonts) {
            if (canEncode(font, c)) {
                return font;
            }
        }
        return null;
    }

    /**
     * Tests whether a font can encode a character without throwing.
     */
    public static boolean canEncode(PDFont font, char c) {
        try {
            font.encode(String.valueOf(c));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildKey(String fontFile) {
        String name = fontFile.toLowerCase();
        String family;
        if (name.contains("liberationsans")) {
            family = "liberation sans";
        } else if (name.contains("liberationserif")) {
            family = "liberation serif";
        } else {
            family = "unknown";
        }
        boolean bold = name.contains("bold");
        boolean italic = name.contains("italic");
        return family + "|" + bold + "|" + italic;
    }

    private String normalizeFamily(String family) {
        if (family == null) return "liberation sans";
        String lower = family.toLowerCase().trim();
        // Strip quotes
        lower = lower.replace("'", "").replace("\"", "");
        // Check custom fonts first — return directly if a custom family matches
        if (customFamilies.contains(lower)) {
            return lower;
        }
        // Map common families
        if (lower.contains("liberation sans") || lower.contains("arial")
                || lower.contains("helvetica") || lower.contains("sans-serif")) {
            return "liberation sans";
        }
        if (lower.contains("liberation serif") || lower.contains("times")
                || lower.contains("serif")) {
            return "liberation serif";
        }
        return "liberation sans"; // default fallback
    }
}
