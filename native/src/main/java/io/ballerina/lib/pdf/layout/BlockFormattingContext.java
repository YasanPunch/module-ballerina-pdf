package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.*;
import io.ballerina.lib.pdf.css.ComputedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements block formatting context: stacks block-level boxes vertically,
 * resolves widths, and delegates inline/table layout to their engines.
 */
public class BlockFormattingContext {

    private final LayoutContext ctx;
    private final InlineLayoutEngine inlineEngine;
    private final TableLayoutEngine tableEngine;

    public BlockFormattingContext(LayoutContext ctx) {
        this.ctx = ctx;
        this.inlineEngine = new InlineLayoutEngine(ctx.getFontManager(), ctx.getFontSizePt(), this);
        this.tableEngine = new TableLayoutEngine(this, ctx.getFontManager(), ctx.getFontSizePt());
    }

    /**
     * Lays out the root box (body) using the page content area width.
     */
    public void layout(BlockBox root) {
        float availableWidth = ctx.getContentWidth();
        root.setWidth(availableWidth);
        root.setX(0);
        root.setY(0);

        // Re-resolve box model with actual container width
        resolveBoxModelWithWidth(root, availableWidth);

        float contentWidth = availableWidth
                - root.getPaddingLeft() - root.getPaddingRight()
                - root.getBorderLeftWidth() - root.getBorderRightWidth();

        float contentHeight = layoutChildren(root, contentWidth);
        root.setHeight(contentHeight);
    }

    // --- Float tracking ---

    /** Tracks the position and extent of a floated box during layout. */
    private record FloatBox(Box box, float x, float y, float width, float height, String side) {}

    /** Returns the available content width at a given y, accounting for active floats. */
    private float getAvailableWidthAtY(float y, float containerWidth, List<FloatBox> floats) {
        float leftIntrusion = 0, rightIntrusion = 0;
        for (FloatBox f : floats) {
            if (y >= f.y && y < f.y + f.height) {
                if ("left".equals(f.side)) {
                    leftIntrusion = Math.max(leftIntrusion, f.x + f.width);
                } else {
                    rightIntrusion = Math.max(rightIntrusion, containerWidth - f.x);
                }
            }
        }
        return Math.max(0, containerWidth - leftIntrusion - rightIntrusion);
    }

    /** Returns the left offset at a given y caused by left floats. */
    private float getLeftOffsetAtY(float y, List<FloatBox> floats) {
        float offset = 0;
        for (FloatBox f : floats) {
            if ("left".equals(f.side) && y >= f.y && y < f.y + f.height) {
                offset = Math.max(offset, f.x + f.width);
            }
        }
        return offset;
    }

    /** Returns true if the child has a float style of left or right. */
    private boolean isFloated(Box child) {
        if (child instanceof BlockBox bb && bb.getStyle() != null) {
            String fl = bb.getStyle().getFloat();
            return "left".equals(fl) || "right".equals(fl);
        }
        return false;
    }

    /**
     * Applies clear by advancing cursorY past the bottom of relevant active floats.
     */
    private float applyClear(String clear, float cursorY, List<FloatBox> activeFloats) {
        if ("none".equals(clear)) return cursorY;
        float clearBelow = cursorY;
        for (FloatBox f : activeFloats) {
            boolean matches = "both".equals(clear)
                    || f.side.equals(clear);
            if (matches) {
                clearBelow = Math.max(clearBelow, f.y + f.height);
            }
        }
        return clearBelow;
    }

