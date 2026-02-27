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

import java.util.ArrayList;
import java.util.List;

/**
 * A table box. Contains TableRowGroupBox or TableRowBox children.
 * Stores column width percentages from &lt;colgroup&gt;.
 */
public class TableBox extends Box {

    private final List<Float> columnWidths = new ArrayList<>(); // percentages
    private boolean borderCollapse;

    public TableBox(ComputedStyle style) {
        super(style);
    }

    public List<Float> getColumnWidths() { return columnWidths; }

    public void addColumnWidth(float pct) {
        columnWidths.add(pct);
    }

    public boolean isBorderCollapse() { return borderCollapse; }
    public void setBorderCollapse(boolean val) { this.borderCollapse = val; }

    @Override
    public String getBoxType() {
        return "table";
    }
}
