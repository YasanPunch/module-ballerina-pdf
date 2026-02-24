package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.*;
import io.ballerina.lib.pdf.css.ComputedStyle;
import io.ballerina.lib.pdf.paint.FontManager;
import io.ballerina.lib.pdf.util.CssValueParser;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lays out tables using a unified content-aware auto layout algorithm.
 * Measures actual cell content to determine column widths, collapsing empty
 * spacer columns and redistributing their space to columns with content.
 * Colgroup percentages serve as proportional hints for non-empty columns.
 */
public class TableLayoutEngine {

    private final BlockFormattingContext bfc;
    private final FontManager fontManager;
    private final float fontSizePt;

    public TableLayoutEngine(BlockFormattingContext bfc, FontManager fontManager, float fontSizePt) {
        this.bfc = bfc;
        this.fontManager = fontManager;
        this.fontSizePt = fontSizePt;
    }

    /**
     * Lays out a table box within the given available width.
     * Returns the total height consumed.
     */
    public float layout(TableBox table, float availableWidth) {
        int numCols = determineColumnCount(table);
        if (numCols == 0) numCols = 1;

        float[] colWidths = computeColumnWidths(table, numCols, availableWidth);
        table.setWidth(availableWidth);

        // Lay out rows
        float cursorY = 0;
        List<Box> rows = collectRows(table);

        for (Box rowBox : rows) {
            if (rowBox instanceof TableRowBox row) { 
                // TableRowBox is a direct child of TableBox,
                // it is a plain row with no children, so we can resolve the box model with the table width
                bfc.resolveBoxModelWithWidth(row, availableWidth);
                float rowHeight = layoutRow(row, colWidths, availableWidth);
                row.setX(0);
                row.setY(cursorY);
                row.setWidth(availableWidth);
                row.setHeight(rowHeight);
                cursorY += rowHeight;
            } else if (rowBox instanceof TableRowGroupBox group) {
                // TableRowGroupBox is a child of TableBox, so we can resolve the box model with the table width
                // the difference is that TableRowGroupBox can have children that are not TableRowBoxes
                // like thead, tbody, tfoot elements,
                // so we need to resolve the box model with the table width for each child
                bfc.resolveBoxModelWithWidth(group, availableWidth);
                float groupY = 0;
                for (Box child : group.getChildren()) {
                    if (child instanceof TableRowBox row) {
                        bfc.resolveBoxModelWithWidth(row, availableWidth);
                        float rowHeight = layoutRow(row, colWidths, availableWidth);
                        row.setX(0);
                        row.setY(groupY);
                        row.setWidth(availableWidth);
                        row.setHeight(rowHeight);
                        groupY += rowHeight;
                    }
                }
                group.setX(0);
                group.setY(cursorY);
                group.setWidth(availableWidth);
                group.setHeight(groupY);
                cursorY += groupY;
            }
        }

        table.setHeight(cursorY);
        return cursorY;
    }

