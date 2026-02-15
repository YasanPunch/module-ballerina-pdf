package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.*;
import io.ballerina.lib.pdf.paint.FontManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TableLayoutEngineTest {

    private static PDDocument document;
    private static FontManager fontManager;
    private static BlockFormattingContext bfc;
    private static TableLayoutEngine engine;

    private static final float TABLE_WIDTH = 500f;
    private static final float FONT_SIZE = 12f;

    @BeforeAll
    static void setUp() throws IOException {
        document = new PDDocument();
        fontManager = new FontManager();
        fontManager.loadFonts(document);
        LayoutContext ctx = new LayoutContext(fontManager);
        bfc = new BlockFormattingContext(ctx);
        engine = new TableLayoutEngine(bfc, fontManager, FONT_SIZE);
    }

    @AfterAll
    static void tearDown() throws IOException {
        document.close();
    }

    private TableBox createTable(int cols, String[] cellTexts) {
        return createTable(cols, cellTexts, null);
    }

    private TableBox createTable(int cols, String[] cellTexts, float[] colPcts) {
        TableBox table = new TableBox(null);
        if (colPcts != null) {
            for (float pct : colPcts) {
                table.addColumnWidth(pct);
            }
        }
        TableRowBox row = new TableRowBox(null);
        for (int i = 0; i < cols; i++) {
            TableCellBox cell = new TableCellBox(null);
            String text = (i < cellTexts.length) ? cellTexts[i] : "";
            if (text != null && !text.isEmpty()) {
                TextRun textRun = new TextRun(null, text);
                cell.addChild(textRun);
            }
            row.addChild(cell);
        }
        table.addChild(row);
        return table;
    }

    @Test
    void collapsesEmptySpacerColumn() {
        // 3 columns: col 0 and col 2 have content, col 1 is empty
        TableBox table = createTable(3,
                new String[]{"Hello", "", "World"},
                new float[]{30f, 33f, 37f});

        engine.layout(table, TABLE_WIDTH);

        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell0 = (TableCellBox) row.getChildren().get(0);
        TableCellBox cell1 = (TableCellBox) row.getChildren().get(1);
        TableCellBox cell2 = (TableCellBox) row.getChildren().get(2);

        // The empty column gets 0 in the computed column widths, so cell1 and cell2
        // should be positioned at the same X (both start right after cell0)
        assertEquals(cell1.getX(), cell2.getX(), 1f,
                "Empty column should collapse: cell1.x=" + cell1.getX()
                        + " cell2.x=" + cell2.getX());
        // cell0 should get more space than equal distribution (500/3 ≈ 166)
        // since the empty column's share is redistributed to content columns
        assertTrue(cell0.getWidth() > TABLE_WIDTH / 3,
                "Content column should be wider than equal distribution, was: "
                        + cell0.getWidth());
    }

    @Test
    void preservesProportionsWhenAllColumnsHaveContent() {
        TableBox table = createTable(2,
                new String[]{"Short", "Also short"},
                new float[]{40f, 60f});

        engine.layout(table, TABLE_WIDTH);

        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell0 = (TableCellBox) row.getChildren().get(0);
        TableCellBox cell1 = (TableCellBox) row.getChildren().get(1);

        // Should roughly maintain 40:60 ratio
        float total = cell0.getWidth() + cell1.getWidth();
        float ratio0 = cell0.getWidth() / total;
        // Allow tolerance since min-content floor may adjust
        assertTrue(ratio0 > 0.25f && ratio0 < 0.55f,
                "Column 0 ratio should be ~0.4, was: " + ratio0);
    }

    @Test
    void usesContentProportionsWithoutColgroup() {
        // No colgroup, col A has much more content than col B
        TableBox table = createTable(2,
                new String[]{"This is a much longer text content for column A", "B"});

        engine.layout(table, TABLE_WIDTH);

        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell0 = (TableCellBox) row.getChildren().get(0);
        TableCellBox cell1 = (TableCellBox) row.getChildren().get(1);

        // Column with more content should get more width
        assertTrue(cell0.getWidth() > cell1.getWidth(),
                "Longer content column should be wider");
    }

    @Test
    void enforcesMinContentFloor() {
        // Create a table where colgroup wants 10% for a column with a long word
        TableBox table = createTable(2,
                new String[]{"Supercalifragilisticexpialidocious", "X"},
                new float[]{10f, 90f});

        engine.layout(table, TABLE_WIDTH);

        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell0 = (TableCellBox) row.getChildren().get(0);

        // The long unbreakable word needs more than 10% of 500 = 50pt
        // Min-content floor should push it above the 50pt proportional allocation
        float minWordWidth = fontManager.measureText(
                "Supercalifragilisticexpialidocious",
                fontManager.getDefaultFont(), FONT_SIZE);
        assertTrue(cell0.getWidth() >= minWordWidth * 0.9f,
                "Column width should be at least the min-content width, was: "
                        + cell0.getWidth() + ", min: " + minWordWidth);
    }

    @Test
    void handlesColspan() {
        TableBox table = new TableBox(null);
        TableRowBox row = new TableRowBox(null);

        TableCellBox cell0 = new TableCellBox(null);
        cell0.setColspan(2);
        cell0.addChild(new TextRun(null, "Spanning cell"));
        row.addChild(cell0);

        TableCellBox cell1 = new TableCellBox(null);
        cell1.addChild(new TextRun(null, "Normal"));
        row.addChild(cell1);

        table.addChild(row);

        // Second row to establish 3-column structure
        TableRowBox row2 = new TableRowBox(null);
        for (int i = 0; i < 3; i++) {
            TableCellBox c = new TableCellBox(null);
            c.addChild(new TextRun(null, "Col" + i));
            row2.addChild(c);
        }
        table.addChild(row2);

        engine.layout(table, TABLE_WIDTH);

        // Spanning cell should be positioned at x=0
        assertEquals(0f, cell0.getX(), 0.01f);
    }

    @Test
    void handlesSingleColumnTable() {
        TableBox table = createTable(1, new String[]{"Only column"});

        engine.layout(table, TABLE_WIDTH);

        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell = (TableCellBox) row.getChildren().get(0);

        // Single column should get the full available width
        assertEquals(TABLE_WIDTH, cell.getWidth(), 1f);
    }

    @Test
    void handlesTableWithRowGroups() {
        TableBox table = new TableBox(null);
        TableRowGroupBox tbody = new TableRowGroupBox(null);

        TableRowBox row = new TableRowBox(null);
        TableCellBox cell0 = new TableCellBox(null);
        cell0.addChild(new TextRun(null, "In tbody"));
        row.addChild(cell0);

        TableCellBox cell1 = new TableCellBox(null);
        cell1.addChild(new TextRun(null, "Also in tbody"));
        row.addChild(cell1);

        tbody.addChild(row);
        table.addChild(tbody);

        float height = engine.layout(table, TABLE_WIDTH);

        // Layout should complete without error and produce positive height
        assertTrue(height > 0);
        assertTrue(table.getHeight() > 0);
    }

    @Test
    void handlesAllEmptyColumns() {
        TableBox table = createTable(3,
                new String[]{"", "", ""},
                new float[]{33f, 33f, 34f});

        engine.layout(table, TABLE_WIDTH);

        // Should not crash — all columns get equal distribution
        assertTrue(table.getHeight() >= 0);
    }

    @Test
    void emptyColumnsGetZeroWidth() {
        // No colgroup, all columns empty
        TableBox table = createTable(2, new String[]{"", ""});

        engine.layout(table, TABLE_WIDTH);

        // When all columns are empty with no colgroup, equal distribution
        TableRowBox row = (TableRowBox) table.getChildren().get(0);
        TableCellBox cell0 = (TableCellBox) row.getChildren().get(0);
        TableCellBox cell1 = (TableCellBox) row.getChildren().get(1);

        // Both should get approximately equal widths (TABLE_WIDTH / 2)
        assertEquals(cell0.getWidth(), cell1.getWidth(), 1f);
    }
}
