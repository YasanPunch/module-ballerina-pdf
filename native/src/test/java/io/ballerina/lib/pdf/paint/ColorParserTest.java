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

package io.ballerina.lib.pdf.paint;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class ColorParserTest {

    @Test
    void parsesSixDigitHex() {
        Color c = ColorParser.parse("#ff0000");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    void parsesThreeDigitHex() {
        Color c = ColorParser.parse("#f00");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
    }

    @Test
    void parsesRgb() {
        Color c = ColorParser.parse("rgb(0, 128, 255)");
        assertNotNull(c);
        assertEquals(0, c.getRed());
        assertEquals(128, c.getGreen());
        assertEquals(255, c.getBlue());
    }

    @Test
    void parsesRgba() {
        Color c = ColorParser.parse("rgba(255, 0, 0, 0.5)");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(0, c.getBlue());
        assertEquals(127, c.getAlpha());
    }

    @Test
    void parsesNamedColors() {
        assertEquals(Color.BLACK, ColorParser.parse("black"));
        assertEquals(Color.WHITE, ColorParser.parse("white"));

        Color navy = ColorParser.parse("navy");
        assertNotNull(navy);
        assertEquals(0, navy.getRed());
        assertEquals(0, navy.getGreen());
        assertEquals(128, navy.getBlue());

        Color transparent = ColorParser.parse("transparent");
        assertNotNull(transparent);
        assertEquals(0, transparent.getAlpha());
    }

    @Test
    void isCaseInsensitive() {
        Color upper = ColorParser.parse("#FF0000");
        assertNotNull(upper);
        assertEquals(255, upper.getRed());

        Color rgbUpper = ColorParser.parse("RGB(1,2,3)");
        assertNotNull(rgbUpper);
        assertEquals(1, rgbUpper.getRed());
    }

    @Test
    void returnsNullForInvalid() {
        assertNull(ColorParser.parse("notacolor"));
        assertNull(ColorParser.parse("#xyz"));
    }

    @Test
    void returnsNullForNullOrBlank() {
        assertNull(ColorParser.parse(null));
        assertNull(ColorParser.parse(""));
        assertNull(ColorParser.parse("   "));
    }

    @Test
    void clampsOutOfRangeValues() {
        // Regex only matches non-negative integers, so test with 300 > 255
        Color c = ColorParser.parse("rgb(300, 0, 128)");
        assertNotNull(c);
        assertEquals(255, c.getRed());
        assertEquals(0, c.getGreen());
        assertEquals(128, c.getBlue());
    }
}
