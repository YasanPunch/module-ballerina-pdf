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

package io.ballerina.lib.pdf;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;

/**
 * Parses raw (potentially messy) HTML into a clean W3C DOM Document
 * suitable for the rendering pipeline.
 * <p>
 * Jsoup handles lenient parsing (self-closing divs, unclosed tags, encoding issues).
 * The only HTML-level fix applied is stripping hardcoded table width attributes,
 * which cause table overflow in fixed-width PDF output.
 */
public class HtmlPreprocessor {

    /**
     * Parses raw HTML and returns a W3C DOM Document for the rendering pipeline.
     */
    public org.w3c.dom.Document preprocess(String rawHtml) {
        Document doc = parseAndClean(rawHtml);
        W3CDom w3cDom = new W3CDom();
        return w3cDom.fromJsoup(doc);
    }

    /**
     * Returns the cleaned XHTML as a string.
     * Useful for debugging — write the output to disk for inspection.
     */
    public String preprocessToString(String rawHtml) {
        return parseAndClean(rawHtml).html();
    }

    /**
     * Shared setup: lenient parse, XHTML output settings, table width stripping.
     */
    private Document parseAndClean(String rawHtml) {
        Document doc = Jsoup.parse(rawHtml);
        doc.outputSettings()
                .syntax(OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");

        for (Element table : doc.select("table")) {
            table.removeAttr("width");
        }
        return doc;
    }
}
