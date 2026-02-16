package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;
import io.ballerina.lib.pdf.css.StyleResolver;
import io.ballerina.lib.pdf.util.CssValueParser;
import io.ballerina.lib.pdf.util.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a box tree from a styled W3C DOM.
 * Maps each element to the appropriate Box subclass based on its computed display value.
 */
public class BoxTreeBuilder {

    private static final Set<String> VOID_ELEMENTS = Set.of(
            "br", "hr", "img", "input", "meta", "link", "col"
    );

    private static final Pattern COL_WIDTH_PATTERN = Pattern.compile("width:\\s*([\\d.]+)%");

    private final StyleResolver styleResolver;

    public BoxTreeBuilder(StyleResolver styleResolver) {
        this.styleResolver = styleResolver;
    }

    /**
     * Builds the box tree from the document body.
     * Returns a root BlockBox representing the body element.
     */
    public BlockBox build(org.w3c.dom.Document document) {
        Element body = findBody(document);
        if (body == null) {
            // Create a minimal body
            ComputedStyle emptyStyle = new ComputedStyle();
            emptyStyle.set("display", "block");
            return new BlockBox(emptyStyle);
        }

        ComputedStyle bodyStyle = styleResolver.resolve(body);
        BlockBox root = new BlockBox(bodyStyle);
        buildChildren(body, root);
        return root;
    }

