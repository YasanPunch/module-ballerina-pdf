package io.ballerina.lib.pdf.paint;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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

    // Key format: "family|bold|italic" e.g. "liberation sans|true|false"
    private final Map<String, PDFont> fontMap = new HashMap<>();
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
        } catch (IOException e) {
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