    /**
     * Lays out a floated child: resolves its box model and content, then positions it
     * in the float band at cursorY, pushing it down if it doesn't fit horizontally.
     */
    private FloatBox layoutFloatedChild(BlockBox block, float containerWidth, float cursorY,
                                         List<FloatBox> activeFloats) {
        resolveBoxModelWithWidth(block, containerWidth);
        ComputedStyle style = block.getStyle();
        if (style == null) return new FloatBox(block, 0, cursorY, 0, 0, "left");
        float fontSize = style.getFontSize(ctx.getFontSizePt());
        String side = style.getFloat();

        // Resolve width (shrink-to-fit for floats without explicit width)
        float explicitWidth = style.getWidth(containerWidth, fontSize);
        float contentWidth;
        if (explicitWidth > 0) {
            contentWidth = explicitWidth;
        } else {
            contentWidth = containerWidth
                    - block.getMarginLeft() - block.getMarginRight()
                    - block.getBorderLeftWidth() - block.getBorderRightWidth()
                    - block.getPaddingLeft() - block.getPaddingRight();
            contentWidth = Math.max(0, contentWidth);
        }

        // Clamp to min-width / max-width
        float maxW = style.getMaxWidth(containerWidth, fontSize);
        float minW = style.getMinWidth(containerWidth, fontSize);
        contentWidth = Math.max(minW, Math.min(contentWidth, maxW));
        block.setWidth(contentWidth);

        // Layout float contents
        float contentHeight = layoutChildren(block, contentWidth);
        float explicitHeight = style.getHeight(contentHeight, fontSize);
        if (explicitHeight > 0) {
            contentHeight = Math.max(contentHeight, explicitHeight);
        }
        float maxH = style.getMaxHeight(contentHeight, fontSize);
        float minH = style.getMinHeight(contentHeight, fontSize);
        contentHeight = Math.max(minH, Math.min(contentHeight, maxH));
        block.setHeight(contentHeight);

        float outerWidth = block.getOuterWidth();
        float outerHeight = block.getOuterHeight();

        // Find placement Y — drop below conflicting floats if no room
        float placeY = cursorY;
        while (true) {
            float avail = getAvailableWidthAtY(placeY, containerWidth, activeFloats);
            if (outerWidth <= avail + 0.01f) break;
            // Drop below the lowest float that overlaps placeY
            float nextY = Float.MAX_VALUE;
            for (FloatBox f : activeFloats) {
                if (placeY >= f.y && placeY < f.y + f.height) {
                    nextY = Math.min(nextY, f.y + f.height);
                }
            }
            if (nextY == Float.MAX_VALUE) break;
            placeY = nextY;
        }

        // Position horizontally
        float placeX;
        if ("left".equals(side)) {
            placeX = getLeftOffsetAtY(placeY, activeFloats);
        } else {
            float rightIntrusion = 0;
            for (FloatBox f : activeFloats) {
                if ("right".equals(f.side) && placeY >= f.y && placeY < f.y + f.height) {
                    rightIntrusion = Math.max(rightIntrusion, containerWidth - f.x);
                }
            }
            placeX = containerWidth - rightIntrusion - outerWidth;
        }

        block.setX(placeX);
        block.setY(placeY);

        return new FloatBox(block, placeX, placeY, outerWidth, outerHeight, side);
    }

