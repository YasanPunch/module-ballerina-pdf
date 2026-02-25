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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HtmlPreprocessorTest {

    private HtmlPreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new HtmlPreprocessor();
    }

    @Test
    void removesTableWidthAttributes() {
        String html = "<html><body><table width=\"1000\"><tr><td>Cell</td></tr></table></body></html>";
        String result = preprocessor.preprocessToString(html);
        assertFalse(result.contains("width=\"1000\""), "Hard-coded table width should be removed");
    }

    @Test
    void outputsValidXhtml() throws Exception {
        String html = "<html><body><div /><br><p>Test<p>Another</body></html>";
        String result = preprocessor.preprocessToString(html);

        // Valid XML should parse without exceptions
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        assertDoesNotThrow(() ->
                builder.parse(new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8))),
                "Preprocessed output should be valid XML"
        );
    }

    @Test
    void producesW3cDomDocument() {
        String html = "<html><body><p>Hello</p></body></html>";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        assertNotNull(doc, "W3C DOM document should not be null");
        assertNotNull(doc.getDocumentElement(), "Document should have a root element");
    }

    @Test
    void handlesEmptyInput() {
        String html = "";
        org.w3c.dom.Document doc = preprocessor.preprocess(html);
        assertNotNull(doc, "Should handle empty input without exceptions");
    }
}
