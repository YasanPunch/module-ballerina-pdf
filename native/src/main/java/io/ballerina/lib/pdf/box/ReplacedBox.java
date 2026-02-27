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

/**
 * A replaced element box (img with base64 data URL).
 * Stores the data URL source and intrinsic dimensions.
 */
public class ReplacedBox extends Box {

    private String src; // data:image/png;base64,...
    private float intrinsicWidth;
    private float intrinsicHeight;

    public ReplacedBox(ComputedStyle style) {
        super(style);
    }

    public String getSrc() { return src; }
    public void setSrc(String src) { this.src = src; }

    public float getIntrinsicWidth() { return intrinsicWidth; }
    public void setIntrinsicWidth(float w) { this.intrinsicWidth = w; }

    public float getIntrinsicHeight() { return intrinsicHeight; }
    public void setIntrinsicHeight(float h) { this.intrinsicHeight = h; }

    @Override
    public String getBoxType() {
        return "replaced";
    }
}