    /**
     * Unified column width computation. Handles both colgroup and no-colgroup tables.
     *
     * 1. Measure min-content and max-content widths per column
     * 2. Determine preferred widths (colgroup proportions for non-empty cols, or max-content proportions)
     * 3. Apply min-content floor
     * 4. Distribute available width proportionally
     */
    private float[] computeColumnWidths(TableBox table, int numCols, float availableWidth) {
        float[] minW = new float[numCols];
        float[] maxW = new float[numCols];
        measureColumnWidths(table, numCols, minW, maxW);

        float[] preferred = new float[numCols];
        // returns the list that was populated when BoxTreeBuilder processed the colgroup element
        List<Float> colPcts = table.getColumnWidths();

        if (colPcts.size() == numCols) {
            // Colgroup exists — use proportions for non-empty columns, renormalize
            float totalPctNonEmpty = 0;
            for (int i = 0; i < numCols; i++) {
                if (maxW[i] > 0) {
                    totalPctNonEmpty += colPcts.get(i);
                }
            }

            if (totalPctNonEmpty > 0) {
                for (int i = 0; i < numCols; i++) {
                    if (maxW[i] > 0) {
                        preferred[i] = colPcts.get(i) / totalPctNonEmpty;
                    } else {
                        preferred[i] = 0;
                    }
                }
            } else {
                // All columns empty — equal distribution
                float equal = 1.0f / numCols;
                for (int i = 0; i < numCols; i++) {
                    preferred[i] = equal;
                }
            }
        } else {
            // No colgroup — empty list, or mismatched count - use max-content proportions
            float totalMaxContent = 0;
            for (float w : maxW) totalMaxContent += w;

            if (totalMaxContent > 0) {
                for (int i = 0; i < numCols; i++) {
                    preferred[i] = maxW[i] / totalMaxContent;
                }
            } else {
                float equal = 1.0f / numCols;
                for (int i = 0; i < numCols; i++) {
                    preferred[i] = equal;
                }
            }
        }

        // Allocate widths: start with preferred proportions, then enforce min-content floor
        float[] colWidths = new float[numCols];
        for (int i = 0; i < numCols; i++) {
            colWidths[i] = availableWidth * preferred[i];
        }

        // Enforce min-content width as floor for each column.
        // If a column's proportional share is less than its min-content width,
        // increase it and redistribute the deficit from other columns.
        float deficit = 0;
        float shrinkableTotal = 0;

        for (int i = 0; i < numCols; i++) {
            if (colWidths[i] < minW[i]) {
                deficit += minW[i] - colWidths[i];
                colWidths[i] = minW[i];
            } else {
                shrinkableTotal += colWidths[i] - minW[i];
            }
        }

        // Redistribute deficit proportionally from columns that have room above their min
        if (deficit > 0 && shrinkableTotal > 0) {
            float shrinkRatio = Math.min(1.0f, deficit / shrinkableTotal);
            for (int i = 0; i < numCols; i++) {
                float room = colWidths[i] - minW[i];
                if (room > 0) {
                    colWidths[i] -= room * shrinkRatio;
                }
            }
        }

        // Normalize to fill exactly availableWidth
        float totalAllocated = 0;
        for (float w : colWidths) totalAllocated += w;

        if (totalAllocated > 0 && Math.abs(totalAllocated - availableWidth) > 0.01f) {
            float scale = availableWidth / totalAllocated;
            for (int i = 0; i < numCols; i++) {
                colWidths[i] *= scale;
            }
        }

        return colWidths;
    }

    /**
     * Measures min-content and max-content widths per column across all rows.
     */
    private void measureColumnWidths(TableBox table, int numCols, float[] minW, float[] maxW) {
        for (Box child : table.getChildren()) {
            if (child instanceof TableRowBox row) {
                measureRowWidths(row, numCols, minW, maxW);
            } else if (child instanceof TableRowGroupBox group) {
                for (Box gc : group.getChildren()) {
                    if (gc instanceof TableRowBox row) {
                        measureRowWidths(row, numCols, minW, maxW);
                    }
                }
            }
        }
    }