    private void buildChildren(Element parentElement, Box parentBox) {
        List<Node> childNodes = DomUtils.childNodes(parentElement);

        // Pass 1: Build all child boxes and recurse into their subtrees
        record ChildEntry(Box box, boolean blockLevel) {}
        List<ChildEntry> entries = new ArrayList<>();
        boolean hasBlockChild = false;

        for (Node child : childNodes) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = DomUtils.getCollapsedText(child);
                if (text.isEmpty()) continue;
                entries.add(new ChildEntry(new TextRun(parentBox.getStyle(), text), false));

            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childEl = (Element) child;
                ComputedStyle childStyle = styleResolver.resolve(childEl);
                String display = childStyle.getDisplay();
                if ("none".equals(display)) continue;

                String tagName = DomUtils.tagName(childEl);
                if (tagName.equals("colgroup") || tagName.equals("col")) continue;

                Box childBox = createBox(childEl, childStyle, display, tagName);
                if (childBox == null) continue;

                boolean blockLevel = isBlockLevel(display);
                if (blockLevel) hasBlockChild = true;

                // Recurse into subtree before wrapping
                if (!tagName.equals("br") && !VOID_ELEMENTS.contains(tagName)) {
                    buildChildren(childEl, childBox);
                }

                entries.add(new ChildEntry(childBox, blockLevel));
            }
        }

        // Pass 2: If mixed block/inline, wrap consecutive inline children in anonymous blocks.
        // This ensures BFC always sees either all-block or all-inline children.
        if (hasBlockChild && isBlockContainer(parentBox)) {
            List<Box> inlineRun = new ArrayList<>();
            for (ChildEntry entry : entries) {
                if (entry.blockLevel()) {
                    flushInlineRun(inlineRun, parentBox);
                    parentBox.addChild(entry.box());
                } else {
                    inlineRun.add(entry.box());
                }
            }
            flushInlineRun(inlineRun, parentBox);
        } else {
            for (ChildEntry entry : entries) {
                parentBox.addChild(entry.box());
            }
        }
    }

    /** Wraps accumulated inline children in an anonymous BlockBox and adds to parent. */
    private void flushInlineRun(List<Box> inlineRun, Box parentBox) {
        if (inlineRun.isEmpty()) return;
        BlockBox anon = new BlockBox(parentBox.getStyle());
        for (Box child : inlineRun) {
            anon.addChild(child);
        }
        parentBox.addChild(anon);
        inlineRun.clear();
    }

    private Box createBox(Element element, ComputedStyle style, String display, String tagName) {
        return switch (display) {
            case "block", "list-item", "inline-block", "flex", "inline-flex", "grid", "inline-grid" -> {
                BlockBox box = new BlockBox(style);
                if ("list-item".equals(display)) {
                    handleListItem(element, box);
                }
                yield box;
            }
            case "inline" -> {
                if (tagName.equals("img")) {
                    yield createReplacedBox(element, style);
                }
                yield new InlineBox(style);
            }
            case "table" -> {
                TableBox box = new TableBox(style);
                String bc = style.getBorderCollapse();
                box.setBorderCollapse("collapse".equals(bc));
                extractColumnWidths(element, box);
                yield box;
            }
            case "table-header-group", "table-row-group", "table-footer-group" ->
                    new TableRowGroupBox(style);
            case "table-row" ->
                    new TableRowBox(style);
            case "table-cell" -> {
                TableCellBox box = new TableCellBox(style);
                // Parse colspan
                String cs = DomUtils.attr(element, "colspan");
                if (cs != null) {
                    try {
                        box.setColspan(Math.max(1, Integer.parseInt(cs.trim())));
                    } catch (NumberFormatException ignored) {}
                }
                applyCellpadding(element, box);
                yield box;
            }
            default -> new InlineBox(style);
        };
    }

    private ReplacedBox createReplacedBox(Element element, ComputedStyle style) {
        ReplacedBox box = new ReplacedBox(style);

        String src = DomUtils.attr(element, "src");
        if (src != null) {
            box.setSrc(src);
        }

        // Parse width/height attributes for intrinsic size
        String widthAttr = DomUtils.attr(element, "width");
        String heightAttr = DomUtils.attr(element, "height");
        float w = widthAttr != null ? CssValueParser.toPoints(widthAttr) : 50 * 0.75f;
        float h = heightAttr != null ? CssValueParser.toPoints(heightAttr) : 50 * 0.75f;
        box.setIntrinsicWidth(w);
        box.setIntrinsicHeight(h);

        return box;
    }

    private void extractColumnWidths(Element tableElement, TableBox tableBox) {
        for (Element child : DomUtils.childElements(tableElement)) {
            if (DomUtils.tagName(child).equals("colgroup")) {
                for (Element col : DomUtils.childElements(child)) {
                    if (DomUtils.tagName(col).equals("col")) {
                        String colStyle = DomUtils.attr(col, "style");
                        if (colStyle != null) {
                            Matcher m = COL_WIDTH_PATTERN.matcher(colStyle);
                            if (m.find()) {
                                tableBox.addColumnWidth(Float.parseFloat(m.group(1)));
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleListItem(Element element, BlockBox box) {
        // Prepend a text run with the list number
        Node parent = element.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            String parentTag = DomUtils.tagName((Element) parent);
            if ("ol".equals(parentTag)) {
                int index = 0;
                for (Element sibling : DomUtils.childElements(parent)) {
                    if (DomUtils.tagName(sibling).equals("li")) {
                        index++;
                        if (sibling == element) break;
                    }
                }
                TextRun numberRun = new TextRun(box.getStyle(), index + ". ");
                box.addChild(numberRun);
            } else if ("ul".equals(parentTag)) {
                TextRun bulletRun = new TextRun(box.getStyle(), "\u2022 ");
                box.addChild(bulletRun);
            }
        }
    }

    private void applyCellpadding(Element cellElement, TableCellBox cellBox) {
        // Walk up to find parent table and check cellpadding attribute
        Node parent = cellElement.getParentNode();
        while (parent != null) {
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                String tag = DomUtils.tagName((Element) parent);
                if ("table".equals(tag)) {
                    String cellpadding = DomUtils.attr((Element) parent, "cellpadding");
                    if (cellpadding != null) {
                        // Bake cellpadding into ComputedStyle so resolveBoxModelWithWidth picks it up.
                        // Only apply if CSS doesn't already specify explicit padding.
                        String padValue = cellpadding + "px";
                        ComputedStyle style = cellBox.getStyle();
                        if (style.get("padding-top") == null) style.set("padding-top", padValue);
                        if (style.get("padding-right") == null) style.set("padding-right", padValue);
                        if (style.get("padding-bottom") == null) style.set("padding-bottom", padValue);
                        if (style.get("padding-left") == null) style.set("padding-left", padValue);
                    }
                    break;
                }
            }
            parent = parent.getParentNode();
        }
    }

    private boolean isBlockContainer(Box box) {
        return box instanceof BlockBox || box instanceof TableCellBox;
    }

    private boolean isBlockLevel(String display) {
        return display.equals("block") || display.equals("table") || display.equals("list-item")
                || display.equals("flex") || display.equals("grid")
                || display.startsWith("table-");
    }

    private Element findBody(org.w3c.dom.Document document) {
        List<Element> bodies = DomUtils.findAll(document, "body");
        return bodies.isEmpty() ? null : bodies.get(0);
    }
}
