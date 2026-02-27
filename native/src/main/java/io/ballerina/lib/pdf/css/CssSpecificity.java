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

package io.ballerina.lib.pdf.css;

/**
 * CSS specificity tuple: (inline, ids, classes, tags).
 * Higher specificity wins in the cascade.
 */
public record CssSpecificity(int inline, int ids, int classes, int tags) implements Comparable<CssSpecificity> {

    public static final CssSpecificity ZERO = new CssSpecificity(0, 0, 0, 0);
    public static final CssSpecificity INLINE = new CssSpecificity(1, 0, 0, 0);

    @Override
    public int compareTo(CssSpecificity other) {
        if (this.inline != other.inline) return Integer.compare(this.inline, other.inline);
        if (this.ids != other.ids) return Integer.compare(this.ids, other.ids);
        if (this.classes != other.classes) return Integer.compare(this.classes, other.classes);
        return Integer.compare(this.tags, other.tags);
    }
}