    /**
     * Lays out children of a block container. Returns total height consumed.
     */
    public float layoutChildren(Box container, float availableWidth) {
        // Clear previous layout results so re-layout works correctly
        container.clearLayoutChildren();
        if (container.getChildren().isEmpty()) return 0;

        // Partition children: absolute, floated, and flow — preserving document order
        // for flow+float (floats must be processed at their source position).
        List<Box> absoluteChildren = new ArrayList<>();
        List<Box> orderedChildren = new ArrayList<>(); // flow + float in document order
        boolean hasFloats = false;

        for (Box child : container.getChildren()) {
            if (child instanceof BlockBox bb && bb.getStyle() != null
                    && "absolute".equals(bb.getStyle().getPosition())) {
                absoluteChildren.add(bb);
            } else {
                orderedChildren.add(child);
                if (isFloated(child)) hasFloats = true;
            }
        }

        // Determine if children contain block-level elements (excluding floats,
        // which are removed from normal flow)
        boolean hasBlockChildren = false;

        for (Box child : orderedChildren) {
            if (isFloated(child)) continue; // floats don't determine block/inline context
            if (child instanceof BlockBox bb
                    && bb.getStyle() != null
                    && "inline-block".equals(bb.getStyle().getDisplay())) {
                // inline-block doesn't create block context
            } else if (child instanceof BlockBox || child instanceof TableBox
                    || child instanceof TableRowGroupBox || child instanceof TableRowBox) {
                hasBlockChildren = true;
            }
        }

        // Pure inline context (no block children) — delegate to inline engine
        if (!hasBlockChildren && !hasFloats) {
            float height = inlineEngine.layout(container, availableWidth);
            if (!absoluteChildren.isEmpty()) {
                layoutAbsoluteChildren(container, absoluteChildren, availableWidth, height);
            }
            return height;
        }

        // Block layout (may include floats): process children in document order
        List<FloatBox> activeFloats = hasFloats ? new ArrayList<>() : null;
        float cursorY = 0;

        // If all non-float children are inline, we still need block-level processing
        // to handle the floats, but delegate the inline content to the inline engine
        // with float-aware widths.
        if (!hasBlockChildren && hasFloats) {
            // Process children in order: floats get positioned, inline content
            // gets laid out with float-aware widths
            List<Box> inlineChildren = new ArrayList<>();
            for (Box child : orderedChildren) {
                if (isFloated(child)) {
                    // Lay out any accumulated inline content before this float
                    if (!inlineChildren.isEmpty()) {
                        cursorY = layoutInlineRunWithFloats(container, inlineChildren,
                                availableWidth, cursorY, activeFloats);
                        inlineChildren.clear();
                    }
                    FloatBox fb = layoutFloatedChild((BlockBox) child, availableWidth,
                            cursorY, activeFloats);
                    activeFloats.add(fb);
                } else {
                    inlineChildren.add(child);
                }
            }
            // Lay out remaining inline content
            if (!inlineChildren.isEmpty()) {
                cursorY = layoutInlineRunWithFloats(container, inlineChildren,
                        availableWidth, cursorY, activeFloats);
            }

            // Container height must encompass floats
            for (FloatBox f : activeFloats) {
                cursorY = Math.max(cursorY, f.y + f.height);
            }

            if (!absoluteChildren.isEmpty()) {
                layoutAbsoluteChildren(container, absoluteChildren, availableWidth, cursorY);
            }
            return cursorY;
        }

        // Mixed block context (blocks, tables, possibly floats).
        // Track pending bottom margin for CSS 2.1 §8.3.1 adjacent sibling margin collapsing.
        float pendingMarginBottom = 0;
        boolean isFirstChild = true;

        // CSS 2.1 §8.3.1 parent-child margin collapsing:
        // Parent's top margin collapses with first child's top margin when parent has
        // no border-top and no padding-top. Similarly, parent's bottom margin collapses
        // with last child's bottom margin when parent has no border-bottom and no padding-bottom.
        ComputedStyle containerStyle = container.getStyle();
        boolean isTableCell = container instanceof TableCellBox;
        boolean collapseFirstChildTop = containerStyle != null
                && !isTableCell
                && container.getBorderTopWidth() == 0 && container.getPaddingTop() == 0;
        boolean collapseLastChildBottom = containerStyle != null
                && !isTableCell
                && container.getBorderBottomWidth() == 0 && container.getPaddingBottom() == 0
                && (containerStyle.getHeight(0, 0) < 0); // height: auto
        for (Box child : orderedChildren) {
            // Handle floated children — floats don't participate in margin collapsing
            if (hasFloats && isFloated(child)) {
                FloatBox fb = layoutFloatedChild((BlockBox) child, availableWidth,
                        cursorY, activeFloats);
                activeFloats.add(fb);
                continue;
            }

            // Apply clear if specified
            if (hasFloats && child instanceof BlockBox bb && bb.getStyle() != null) {
                cursorY = applyClear(bb.getStyle().getClear(), cursorY, activeFloats);
            }

            // Determine if this child participates in margin collapsing.
            // Floats and inline-blocks do not collapse margins.
            boolean collapseMargins = !isFloated(child)
                    && !(child instanceof BlockBox bb2 && bb2.getStyle() != null
                         && "inline-block".equals(bb2.getStyle().getDisplay()));

            if (child instanceof TableBox table) {
                resolveBoxModelWithWidth(table, availableWidth);

                ComputedStyle tableStyle = table.getStyle();
                float tableFontSize = tableStyle != null ? tableStyle.getFontSize(ctx.getFontSizePt()) : ctx.getFontSizePt();
                float tableContentWidth = availableWidth
                        - table.getMarginLeft() - table.getMarginRight()
                        - table.getBorderLeftWidth() - table.getBorderRightWidth()
                        - table.getPaddingLeft() - table.getPaddingRight();

                if (tableStyle != null) {
                    float tMaxW = tableStyle.getMaxWidth(availableWidth, tableFontSize);
                    float tMinW = tableStyle.getMinWidth(availableWidth, tableFontSize);
                    tableContentWidth = Math.max(tMinW, Math.min(tableContentWidth, tMaxW));
                }

                // Collapse top margin with previous sibling's bottom margin
                float effectiveMarginTop;
                if (isFirstChild && collapseFirstChildTop && collapseMargins) {
                    // Parent-first-child collapsing: margin collapses with parent's top margin
                    effectiveMarginTop = 0;
                } else if (!isFirstChild && collapseMargins) {
                    effectiveMarginTop = Math.max(pendingMarginBottom, table.getMarginTop());
                } else {
                    effectiveMarginTop = (isFirstChild ? 0 : pendingMarginBottom) + table.getMarginTop();
                }
                cursorY += effectiveMarginTop;

                // CSS 2.1 §10.3.3: center block with margin-left: auto and margin-right: auto
                float tableXOffset = 0;
                if (tableStyle != null && tableStyle.isMarginLeftAuto() && tableStyle.isMarginRightAuto()) {
                    float totalOuter = tableContentWidth + table.getBorderLeftWidth() + table.getPaddingLeft()
                            + table.getPaddingRight() + table.getBorderRightWidth();
                    tableXOffset = Math.max(0, (availableWidth - totalOuter) / 2f);
                }
                table.setX(tableXOffset);
                table.setY(cursorY - table.getMarginTop());

                float tableHeight = tableEngine.layout(table, tableContentWidth);
                table.setHeight(tableHeight);

                cursorY += table.getBorderTopWidth() + table.getPaddingTop()
                        + tableHeight
                        + table.getPaddingBottom() + table.getBorderBottomWidth();

                pendingMarginBottom = table.getMarginBottom();
                isFirstChild = false;

                if (tableStyle != null && "relative".equals(tableStyle.getPosition())) {
                    applyRelativeOffset(table, availableWidth, tableHeight);
                }

            } else if (child instanceof BlockBox block) {
                resolveBoxModelWithWidth(block, availableWidth);

                ComputedStyle style = block.getStyle();
                if (style == null) continue;
                float explicitWidth = style.getWidth(availableWidth, ctx.getFontSizePt());
                float blockWidth;
                if (explicitWidth > 0) {
                    blockWidth = Math.min(explicitWidth, availableWidth
                            - block.getMarginLeft() - block.getMarginRight()
                            - block.getBorderLeftWidth() - block.getBorderRightWidth()
                            - block.getPaddingLeft() - block.getPaddingRight());
                } else {
                    blockWidth = availableWidth
                            - block.getMarginLeft() - block.getMarginRight()
                            - block.getBorderLeftWidth() - block.getBorderRightWidth()
                            - block.getPaddingLeft() - block.getPaddingRight();
                }
                blockWidth = Math.max(0, blockWidth);

                float fontSize = style.getFontSize(ctx.getFontSizePt());
                float maxW = style.getMaxWidth(availableWidth, fontSize);
                float minW = style.getMinWidth(availableWidth, fontSize);
                blockWidth = Math.max(minW, Math.min(blockWidth, maxW));

                // Collapse top margin with previous sibling's bottom margin
                float effectiveMarginTop;
                if (isFirstChild && collapseFirstChildTop && collapseMargins) {
                    // Parent-first-child collapsing: margin collapses with parent's top margin
                    effectiveMarginTop = 0;
                } else if (!isFirstChild && collapseMargins) {
                    effectiveMarginTop = Math.max(pendingMarginBottom, block.getMarginTop());
                } else {
                    effectiveMarginTop = (isFirstChild ? 0 : pendingMarginBottom) + block.getMarginTop();
                }
                cursorY += effectiveMarginTop;

                // CSS 2.1 §10.3.3: center block with margin-left: auto and margin-right: auto.
                // Applies when the block is narrower than the container (due to explicit width
                // or max-width constraint) and both horizontal margins are auto.
                float blockXOffset = 0;
                float totalOuter = blockWidth + block.getBorderLeftWidth() + block.getPaddingLeft()
                        + block.getPaddingRight() + block.getBorderRightWidth();
                if (style.isMarginLeftAuto() && style.isMarginRightAuto()
                        && totalOuter < availableWidth) {
                    blockXOffset = (availableWidth - totalOuter) / 2f;
                }
                block.setX(blockXOffset);
                block.setY(cursorY - block.getMarginTop());
                block.setWidth(blockWidth);

                float contentHeight = layoutChildren(block, blockWidth);
                float explicitHeight = style.getHeight(contentHeight, fontSize);
                if (explicitHeight > 0) {
                    contentHeight = Math.max(contentHeight, explicitHeight);
                }
                float maxH = style.getMaxHeight(contentHeight, fontSize);
                float minH = style.getMinHeight(contentHeight, fontSize);
                contentHeight = Math.max(minH, Math.min(contentHeight, maxH));

                block.setHeight(contentHeight);

                cursorY += block.getBorderTopWidth() + block.getPaddingTop()
                        + contentHeight
                        + block.getPaddingBottom() + block.getBorderBottomWidth();

                pendingMarginBottom = block.getMarginBottom();
                isFirstChild = false;

                if ("relative".equals(style.getPosition())) {
                    applyRelativeOffset(block, availableWidth, contentHeight);
                }

            } else {
                // Inline content mixed with blocks — position at cursor
                // Inline content resets margin collapsing
                if (!isFirstChild) {
                    cursorY += pendingMarginBottom;
                }
                pendingMarginBottom = 0;
                isFirstChild = false;

                resolveBoxModelWithWidth(child, availableWidth);
                child.setX(0);
                child.setY(cursorY);
                cursorY += child.getHeight() > 0 ? child.getHeight() : ctx.getFontSizePt() * 1.333f;
            }
        }

        // Flush any remaining pending bottom margin.
        // CSS 2.1 §8.3.1: if parent has no border-bottom/padding-bottom and height is auto,
        // last child's bottom margin collapses with parent's bottom margin (skip it here).
        if (!collapseLastChildBottom) {
            cursorY += pendingMarginBottom;
        }

        // Container height must encompass floats
        if (hasFloats) {
            for (FloatBox f : activeFloats) {
                cursorY = Math.max(cursorY, f.y + f.height);
            }
        }

        // Position absolute children after flow layout completes
        if (!absoluteChildren.isEmpty()) {
            layoutAbsoluteChildren(container, absoluteChildren, availableWidth, cursorY);
        }

        return cursorY;
    }

