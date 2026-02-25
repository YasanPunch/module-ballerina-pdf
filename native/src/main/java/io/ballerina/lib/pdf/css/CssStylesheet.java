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

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed stylesheet: collection of rules + @page declarations.
 */
public class CssStylesheet {

    private final List<CssRule> rules = new ArrayList<>();
    private final List<CssDeclaration> pageDeclarations = new ArrayList<>();

    public void addRule(CssRule rule) {
        rules.add(rule);
    }

    public void addPageDeclaration(CssDeclaration decl) {
        pageDeclarations.add(decl);
    }

    public List<CssRule> getRules() {
        return rules;
    }

    public List<CssDeclaration> getPageDeclarations() {
        return pageDeclarations;
    }

    /**
     * Gets the @page size (returns "a4" by default).
     */
    public String getPageSize() {
        for (CssDeclaration decl : pageDeclarations) {
            if (decl.property().equals("size")) {
                return decl.value();
            }
        }
        return "a4";
    }

    /**
     * Gets the @page margin values as [top, right, bottom, left] in the raw CSS value.
     */
    public String getPageMargin() {
        for (CssDeclaration decl : pageDeclarations) {
            if (decl.property().equals("margin")) {
                return decl.value();
            }
        }
        return "15mm 10mm";
    }
}