    private void measureRowWidths(TableRowBox row, int numCols, float[] minW, float[] maxW) {
        int colIdx = 0;
        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell) {
                float cellMax = measureBoxContentWidth(cell);
                float cellMin = measureMinContentWidth(cell);
                int colspan = cell.getColspan();

                if (colspan == 1 && colIdx < numCols) {
                    maxW[colIdx] = Math.max(maxW[colIdx], cellMax);
                    minW[colIdx] = Math.max(minW[colIdx], cellMin);
                } else {
                    // Distribute evenly across spanned columns
                    float perColMax = cellMax / colspan;
                    float perColMin = cellMin / colspan;
                    for (int i = 0; i < colspan && (colIdx + i) < numCols; i++) {
                        maxW[colIdx + i] = Math.max(maxW[colIdx + i], perColMax);
                        minW[colIdx + i] = Math.max(minW[colIdx + i], perColMin);
                    }
                }

                colIdx += colspan;
            }
        }
    }

    /**
     * Determines the column count for the table.
     * Uses colgroup if available, otherwise scans rows.
     */
    private int determineColumnCount(TableBox table) {
        List<Float> colPcts = table.getColumnWidths();
        if (!colPcts.isEmpty()) {
            return colPcts.size();
        }
        return countColumnsFromRows(table);
    }

    /**
     * Measures the minimum content width of a box subtree — the widest
     * unbreakable word. This is the narrowest a column can be without
     * causing mid-word overflow.
     */
    private float measureMinContentWidth(Box box) {
        if (box instanceof TextRun textRun) {
            return measureWidestWord(textRun);
        }

        float maxWordWidth = 0;
        for (Box child : box.getChildren()) {
            maxWordWidth = Math.max(maxWordWidth, measureMinContentWidth(child));
        }
        return maxWordWidth;
    }

    /**
     * Measures the width of the widest individual word in a text run.
     */
    private float measureWidestWord(TextRun textRun) {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) return 0;

        ComputedStyle style = textRun.getStyle();
        PDFont font;
        float fontSize;

        if (style != null) {
            String[] families = CssValueParser.parseFontFamilyList(style.getFontFamily());
            boolean bold = style.isBold();
            boolean italic = style.isItalic();
            fontSize = style.getFontSize(fontSizePt);
            font = fontManager.getFont(families, bold, italic);
        } else {
            font = fontManager.getDefaultFont();
            fontSize = fontSizePt;
        }

        float maxWidth = 0;
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                float w = fontManager.measureText(word, font, fontSize);
                maxWidth = Math.max(maxWidth, w);
            }
        }
        return maxWidth;
    }

    /**
     * Lays out a single table row. Returns the row height (= tallest cell).
     */
    private float layoutRow(TableRowBox row, float[] colWidths, float tableWidth) {
        float maxCellHeight = 0;
        int colIdx = 0;
        Map<TableCellBox, Float> contentHeights = new HashMap<>();

        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell) {
                cell.setStartColumn(colIdx);

                // Calculate cell width from colspan
                int colspan = cell.getColspan();
                float cellWidth = 0;
                for (int i = 0; i < colspan && (colIdx + i) < colWidths.length; i++) {
                    cellWidth += colWidths[colIdx + i];
                }

                // If no column widths match, use equal distribution
                if (cellWidth <= 0 && colWidths.length > 0) {
                    cellWidth = tableWidth / colWidths.length * colspan;
                }

                // Resolve cell box model before reading padding/border
                bfc.resolveBoxModelWithWidth(cell, cellWidth);

                // Subtract cell's own border+padding from available content width
                float contentWidth = cellWidth
                        - cell.getPaddingLeft() - cell.getPaddingRight()
                        - cell.getBorderLeftWidth() - cell.getBorderRightWidth();
                contentWidth = Math.max(0, contentWidth);

                // Position cell
                float cellX = 0;
                for (int i = 0; i < colIdx && i < colWidths.length; i++) {
                    cellX += colWidths[i];
                }
                cell.setX(cellX);
                cell.setWidth(contentWidth);

                // Layout cell contents (cells act as block formatting contexts)
                float cellContentHeight = bfc.layoutChildren(cell, contentWidth);
                cell.setHeight(cellContentHeight);
                contentHeights.put(cell, cellContentHeight);

                float totalCellHeight = cellContentHeight
                        + cell.getPaddingTop() + cell.getPaddingBottom()
                        + cell.getBorderTopWidth() + cell.getBorderBottomWidth();
                maxCellHeight = Math.max(maxCellHeight, totalCellHeight);

                colIdx += colspan;
            }
        }

        // Enforce explicit cell heights as minimum row height
        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell && cell.getStyle() != null) {
                float explicitHeight = cell.getStyle().getHeight(-1, fontSizePt);
                if (explicitHeight > 0) {
                    maxCellHeight = Math.max(maxCellHeight, explicitHeight);
                }
            }
        }

        // Enforce explicit row height as minimum
        if (row.getStyle() != null) {
            float rowExplicitHeight = row.getStyle().getHeight(-1, fontSizePt);
            if (rowExplicitHeight > 0) {
                maxCellHeight = Math.max(maxCellHeight, rowExplicitHeight);
            }
        }

        // Equalize cell heights to row height
        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell) {
                float innerHeight = maxCellHeight
                        - cell.getPaddingTop() - cell.getPaddingBottom()
                        - cell.getBorderTopWidth() - cell.getBorderBottomWidth();
                cell.setHeight(Math.max(cell.getHeight(), innerHeight));
            }
        }

        // Apply vertical-align offset within cells
        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell) {
                float contentHeight = contentHeights.getOrDefault(cell, cell.getHeight());
                float innerHeight = cell.getHeight();
                float offset = 0;

                String valign = cell.getStyle() != null
                        ? cell.getStyle().getVerticalAlign() : "baseline";

                if ("middle".equals(valign)) {
                    offset = (innerHeight - contentHeight) / 2;
                } else if ("bottom".equals(valign)) {
                    offset = innerHeight - contentHeight;
                }
                // "top" and "baseline" → no offset (content stays at top)

                if (offset > 0) {
                    for (Box cellChild : cell.getEffectiveChildren()) {
                        cellChild.setY(cellChild.getY() + offset);
                    }
                }
            }
        }

        return maxCellHeight;
    }

    /**
     * Collects all direct row-level children (rows and row groups).
     */
    private List<Box> collectRows(TableBox table) {
        List<Box> rows = new ArrayList<>();
        for (Box child : table.getChildren()) {
            if (child instanceof TableRowBox || child instanceof TableRowGroupBox) {
                rows.add(child);
            }
        }
        return rows;
    }

    /**
     * Counts columns by scanning ALL rows and returning the maximum.
     */
    private int countColumnsFromRows(TableBox table) {
        int max = 0;
        for (Box child : table.getChildren()) {
            if (child instanceof TableRowBox row) {
                max = Math.max(max, countCellColumns(row));
            } else if (child instanceof TableRowGroupBox group) {
                for (Box gc : group.getChildren()) {
                    if (gc instanceof TableRowBox row) {
                        max = Math.max(max, countCellColumns(row));
                    }
                }
            }
        }
        return max;
    }

    private int countCellColumns(TableRowBox row) {
        int count = 0;
        for (Box child : row.getChildren()) {
            if (child instanceof TableCellBox cell) {
                count += cell.getColspan();
            }
        }
        return count;
    }

    /**
     * Recursively measures the total inline text width of a box subtree.
     * Returns the max-content width (single-line width with no breaks).
     */
    private float measureBoxContentWidth(Box box) {
        if (box instanceof TextRun textRun) {
            return measureTextRunWidth(textRun);
        }

        float totalWidth = 0;
        for (Box child : box.getChildren()) {
            totalWidth += measureBoxContentWidth(child);
        }
        return totalWidth;
    }

    private float measureTextRunWidth(TextRun textRun) {
        String text = textRun.getText();
        if (text == null || text.isEmpty()) return 0;

        ComputedStyle style = textRun.getStyle();
        PDFont font;
        float fontSize;

        if (style != null) {
            String[] families = CssValueParser.parseFontFamilyList(style.getFontFamily());
            boolean bold = style.isBold();
            boolean italic = style.isItalic();
            fontSize = style.getFontSize(fontSizePt);
            font = fontManager.getFont(families, bold, italic);
        } else {
            font = fontManager.getDefaultFont();
            fontSize = fontSizePt;
        }

        return fontManager.measureText(text, font, fontSize);
    }
}
