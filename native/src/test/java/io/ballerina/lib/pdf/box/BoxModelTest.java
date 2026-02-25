/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf.box;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxModelTest {

    private BlockBox newBox() {
        return new BlockBox(null);
    }

    @Test
    void outerWidthIncludesAllBoxModel() {
        BlockBox box = newBox();
        box.setMarginLeft(5);
        box.setMarginRight(5);
        box.setBorderLeftWidth(2);
        box.setBorderRightWidth(2);
        box.setPaddingLeft(3);
        box.setPaddingRight(3);
        box.setWidth(100);
        // 5 + 2 + 3 + 100 + 3 + 2 + 5 = 120
        assertEquals(120f, box.getOuterWidth(), 0.01f);
    }

    @Test
    void outerHeightIncludesAllBoxModel() {
        BlockBox box = newBox();
        box.setMarginTop(5);
        box.setMarginBottom(5);
        box.setBorderTopWidth(2);
        box.setBorderBottomWidth(2);
        box.setPaddingTop(3);
        box.setPaddingBottom(3);
        box.setHeight(100);
        // 5 + 2 + 3 + 100 + 3 + 2 + 5 = 120
        assertEquals(120f, box.getOuterHeight(), 0.01f);
    }

    @Test
    void borderBoxWidthExcludesMargin() {
        BlockBox box = newBox();
        box.setMarginLeft(10);
        box.setMarginRight(10);
        box.setBorderLeftWidth(2);
        box.setBorderRightWidth(2);
        box.setPaddingLeft(3);
        box.setPaddingRight(3);
        box.setWidth(100);
        // 2 + 3 + 100 + 3 + 2 = 110 (no margin)
        assertEquals(110f, box.getBorderBoxWidth(), 0.01f);
    }

    @Test
    void absoluteXTraversesParentChain() {
        BlockBox parent = newBox();
        parent.setX(10);
        parent.setBorderLeftWidth(2);
        parent.setPaddingLeft(3);

        BlockBox child = newBox();
        child.setX(5);
        parent.addChild(child);

        // child absolute X = child.x + child.marginLeft + parent.getContentX()
        // parent.getContentX() = parent.getAbsoluteX() + parent.borderLeft + parent.paddingLeft
        // parent.getAbsoluteX() = parent.x + parent.marginLeft = 10 + 0 = 10
        // parent.getContentX() = 10 + 2 + 3 = 15
        // child.getAbsoluteX() = 5 + 0 + 15 = 20
        assertEquals(20f, child.getAbsoluteX(), 0.01f);
    }

    @Test
    void absoluteYTraversesParentChain() {
        BlockBox parent = newBox();
        parent.setY(10);
        parent.setBorderTopWidth(2);
        parent.setPaddingTop(3);

        BlockBox child = newBox();
        child.setY(5);
        parent.addChild(child);

        // parent.getAbsoluteY() = 10 + 0 = 10
        // parent.getContentY() = 10 + 2 + 3 = 15
        // child.getAbsoluteY() = 5 + 0 + 15 = 20
        assertEquals(20f, child.getAbsoluteY(), 0.01f);
    }

    @Test
    void contentXAccountsForBorderAndPadding() {
        BlockBox box = newBox();
        box.setX(10);
        box.setBorderLeftWidth(2);
        box.setPaddingLeft(3);
        // absoluteX = 10 + 0 = 10
        // contentX = 10 + 2 + 3 = 15
        assertEquals(15f, box.getContentX(), 0.01f);
    }

    @Test
    void defaultDimensionsAreZero() {
        BlockBox box = newBox();
        assertEquals(0, box.getX());
        assertEquals(0, box.getY());
        assertEquals(0, box.getWidth());
        assertEquals(0, box.getHeight());
        assertEquals(0, box.getMarginTop());
        assertEquals(0, box.getPaddingLeft());
        assertEquals(0, box.getBorderTopWidth());
    }

    @Test
    void addChildSetsParent() {
        BlockBox parent = newBox();
        BlockBox child = newBox();
        parent.addChild(child);
        assertSame(parent, child.getParent());
    }
}