    /**
     * Lays out a run of inline children with float-aware line widths.
     * Creates a temporary wrapper, delegates to the inline engine, then merges
     * the positioned children back into the container.
     */
    private float layoutInlineRunWithFloats(Box container, List<Box> inlineChildren,
                                             float availableWidth, float startY,
                                             List<FloatBox> activeFloats) {
        // Create a temporary container for the inline run
        BlockBox wrapper = new BlockBox(container.getStyle());
        for (Box child : inlineChildren) {
            wrapper.addChild(child);
        }

        InlineLayoutEngine.LineWidthProvider provider = (float y) -> {
            float w = getAvailableWidthAtY(y, availableWidth, activeFloats);
            float xOff = getLeftOffsetAtY(y, activeFloats);
            return new float[]{w, xOff};
        };

        float height = inlineEngine.layout(wrapper, availableWidth, provider, startY);

        // Transfer positioned children to the real container
        List<Box> positioned = wrapper.getEffectiveChildren();
        if (positioned != null) {
            List<Box> existing = container.getLayoutChildren();
            if (existing == null) {
                existing = new ArrayList<>();
            }
            for (Box child : positioned) {
                child.setY(child.getY() + startY);
                existing.add(child);
            }
            container.setLayoutChildren(existing);
        }

        return startY + height;
    }

