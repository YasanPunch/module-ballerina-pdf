package io.ballerina.lib.pdf;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Preprocesses raw (potentially messy) HTML into a clean W3C DOM Document
 * suitable for the rendering pipeline.
 */
public class HtmlPreprocessor {

    private static final Pattern XML_DECLARATION = Pattern.compile("<\\?xml[^?]*\\?>");
    private static final Pattern DUPLICATE_FONT_FAMILY =
            Pattern.compile("font-family\\s*:\\s*font-family\\s*:", Pattern.CASE_INSENSITIVE);

    // Universal CSS resets — sensible defaults for any HTML-to-PDF conversion
    private static final String UNIVERSAL_CSS = String.join("\n",
            "body { margin: 0; padding: 0; }",
            "table { width: 100% !important; max-width: 100%; table-layout: fixed; }",
            "img { max-width: 100%; height: auto; }"
    );

    /**
     * Preprocesses raw HTML with options and returns a W3C DOM Document for the rendering pipeline.
     */
    public org.w3c.dom.Document preprocess(String rawHtml, ConverterOptions options) {
        Document jsoupDoc = cleanupHtml(rawHtml, options);
        W3CDom w3cDom = new W3CDom();
        return w3cDom.fromJsoup(jsoupDoc);
    }

    /** Backwards-compatible overload using default options. */
    public org.w3c.dom.Document preprocess(String rawHtml) {
        return preprocess(rawHtml, new ConverterOptions());
    }

