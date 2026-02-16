package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.HtmlPreprocessor;
import io.ballerina.lib.pdf.css.CssParser;
import io.ballerina.lib.pdf.css.CssStylesheet;
import io.ballerina.lib.pdf.css.StyleResolver;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoxTreeBuilderTest {

    private final HtmlPreprocessor preprocessor = new HtmlPreprocessor();

    /**
     * Builds a box tree from an HTML string and returns the root BlockBox (body).
     */
    private BlockBox buildTree(String html) {
        Document doc = preprocessor.parseOnly(html);
        CssStylesheet sheet = new CssParser().parse(doc);
        StyleResolver resolver = new StyleResolver(sheet);
        BoxTreeBuilder builder = new BoxTreeBuilder(resolver);
        return builder.build(doc);
    }

    // ===== Basic box types =====

    @Test
    void blockElementCreatesBlockBox() {
        BlockBox root = buildTree("<html><body><div>content</div></body></html>");
        // root is body (BlockBox), first child should be the div (BlockBox)
        assertFalse(root.getChildren().isEmpty(), "Body should have children");
        Box div = root.getChildren().get(0);
        assertInstanceOf(BlockBox.class, div);
        assertEquals("block", div.getBoxType());
    }

    @Test
    void inlineElementCreatesInlineBox() {
        BlockBox root = buildTree("<html><body><span>text</span></body></html>");
        assertFalse(root.getChildren().isEmpty());
        Box span = root.getChildren().get(0);
        assertInstanceOf(InlineBox.class, span);
        assertEquals("inline", span.getBoxType());
    }

    @Test
    void displayNoneSkipsElement() {
        BlockBox root = buildTree(
                "<html><head><style>.hidden { display: none; }</style></head>"
                + "<body><div class=\"hidden\">invisible</div><div>visible</div></body></html>");
        // Only the visible div should be in the tree
        List<Box> children = root.getChildren();
        assertEquals(1, children.size(), "display:none element should be skipped");
        assertInstanceOf(BlockBox.class, children.get(0));
    }

    // ===== Replaced elements =====

    @Test
    void imgCreatesReplacedBox() {
        BlockBox root = buildTree(
                "<html><body><img src=\"data:image/png;base64,abc\" /></body></html>");
        assertFalse(root.getChildren().isEmpty());
        Box img = root.getChildren().get(0);
        assertInstanceOf(ReplacedBox.class, img);
        assertEquals("data:image/png;base64,abc", ((ReplacedBox) img).getSrc());
    }

    @Test
    void imgParsesWidthHeight() {
        BlockBox root = buildTree(
                "<html><body><img src=\"x\" width=\"100\" height=\"50\" /></body></html>");
        Box img = root.getChildren().get(0);
        assertInstanceOf(ReplacedBox.class, img);
        ReplacedBox replaced = (ReplacedBox) img;
        // 100px * 0.75 = 75pt, 50px * 0.75 = 37.5pt
        assertEquals(75f, replaced.getIntrinsicWidth(), 0.01f);
        assertEquals(37.5f, replaced.getIntrinsicHeight(), 0.01f);
    }

    // ===== Table elements =====

    @Test
    void tableCreatesTableBox() {
        BlockBox root = buildTree(
                "<html><body><table><tr><td>cell</td></tr></table></body></html>");
        assertFalse(root.getChildren().isEmpty());
        Box table = root.getChildren().get(0);
        assertInstanceOf(TableBox.class, table);
        assertEquals("table", table.getBoxType());
    }

    @Test
    void tdCreatesTableCellBox() {
        BlockBox root = buildTree(
                "<html><body><table><tr><td>cell</td></tr></table></body></html>");
        // table > tbody (TableRowGroupBox) > tr (TableRowBox) > td (TableCellBox)
        Box table = root.getChildren().get(0);
        assertInstanceOf(TableBox.class, table);

        // Find the first TableCellBox in the tree
        TableCellBox cell = findFirstOfType(table, TableCellBox.class);
        assertNotNull(cell, "Should find a TableCellBox");
        assertEquals("table-cell", cell.getBoxType());
    }

    @Test
    void colspanParsedOnCell() {
        BlockBox root = buildTree(
                "<html><body><table><tr><td colspan=\"3\">wide</td></tr></table></body></html>");
        TableCellBox cell = findFirstOfType(root, TableCellBox.class);
        assertNotNull(cell);
        assertEquals(3, cell.getColspan());
    }

    // ===== Anonymous block wrapping =====

    @Test
    void mixedBlockInlineWrapsInAnonymous() {
        // When a block container has both block and inline children,
        // inline children get wrapped in anonymous BlockBoxes
        BlockBox root = buildTree(
                "<html><body><div>text before<p>block</p>text after</div></body></html>");
        Box div = root.getChildren().get(0);
        assertInstanceOf(BlockBox.class, div);

        // div should have 3 children: anon block (text before), p block, anon block (text after)
        List<Box> divChildren = div.getChildren();
        assertEquals(3, divChildren.size(),
                "Mixed block/inline should produce 3 children (anon, p, anon)");

        // First and last should be anonymous blocks wrapping the text runs
        assertInstanceOf(BlockBox.class, divChildren.get(0));
        assertInstanceOf(BlockBox.class, divChildren.get(1)); // the <p>
        assertInstanceOf(BlockBox.class, divChildren.get(2));
    }

    // ===== List items =====

    @Test
    void olListItemGetsNumber() {
        BlockBox root = buildTree(
                "<html><body><ol><li>first</li><li>second</li></ol></body></html>");
        Box ol = root.getChildren().get(0);
        assertInstanceOf(BlockBox.class, ol);

        // Each li is a BlockBox with a TextRun prefix
        List<Box> items = ol.getChildren();
        assertEquals(2, items.size());

        // First item should have "1. " as its first child
        Box firstItem = items.get(0);
        assertFalse(firstItem.getChildren().isEmpty());
        Box firstChild = firstItem.getChildren().get(0);
        assertInstanceOf(TextRun.class, firstChild);
        assertEquals("1. ", ((TextRun) firstChild).getText());

        // Second item should have "2. "
        Box secondChild = items.get(1).getChildren().get(0);
        assertInstanceOf(TextRun.class, secondChild);
        assertEquals("2. ", ((TextRun) secondChild).getText());
    }

    @Test
    void ulListItemGetsBullet() {
        BlockBox root = buildTree(
                "<html><body><ul><li>item</li></ul></body></html>");
        Box ul = root.getChildren().get(0);
        Box li = ul.getChildren().get(0);
        assertFalse(li.getChildren().isEmpty());

        Box bullet = li.getChildren().get(0);
        assertInstanceOf(TextRun.class, bullet);
        assertEquals("\u2022 ", ((TextRun) bullet).getText());
    }

    // ===== Cellpadding =====

    @Test
    void cellpaddingAppliedFromTable() {
        BlockBox root = buildTree(
                "<html><body><table cellpadding=\"5\"><tr><td>cell</td></tr></table></body></html>");
        TableCellBox cell = findFirstOfType(root, TableCellBox.class);
        assertNotNull(cell);
        assertEquals("5px", cell.getStyle().get("padding-top"));
        assertEquals("5px", cell.getStyle().get("padding-right"));
        assertEquals("5px", cell.getStyle().get("padding-bottom"));
        assertEquals("5px", cell.getStyle().get("padding-left"));
    }

    // ===== Flex/Grid fallback =====

    @Test
    void flexDisplayCreatesBlockBox() {
        BlockBox root = buildTree(
                "<html><head><style>.flex { display: flex; }</style></head>"
                + "<body><div class=\"flex\">flex content</div></body></html>");
        assertFalse(root.getChildren().isEmpty());
        Box flexDiv = root.getChildren().get(0);
        assertInstanceOf(BlockBox.class, flexDiv, "display:flex should produce a BlockBox");
    }

    @Test
    void gridDisplayCreatesBlockBox() {
        BlockBox root = buildTree(
                "<html><head><style>.grid { display: grid; }</style></head>"
                + "<body><div class=\"grid\">grid content</div></body></html>");
        assertFalse(root.getChildren().isEmpty());
        Box gridDiv = root.getChildren().get(0);
        assertInstanceOf(BlockBox.class, gridDiv, "display:grid should produce a BlockBox");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private <T extends Box> T findFirstOfType(Box root, Class<T> type) {
        if (type.isInstance(root)) return (T) root;
        for (Box child : root.getChildren()) {
            T found = findFirstOfType(child, type);
            if (found != null) return found;
        }
        return null;
    }
}