    /**
     * Applies CSS relative positioning offsets to a box.
     * Per CSS 2.1 §9.4.3: top takes precedence over bottom, left over right.
     * The box's flow position is already set; this only adjusts the visual position.
     */
    private void applyRelativeOffset(Box box, float containerWidth, float containerHeight) {
        ComputedStyle style = box.getStyle();
        if (style == null) return;
        float fontSize = style.getFontSize(ctx.getFontSizePt());
        float top = style.getTop(containerHeight, fontSize);
        float left = style.getLeft(containerWidth, fontSize);
        float right = style.getRight(containerWidth, fontSize);
        float bottom = style.getBottom(containerHeight, fontSize);

        if (!Float.isNaN(top))         box.setY(box.getY() + top);
        else if (!Float.isNaN(bottom)) box.setY(box.getY() - bottom);

        if (!Float.isNaN(left))        box.setX(box.getX() + left);
        else if (!Float.isNaN(right))  box.setX(box.getX() - right);
    }

    /**
     * Lays out absolute-positioned children and appends them after flow children
     * so they paint on top.
     */
    private void layoutAbsoluteChildren(Box container, List<Box> absoluteChildren,
                                         float containerWidth, float containerHeight) {
        for (Box absChild : absoluteChildren) {
            layoutAbsoluteChild((BlockBox) absChild, containerWidth, containerHeight);
        }

        // Reorder for painting: flow children first, then absolute on top
        List<Box> ordered = new ArrayList<>();
        List<Box> effective = container.getEffectiveChildren();
        if (effective != null) {
            for (Box child : effective) {
                if (!absoluteChildren.contains(child)) {
                    ordered.add(child);
                }
            }
        }
        ordered.addAll(absoluteChildren);
        container.setLayoutChildren(ordered);
    }

