package io.ballerina.lib.pdf.box;

import io.ballerina.lib.pdf.css.ComputedStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all layout boxes.
 * Each box has position, size, margin/padding/border, a computed style, and children.
 */
public abstract class Box {

    // Position relative to parent's content area (in points)
    protected float x;
    protected float y;

    // Content dimensions (in points)
    protected float width;
    protected float height;

    // Margin
    protected float marginTop, marginRight, marginBottom, marginLeft;

    // Padding
    protected float paddingTop, paddingRight, paddingBottom, paddingLeft;

    // Border widths
    protected float borderTop, borderRight, borderBottom, borderLeft;

    protected ComputedStyle style;
    protected List<Box> children = new ArrayList<>();
    private List<Box> layoutChildren;  // positioned children from layout (null = use children)
    protected Box parent;

    public Box(ComputedStyle style) {
        this.style = style;
    }

    public void addChild(Box child) {
        children.add(child);
        child.parent = this;
    }

    // --- Total dimensions including margin/padding/border ---

    /** Total outer width (margin + border + padding + content). */
    public float getOuterWidth() {
        return marginLeft + borderLeft + paddingLeft + width + paddingRight + borderRight + marginRight;
    }

    /** Total outer height (margin + border + padding + content). */
    public float getOuterHeight() {
        return marginTop + borderTop + paddingTop + height + paddingBottom + borderBottom + marginBottom;
    }

    /** Width including border + padding + content (no margin). */
    public float getBorderBoxWidth() {
        return borderLeft + paddingLeft + width + paddingRight + borderRight;
    }

    /** Height including border + padding + content (no margin). */
    public float getBorderBoxHeight() {
        return borderTop + paddingTop + height + paddingBottom + borderBottom;
    }

    /** X position of the content area's left edge (absolute). */
    public float getContentX() {
        return getAbsoluteX() + borderLeft + paddingLeft;
    }

    /** Y position of the content area's top edge (absolute). */
    public float getContentY() {
        return getAbsoluteY() + borderTop + paddingTop;
    }

    /** Absolute X position (traversing parent chain). */
    public float getAbsoluteX() {
        float ax = x + marginLeft;
        if (parent != null) {
            ax += parent.getContentX();
        }
        return ax;
    }

    /** Absolute Y position (traversing parent chain). */
    public float getAbsoluteY() {
        float ay = y + marginTop;
        if (parent != null) {
            ay += parent.getContentY();
        }
        return ay;
    }

    // --- Getters/Setters ---

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getMarginTop() { return marginTop; }
    public void setMarginTop(float v) { this.marginTop = v; }
    public float getMarginRight() { return marginRight; }
    public void setMarginRight(float v) { this.marginRight = v; }
    public float getMarginBottom() { return marginBottom; }
    public void setMarginBottom(float v) { this.marginBottom = v; }
    public float getMarginLeft() { return marginLeft; }
    public void setMarginLeft(float v) { this.marginLeft = v; }

    public float getPaddingTop() { return paddingTop; }
    public void setPaddingTop(float v) { this.paddingTop = v; }
    public float getPaddingRight() { return paddingRight; }
    public void setPaddingRight(float v) { this.paddingRight = v; }
    public float getPaddingBottom() { return paddingBottom; }
    public void setPaddingBottom(float v) { this.paddingBottom = v; }
    public float getPaddingLeft() { return paddingLeft; }
    public void setPaddingLeft(float v) { this.paddingLeft = v; }

    public float getBorderTopWidth() { return borderTop; }
    public void setBorderTopWidth(float v) { this.borderTop = v; }
    public float getBorderRightWidth() { return borderRight; }
    public void setBorderRightWidth(float v) { this.borderRight = v; }
    public float getBorderBottomWidth() { return borderBottom; }
    public void setBorderBottomWidth(float v) { this.borderBottom = v; }
    public float getBorderLeftWidth() { return borderLeft; }
    public void setBorderLeftWidth(float v) { this.borderLeft = v; }

    public ComputedStyle getStyle() { return style; }
    public List<Box> getChildren() { return children; }
    public Box getParent() { return parent; }

    public List<Box> getLayoutChildren() { return layoutChildren; }
    public void setLayoutChildren(List<Box> lc) { this.layoutChildren = lc; }
    public List<Box> getEffectiveChildren() {
        return layoutChildren != null ? layoutChildren : children;
    }
    public void clearLayoutChildren() { this.layoutChildren = null; }

    public abstract String getBoxType();
}
