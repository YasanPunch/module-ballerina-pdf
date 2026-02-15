package io.ballerina.lib.pdf.paint;

import java.awt.Color;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS color values: #hex, rgb(), named colors → java.awt.Color.
 */
public class ColorParser {

    private static final Pattern HEX3 = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern HEX6 = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
    private static final Pattern RGB = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
    private static final Pattern RGBA = Pattern.compile("rgba\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([\\d.]+)\\s*\\)");

    private static final Map<String, Color> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", Color.BLACK),
            Map.entry("white", Color.WHITE),
            Map.entry("red", new Color(255, 0, 0)),
            Map.entry("green", new Color(0, 128, 0)),
            Map.entry("blue", new Color(0, 0, 255)),
            Map.entry("yellow", new Color(255, 255, 0)),
            Map.entry("orange", new Color(255, 165, 0)),
            Map.entry("purple", new Color(128, 0, 128)),
            Map.entry("grey", new Color(128, 128, 128)),
            Map.entry("gray", new Color(128, 128, 128)),
            Map.entry("silver", new Color(192, 192, 192)),
            Map.entry("navy", new Color(0, 0, 128)),
            Map.entry("teal", new Color(0, 128, 128)),
            Map.entry("maroon", new Color(128, 0, 0)),
            Map.entry("olive", new Color(128, 128, 0)),
            Map.entry("aqua", new Color(0, 255, 255)),
            Map.entry("fuchsia", new Color(255, 0, 255)),
            Map.entry("lime", new Color(0, 255, 0)),
            Map.entry("transparent", new Color(0, 0, 0, 0))
    );

    /**
     * Parses a CSS color string. Returns null if unparseable.
     */
    public static Color parse(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim().toLowerCase();

        // Named color
        Color named = NAMED_COLORS.get(value);
        if (named != null) return named;

        // #RRGGBB
        Matcher m6 = HEX6.matcher(value);
        if (m6.matches()) {
            return new Color(
                    Integer.parseInt(m6.group(1), 16),
                    Integer.parseInt(m6.group(2), 16),
                    Integer.parseInt(m6.group(3), 16)
            );
        }

        // #RGB
        Matcher m3 = HEX3.matcher(value);
        if (m3.matches()) {
            return new Color(
                    Integer.parseInt(m3.group(1) + m3.group(1), 16),
                    Integer.parseInt(m3.group(2) + m3.group(2), 16),
                    Integer.parseInt(m3.group(3) + m3.group(3), 16)
            );
        }

        // rgb(r, g, b)
        Matcher mRgb = RGB.matcher(value);
        if (mRgb.matches()) {
            return new Color(
                    clamp(Integer.parseInt(mRgb.group(1))),
                    clamp(Integer.parseInt(mRgb.group(2))),
                    clamp(Integer.parseInt(mRgb.group(3)))
            );
        }

        // rgba(r, g, b, a)
        Matcher mRgba = RGBA.matcher(value);
        if (mRgba.matches()) {
            float alpha = Float.parseFloat(mRgba.group(4));
            return new Color(
                    clamp(Integer.parseInt(mRgba.group(1))),
                    clamp(Integer.parseInt(mRgba.group(2))),
                    clamp(Integer.parseInt(mRgba.group(3))),
                    clamp((int) (alpha * 255))
            );
        }

        return null;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