    /**
     * Positions and sizes a single absolute-positioned child.
     * The child is removed from normal flow and positioned relative to its
     * containing block using top/left/right/bottom offsets.
     */
    private void layoutAbsoluteChild(BlockBox box, float containerWidth, float containerHeight) {
        ComputedStyle style = box.getStyle();
        if (style == null) return;
        float fontSize = style.getFontSize(ctx.getFontSizePt());
        resolveBoxModelWithWidth(box, containerWidth);

        // Resolve width
        float explicitWidth = style.getWidth(containerWidth, fontSize);
        float contentWidth;
        if (explicitWidth > 0) {
            contentWidth = explicitWidth;
        } else {
            contentWidth = containerWidth
                    - box.getMarginLeft() - box.getMarginRight()
                    - box.getBorderLeftWidth() - box.getBorderRightWidth()
                    - box.getPaddingLeft() - box.getPaddingRight();
            contentWidth = Math.max(0, contentWidth);
        }
        box.setWidth(contentWidth);

        // Layout contents
        float contentHeight = layoutChildren(box, contentWidth);
        float explicitHeight = style.getHeight(containerHeight, fontSize);
        if (explicitHeight > 0) contentHeight = explicitHeight;
        box.setHeight(contentHeight);

        // Position using offsets
        float top = style.getTop(containerHeight, fontSize);
        float left = style.getLeft(containerWidth, fontSize);
        float right = style.getRight(containerWidth, fontSize);
        float bottom = style.getBottom(containerHeight, fontSize);

        // Horizontal positioning
        if (!Float.isNaN(left)) {
            box.setX(left);
        } else if (!Float.isNaN(right)) {
            box.setX(containerWidth - box.getOuterWidth() + box.getMarginLeft() - right);
        } else {
            box.setX(0);
        }

        // Vertical positioning
        if (!Float.isNaN(top)) {
            box.setY(top);
        } else if (!Float.isNaN(bottom)) {
            box.setY(containerHeight - box.getOuterHeight() + box.getMarginTop() - bottom);
        } else {
            box.setY(0);
        }
    }

    /**
     * Resolves margin/padding/border values with actual container width.
     * Package-private so TableLayoutEngine can resolve cell box models before reading geometry.
     */
    void resolveBoxModelWithWidth(Box box, float containerWidth) {
        ComputedStyle style = box.getStyle();
        if (style == null) return;

        float fontSize = style.getFontSize(ctx.getFontSizePt());

        box.setMarginTop(style.getMarginTop(containerWidth, fontSize));
        box.setMarginRight(style.getMarginRight(containerWidth, fontSize));
        box.setMarginBottom(style.getMarginBottom(containerWidth, fontSize));
        box.setMarginLeft(style.getMarginLeft(containerWidth, fontSize));

        box.setPaddingTop(style.getPaddingTop(containerWidth, fontSize));
        box.setPaddingRight(style.getPaddingRight(containerWidth, fontSize));
        box.setPaddingBottom(style.getPaddingBottom(containerWidth, fontSize));
        box.setPaddingLeft(style.getPaddingLeft(containerWidth, fontSize));

        box.setBorderTopWidth(style.getBorderTopWidth(containerWidth, fontSize));
        box.setBorderRightWidth(style.getBorderRightWidth(containerWidth, fontSize));
        box.setBorderBottomWidth(style.getBorderBottomWidth(containerWidth, fontSize));
        box.setBorderLeftWidth(style.getBorderLeftWidth(containerWidth, fontSize));
    }
}
