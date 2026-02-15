package io.ballerina.lib.pdf.layout;

import io.ballerina.lib.pdf.box.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a laid-out box tree across pages.
 * Breaks at block boundaries — never mid-cell or mid-text-line.
 * Returns a list of "page slices" (Y offset ranges).
 */
public class PageBreaker {

    /**
     * A page slice defines a vertical range [startY, endY) of content.
     */
    public record PageSlice(float startY, float endY) {
        public float height() { return endY - startY; }
    }

    /**
     * Computes page breaks for the given root box and page content height.
     */
    public List<PageSlice> computePages(Box root, float pageContentHeight) {
        List<PageSlice> pages = new ArrayList<>();
        float totalHeight = root.getHeight()
                + root.getPaddingTop() + root.getPaddingBottom()
                + root.getBorderTopWidth() + root.getBorderBottomWidth();

        if (totalHeight <= pageContentHeight) {
            // Everything fits on one page
            pages.add(new PageSlice(0, totalHeight));
            return pages;
        }

        // Find break points
        List<Float> breakPoints = new ArrayList<>();
        breakPoints.add(0f);
        collectBreakPoints(root, 0, breakPoints);

        // Build pages using break points
        float currentPageStart = 0;

        for (float bp : breakPoints) {
            if (bp - currentPageStart > pageContentHeight) {
                // Need to break before this point
                pages.add(new PageSlice(currentPageStart, currentPageStart + pageContentHeight));
                currentPageStart = currentPageStart + pageContentHeight;
                // Might need to re-check if current bp is still over
                while (bp - currentPageStart > pageContentHeight) {
                    pages.add(new PageSlice(currentPageStart, currentPageStart + pageContentHeight));
                    currentPageStart += pageContentHeight;
                }
            }
        }

        // Final page
        if (currentPageStart < totalHeight) {
            pages.add(new PageSlice(currentPageStart, totalHeight));
        }

        if (pages.isEmpty()) {
            pages.add(new PageSlice(0, totalHeight));
        }

        return pages;
    }

    /**
     * Collects potential break points (Y positions where page breaks can occur).
     * Breaks are preferred at block/row boundaries.
     */
    private void collectBreakPoints(Box box, float offsetY, List<Float> breakPoints) {
        float boxAbsY = offsetY + box.getY() + box.getMarginTop();
        float contentStartY = boxAbsY + box.getBorderTopWidth() + box.getPaddingTop();

        for (Box child : box.getEffectiveChildren()) {
            if (child instanceof BlockBox || child instanceof TableRowBox
                    || child instanceof TableRowGroupBox) {
                float childAbsY = contentStartY + child.getY() + child.getMarginTop();
                breakPoints.add(childAbsY);
                collectBreakPoints(child, contentStartY, breakPoints);
            } else if (child instanceof TableBox) {
                // Can break between rows within a table
                float childAbsY = contentStartY + child.getY() + child.getMarginTop();
                breakPoints.add(childAbsY);
                collectBreakPoints(child, contentStartY, breakPoints);
            }
        }
    }
}
