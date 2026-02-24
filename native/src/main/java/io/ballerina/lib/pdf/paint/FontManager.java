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
                PDType0Font font = PDType0Font.load(document, is, true);
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
                PDType0Font symbolFont = PDType0Font.load(document, is, true);
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

            PDType0Font font = PDType0Font.load(document, new ByteArrayInputStream(fontBytes), true);

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

        // Register custom font regular variants as glyph-level fallback candidates.
        // Inserted before existing fallback fonts (e.g., Noto Symbols) so user-provided
        // fonts are tried first for mixed-script content.
        // Note: each loadCustomFonts() call only adds fonts from that call's map.
        int insertPos = 0;
        for (String family : customFamilies) {
            PDFont regular = fontMap.get(family + "|false|false");
            if (regular != null) {
                fallbackFonts.add(insertPos++, regular);
            }
        }
    }

    /**
     * Resolves a CSS font-family, weight, and style to a loaded PDFont.
     * Delegates to the array-based overload for a single-family input.
     */
    public PDFont getFont(String family, boolean bold, boolean italic) {
        return getFont(new String[]{family}, bold, italic);
    }

    /**
     * Resolves a CSS font-family fallback chain to a loaded PDFont.
     * Walks the chain in order, returning the first family that maps to a loaded font.
     * Falls back to the default font only after exhausting all families.
     */
    public PDFont getFont(String[] families, boolean bold, boolean italic) {
        for (String family : families) {
            String resolved = tryResolveFamily(family);
            if (resolved == null) {
                continue;
            }
            PDFont font = lookupFont(resolved, bold, italic);
            if (font != null) {
                return font;
            }
        }
        return defaultFont;
    }

    /**
     * Attempts to resolve a single CSS family name to a loaded font family key.
     * Returns null if the family is not recognized — the caller should try the next
     * family in the chain rather than falling back to a default.
     */
    private String tryResolveFamily(String family) {
        if (family == null) return null;
        String lower = family.toLowerCase().trim();
        lower = lower.replace("'", "").replace("\"", "");

        // Custom fonts take priority
        if (customFamilies.contains(lower)) {
            return lower;
        }

        // Map known families — sans-serif check must precede serif check
        // because "sans-serif".contains("serif") is true
        if (lower.contains("liberation sans") || lower.contains("arial")
                || lower.contains("helvetica") || lower.equals("sans-serif")) {
            return "liberation sans";
        }
        if (lower.contains("liberation serif") || lower.contains("times")
                || lower.equals("serif")) {
            return "liberation serif";
        }
        if (lower.equals("monospace") || lower.equals("courier")) {
            return "liberation sans"; // no monospace font bundled; best available fallback
        }

        // Unrecognized family — return null so the chain walker tries the next entry
        return null;
    }

    /**
     * Looks up a font by resolved family key with bold/italic fallback cascade.
     * Returns null if no variant of the family is loaded.
     */
    private PDFont lookupFont(String resolvedFamily, boolean bold, boolean italic) {
        String key = resolvedFamily + "|" + bold + "|" + italic;
        PDFont font = fontMap.get(key);
        if (font != null) return font;

        // Try without italic
        if (italic) {
            font = fontMap.get(resolvedFamily + "|" + bold + "|false");
            if (font != null) return font;
        }
        // Try without bold
        if (bold) {
            font = fontMap.get(resolvedFamily + "|false|" + italic);
            if (font != null) return font;
        }
        // Try regular variant of the family
        font = fontMap.get(resolvedFamily + "|false|false");
        return font;
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

}
