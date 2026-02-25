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

import io.ballerina.lib.pdf.css.ComputedStyle;
import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * A text fragment with associated font metrics.
 * Leaf node in the box tree — has no children.
 */
public class TextRun extends Box {

    private String text;
    private PDFont font;
    private float fontSize;
    private float textWidth;
    private boolean isSuperscript;
    private boolean isSubscript;

    public TextRun(ComputedStyle style, String text) {
        super(style);
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public PDFont getFont() { return font; }
    public void setFont(PDFont font) { this.font = font; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public float getTextWidth() { return textWidth; }
    public void setTextWidth(float textWidth) { this.textWidth = textWidth; }

    public boolean isSuperscript() { return isSuperscript; }
    public void setSuperscript(boolean superscript) { isSuperscript = superscript; }

    public boolean isSubscript() { return isSubscript; }
    public void setSubscript(boolean subscript) { isSubscript = subscript; }

    @Override
    public String getBoxType() {
        return "text";
    }
}
