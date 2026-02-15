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
    private TableLayoutEngine tableEngine;

    public BlockFormattingContext(LayoutContext ctx) {
        this.ctx = ctx;
        this.inlineEngine = new InlineLayoutEngine(ctx.getFontManager(), ctx.getDefaultFontSizePt(), this);
        this.tableEngine = new TableLayoutEngine(this, ctx.getFontManager(), ctx.getDefaultFontSizePt());
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

    /**
     * Lays out children of a block container. Returns total height consumed.
     */
    public float layoutChildren(Box container, float availableWidth) {
        // Clear previous layout results so re-layout works correctly
        container.clearLayoutChildren();
        if (container.getChildren().isEmpty()) return 0;

        // Partition absolute-positioned children from normal flow
        List<Box> flowChildren = new ArrayList<>();
        List<Box> absoluteChildren = new ArrayList<>();
        for (Box child : container.getChildren()) {
            if (child instanceof BlockBox bb && bb.getStyle() != null
                    && "absolute".equals(bb.getStyle().getPosition())) {
                absoluteChildren.add(bb);
            } else {
                flowChildren.add(child);
            }
        }

        // Determine if flow children are all inline or contain blocks
        boolean hasBlockChildren = false;
        boolean hasInlineChildren = false;

        for (Box child : flowChildren) {
            if (child instanceof BlockBox bb
                    && bb.getStyle() != null
                    && "inline-block".equals(bb.getStyle().getDisplay())) {
                hasInlineChildren = true;
            } else if (child instanceof BlockBox || child instanceof TableBox
                    || child instanceof TableRowGroupBox || child instanceof TableRowBox) {
                hasBlockChildren = true;
            } else {
                hasInlineChildren = true;
            }
        }

        if (!hasBlockChildren) {
            // All inline: use inline layout engine
            float height = inlineEngine.layout(container, availableWidth);
            if (!absoluteChildren.isEmpty()) {
                layoutAbsoluteChildren(container, absoluteChildren, availableWidth, height);
            }
            return height;
        }

        // Block layout: stack children vertically
        float cursorY = 0;

        for (Box child : flowChildren) {
            if (child instanceof TableBox table) {
                // Re-resolve box model with actual width
                resolveBoxModelWithWidth(table, availableWidth);

                ComputedStyle tableStyle = table.getStyle();
                float tableFontSize = tableStyle != null ? tableStyle.getFontSize(ctx.getDefaultFontSizePt()) : ctx.getDefaultFontSizePt();
                float tableContentWidth = availableWidth
                        - table.getMarginLeft() - table.getMarginRight()
                        - table.getBorderLeftWidth() - table.getBorderRightWidth()
                        - table.getPaddingLeft() - table.getPaddingRight();

                // Clamp to min-width / max-width
                if (tableStyle != null) {
                    float tMaxW = tableStyle.getMaxWidth(availableWidth, tableFontSize);
                    float tMinW = tableStyle.getMinWidth(availableWidth, tableFontSize);
                    tableContentWidth = Math.max(tMinW, Math.min(tableContentWidth, tMaxW));
                }

                table.setX(0);
                table.setY(cursorY);

                float tableHeight = tableEngine.layout(table, tableContentWidth);
                table.setHeight(tableHeight);

                cursorY += table.getMarginTop()
                        + table.getBorderTopWidth() + table.getPaddingTop()
                        + tableHeight
                        + table.getPaddingBottom() + table.getBorderBottomWidth()
                        + table.getMarginBottom();

                // Apply relative offset (visual only, does not affect cursorY)
                if (tableStyle != null && "relative".equals(tableStyle.getPosition())) {
                    applyRelativeOffset(table, availableWidth, tableHeight);
                }

            } else if (child instanceof BlockBox block) {
                // Re-resolve box model with actual width
                resolveBoxModelWithWidth(block, availableWidth);

                // Resolve explicit width
                ComputedStyle style = block.getStyle();
                float explicitWidth = style.getWidth(availableWidth, ctx.getDefaultFontSizePt());
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

                // Clamp to min-width / max-width
                float fontSize = style.getFontSize(ctx.getDefaultFontSizePt());
                float maxW = style.getMaxWidth(availableWidth, fontSize);
                float minW = style.getMinWidth(availableWidth, fontSize);
                blockWidth = Math.max(minW, Math.min(blockWidth, maxW));

                block.setX(0);
                block.setY(cursorY);
                block.setWidth(blockWidth);

                float contentHeight = layoutChildren(block, blockWidth);
                float explicitHeight = style.getHeight(contentHeight, fontSize);
                if (explicitHeight > 0) {
                    contentHeight = Math.max(contentHeight, explicitHeight);
                }
                // Clamp to min-height / max-height
                float maxH = style.getMaxHeight(contentHeight, fontSize);
                float minH = style.getMinHeight(contentHeight, fontSize);
                contentHeight = Math.max(minH, Math.min(contentHeight, maxH));

                block.setHeight(contentHeight);

                cursorY += block.getMarginTop()
                        + block.getBorderTopWidth() + block.getPaddingTop()
                        + contentHeight
                        + block.getPaddingBottom() + block.getBorderBottomWidth()
                        + block.getMarginBottom();

                // Apply relative offset (visual only, does not affect cursorY)
                if ("relative".equals(style.getPosition())) {
                    applyRelativeOffset(block, availableWidth, contentHeight);
                }

            } else {
                // Inline content mixed with blocks — position at cursor
                resolveBoxModelWithWidth(child, availableWidth);
                child.setX(0);
                child.setY(cursorY);
                cursorY += child.getHeight() > 0 ? child.getHeight() : ctx.getDefaultFontSizePt() * 1.333f;
            }
        }

        // Position absolute children after flow layout completes
        if (!absoluteChildren.isEmpty()) {
            layoutAbsoluteChildren(container, absoluteChildren, availableWidth, cursorY);
        }

        return cursorY;
    }

    /**
     * Applies CSS relative positioning offsets to a box.
     * Per CSS 2.1 §9.4.3: top takes precedence over bottom, left over right.
     * The box's flow position is already set; this only adjusts the visual position.
     */
    private void applyRelativeOffset(Box box, float containerWidth, float containerHeight) {
        ComputedStyle style = box.getStyle();
        float fontSize = style.getFontSize(ctx.getDefaultFontSizePt());
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
        float fontSize = style.getFontSize(ctx.getDefaultFontSizePt());
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

        float fontSize = style.getFontSize(ctx.getDefaultFontSizePt());

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