    /**
     * Parses raw HTML to W3C DOM without preprocessing (Jsoup parse only).
     * Used when preprocessing is disabled but we still need a DOM.
     */
    public org.w3c.dom.Document parseOnly(String rawHtml) {
        Document doc = Jsoup.parse(rawHtml);
        doc.outputSettings()
                .syntax(OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");
        W3CDom w3cDom = new W3CDom();
        return w3cDom.fromJsoup(doc);
    }

    /**
     * Preprocesses and returns the cleaned XHTML as a string.
     * Useful for debugging — write the output to disk for inspection.
     */
    public String preprocessToString(String rawHtml) {
        Document jsoupDoc = cleanupHtml(rawHtml, new ConverterOptions());
        return jsoupDoc.html();
    }

    private Document cleanupHtml(String rawHtml, ConverterOptions options) {
        // Step 1: Strip XML declaration (e.g. <?xml version="1.0" encoding="utf-16"?>)
        // Some XHTML documents declare utf-16 encoding but are actually UTF-8.
        String html = XML_DECLARATION.matcher(rawHtml).replaceFirst("");

        // Step 2: Parse with Jsoup (lenient — handles self-closing divs, unclosed tags)
        Document doc = Jsoup.parse(html);

        // Step 3: Configure XHTML output
        doc.outputSettings()
                .syntax(OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");

        // Step 4: Fix CSS issues in <style> blocks
        for (Element styleEl : doc.select("style")) {
            String css = styleEl.html();

            // Fix duplicate font-family property (e.g. "font-family:font-family:Arial")
            css = DUPLICATE_FONT_FAMILY.matcher(css).replaceAll("font-family:");

            // Map Arial/Helvetica to Liberation Sans (our bundled font)
            css = css.replaceAll(
                    "font-family\\s*:\\s*Arial",
                    "font-family: 'Liberation Sans', Arial"
            );

            styleEl.html(css);
        }

        // Step 5: Inject generated CSS (page sizing + universal resets + additional CSS)
        Element head = doc.head();
        if (head != null) {
            StringBuilder css = new StringBuilder();

            // Generate @page rule from options
            String pageSizeCss = String.format("%.1fpt %.1fpt",
                    options.getPageWidth(), options.getPageHeight());
            String marginCss = String.format("%.1fpt %.1fpt %.1fpt %.1fpt",
                    options.getMarginTop(), options.getMarginRight(),
                    options.getMarginBottom(), options.getMarginLeft());
            css.append(String.format("@page { size: %s; margin: %s; }\n", pageSizeCss, marginCss));

            // Universal resets
            css.append(UNIVERSAL_CSS).append("\n");

            // Additional CSS from options (consumer-specific overrides)
            String additionalCss = options.getAdditionalCss();
            if (additionalCss != null && !additionalCss.isBlank()) {
                css.append(additionalCss).append("\n");
            }

            Element pageStyle = doc.createElement("style");
            pageStyle.html(css.toString());
            head.prependChild(pageStyle);
        }

        // Step 6: Normalize table column widths using <colgroup> injection.
        // With table-layout:fixed, the renderer checks <col> widths before first-row widths.
        // This solves the mixed-column-count problem where section headers (3 cols) and
        // data rows (4-6 cols) coexist in the same table.
        normalizeTableWidths(doc);

        return doc;
    }

    /**
     * Normalizes column widths across all tables using {@code <colgroup>} injection.
     * <p>
     * For each table: finds the "reference row" (the row with the most actual cells
     * where all cells have width attributes), computes per-column percentage widths,
     * injects a {@code <colgroup>} defining those widths, and normalizes all other rows
     * so their colspan totals match the reference column count.
     * <p>
     * This enables {@code table-layout: fixed} to honor column widths from the colgroup
     * rather than the first row, normalizing tables with inconsistent column counts across rows.
     */
    private void normalizeTableWidths(Document doc) {
        for (Element table : doc.select("table")) {
            // Remove table-level width attribute
            table.removeAttr("width");

            // Get direct rows only (skip nested tables)
            List<Element> rows = getDirectRows(table);
            if (rows.isEmpty()) continue;

            // Find reference row: the row with the most actual cells where all have width attrs.
            // On ties, prefer the row with the most evenly distributed widths (lowest max/min
            // ratio). An even colgroup distributes space fairly; a skewed one starves columns.
            Element referenceRow = null;
            int maxActualCells = 0;
            double bestUniformity = Double.MAX_VALUE;
            for (Element tr : rows) {
                List<Element> cells = getCells(tr);
                if (cells.isEmpty()) continue;
                boolean allHaveWidth = cells.stream().allMatch(c -> c.hasAttr("width"));
                if (!allHaveWidth) continue;
                if (cells.size() > maxActualCells) {
                    maxActualCells = cells.size();
                    referenceRow = tr;
                    bestUniformity = widthVariation(cells);
                } else if (cells.size() == maxActualCells) {
                    double variation = widthVariation(cells);
                    if (variation < bestUniformity) {
                        bestUniformity = variation;
                        referenceRow = tr;
                    }
                }
            }

            if (referenceRow == null) continue; // No row with width attributes → skip table

            // Determine canonical column count from reference row
            List<Element> refCells = getCells(referenceRow);
            int maxCols = 0;
            for (Element cell : refCells) {
                maxCols += getColspan(cell);
            }
            if (maxCols == 0) continue;

            // Compute per-column percentage widths from reference row
            double totalPx = 0;
            for (Element cell : refCells) {
                totalPx += parsePixelWidth(cell.attr("width"));
            }
            if (totalPx <= 0) continue;

            double[] colWidths = new double[maxCols];
            int colIdx = 0;
            for (Element cell : refCells) {
                double px = parsePixelWidth(cell.attr("width"));
                double pct = (px / totalPx) * 100.0;
                int colspan = getColspan(cell);
                double perCol = pct / colspan;
                for (int i = 0; i < colspan && colIdx < maxCols; i++) {
                    colWidths[colIdx++] = perCol;
                }
            }

            // Inject <colgroup> with computed column widths
            Element colgroup = doc.createElement("colgroup");
            for (int i = 0; i < maxCols; i++) {
                Element col = doc.createElement("col");
                col.attr("style", String.format("width: %.1f%%", colWidths[i]));
                colgroup.appendChild(col);
            }
            table.prependChild(colgroup);

            // Normalize rows: adjust colspans so every row spans exactly maxCols
            for (Element tr : rows) {
                List<Element> cells = getCells(tr);
                if (cells.isEmpty()) continue;

                int logicalCols = 0;
                for (Element cell : cells) {
                    logicalCols += getColspan(cell);
                }

                if (logicalCols < maxCols && cells.stream().allMatch(c -> c.hasAttr("width"))) {
                    // Short row with widths: use width-based greedy colspan assignment.
                    // For each cell, assign columns left-to-right until cumulative
                    // column width best matches the cell's declared width.
                    int col = 0;
                    for (int ci = 0; ci < cells.size(); ci++) {
                        Element cell = cells.get(ci);
                        double cellPx = parsePixelWidth(cell.attr("width"));
                        boolean isLast = (ci == cells.size() - 1);

                        if (isLast) {
                            // Last cell gets all remaining columns
                            cell.attr("colspan", String.valueOf(maxCols - col));
                        } else {
                            // Greedily assign columns while cumulative width < cell width
                            double cumWidth = 0;
                            int span = 0;
                            while (col + span < maxCols) {
                                double nextWidth = cumWidth + (col + span < colWidths.length ? colWidths[col + span] / 100.0 * totalPx : 0);
                                if (span > 0 && Math.abs(nextWidth - cellPx) > Math.abs(cumWidth - cellPx)) {
                                    break; // Adding another column makes it worse
                                }
                                cumWidth = nextWidth;
                                span++;
                            }
                            span = Math.max(1, span);
                            cell.attr("colspan", String.valueOf(span));
                            col += span;
                        }
                    }
                } else if (logicalCols < maxCols) {
                    // Short row without widths: fill last cell
                    Element lastCell = cells.get(cells.size() - 1);
                    int currentColspan = getColspan(lastCell);
                    int deficit = maxCols - logicalCols;
                    lastCell.attr("colspan", String.valueOf(currentColspan + deficit));
                } else if (logicalCols > maxCols) {
                    // Oversized row (e.g., spacer with excessive colspan): reduce colspans
                    int excess = logicalCols - maxCols;
                    for (int i = cells.size() - 1; i >= 0 && excess > 0; i--) {
                        Element cell = cells.get(i);
                        int cs = getColspan(cell);
                        if (cs > 1) {
                            int reduction = Math.min(cs - 1, excess);
                            cell.attr("colspan", String.valueOf(cs - reduction));
                            excess -= reduction;
                        }
                    }
                }
            }

            // Remove width attributes from all cells — colgroup handles widths now
            for (Element tr : rows) {
                for (Element cell : getCells(tr)) {
                    cell.removeAttr("width");
                }
            }
        }
    }

    /** Returns direct {@code <tr>} children of a table, including those inside tbody/thead/tfoot. */
    private List<Element> getDirectRows(Element table) {
        List<Element> rows = new ArrayList<>();
        for (Element child : table.children()) {
            if (child.tagName().equals("tr")) {
                rows.add(child);
            } else if (child.tagName().equals("tbody") || child.tagName().equals("thead")
                    || child.tagName().equals("tfoot")) {
                for (Element grandchild : child.children()) {
                    if (grandchild.tagName().equals("tr")) {
                        rows.add(grandchild);
                    }
                }
            }
        }
        return rows;
    }

    /** Returns direct td/th children of a row. */
    private List<Element> getCells(Element tr) {
        return tr.children().stream()
                .filter(el -> el.tagName().equals("td") || el.tagName().equals("th"))
                .toList();
    }

    /** Parses the colspan attribute, defaulting to 1. */
    private int getColspan(Element cell) {
        String cs = cell.attr("colspan");
        if (cs.isEmpty()) return 1;
        try {
            return Math.max(1, Integer.parseInt(cs.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** Returns max/min ratio of cell widths. Lower = more uniform distribution. */
    private double widthVariation(List<Element> cells) {
        double min = Double.MAX_VALUE;
        double max = 0;
        for (Element cell : cells) {
            double w = parsePixelWidth(cell.attr("width"));
            if (w > 0) {
                min = Math.min(min, w);
                max = Math.max(max, w);
            }
        }
        if (min <= 0) return Double.MAX_VALUE;
        return max / min;
    }

    private double parsePixelWidth(String widthAttr) {
        String cleaned = widthAttr.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
