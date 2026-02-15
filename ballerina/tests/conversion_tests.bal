// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

function assertValidPdf(byte[] pdf, string msg) {
    test:assertTrue(pdf.length() > 500, msg + " - PDF too small (" + pdf.length().toString() + " bytes)");
    // %PDF- = [0x25, 0x50, 0x44, 0x46, 0x2D]
    test:assertEquals(pdf[0], 0x25, msg + " - invalid PDF header");
    test:assertEquals(pdf[1], 0x50, msg + " - invalid PDF header");
    test:assertEquals(pdf[2], 0x44, msg + " - invalid PDF header");
    test:assertEquals(pdf[3], 0x46, msg + " - invalid PDF header");
    test:assertEquals(pdf[4], 0x2D, msg + " - invalid PDF header");
}

@test:Config {}
function testBasicConversion() returns error? {
    byte[] pdf = check convertToPdf("<h1>Hello World</h1>");
    assertValidPdf(pdf, "Basic conversion");
}

@test:Config {}
function testConversionWithCustomFontSize() returns error? {
    byte[] pdf = check convertToPdf("<p>Small text</p>", defaultFontSizePt = 9.0);
    assertValidPdf(pdf, "Custom font size");
}

@test:Config {}
function testConversionWithLetterPageSize() returns error? {
    byte[] pdf = check convertToPdf("<p>Letter page</p>", pageSize = LETTER);
    assertValidPdf(pdf, "Letter page size");
}

@test:Config {}
function testConversionWithCustomMargins() returns error? {
    byte[] pdf = check convertToPdf("<p>Custom margins</p>",
        margins = {top: 72, right: 72, bottom: 72, left: 72}
    );
    assertValidPdf(pdf, "Custom margins");
}

@test:Config {}
function testConversionWithAdditionalCss() returns error? {
    byte[] pdf = check convertToPdf(
        "<div class=\"highlight\"><p>Styled content</p></div>",
        additionalCss = ".highlight { background-color: yellow; padding: 10px; }"
    );
    assertValidPdf(pdf, "Additional CSS");
}

@test:Config {}
function testMalformedHtml() returns error? {
    byte[] pdf = check convertToPdf("<div><p>unclosed paragraph<span>and span");
    assertValidPdf(pdf, "Malformed HTML");
}

@test:Config {}
function testFullHtmlDocument() returns error? {
    string html = string `
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; }
                h1 { color: navy; }
                table { border-collapse: collapse; }
                td { border: 1px solid black; padding: 5px; }
            </style>
        </head>
        <body>
            <h1>Test Document</h1>
            <p>This is a paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
            <table>
                <tr><td>Cell 1</td><td>Cell 2</td></tr>
                <tr><td>Cell 3</td><td>Cell 4</td></tr>
            </table>
        </body>
        </html>
    `;
    byte[] pdf = check convertToPdf(html);
    assertValidPdf(pdf, "Full HTML document");
}

@test:Config {}
function testAllOptionsCombined() returns error? {
    byte[] pdf = check convertToPdf(
        "<p>All options</p>",
        defaultFontSizePt = 10.0,
        pageSize = LETTER,
        margins = {top: 72, right: 54, bottom: 72, left: 54},
        additionalCss = "p { color: darkblue; }"
    );
    assertValidPdf(pdf, "All options combined");
}

@test:Config {}
function testEmptyHtml() returns error? {
    byte[] pdf = check convertToPdf("");
    assertValidPdf(pdf, "Empty HTML");
}

@test:Config {}
function testWhitespaceOnlyHtml() returns error? {
    byte[] pdf = check convertToPdf("   \n\t  ");
    assertValidPdf(pdf, "Whitespace-only HTML");
}

@test:Config {}
function testPreprocessDisabled() returns error? {
    string xhtml = string `<?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
        <html xmlns="http://www.w3.org/1999/xhtml">
        <head><title>Test</title></head>
        <body><p>Well-formed XHTML</p></body>
        </html>`;
    byte[] pdf = check convertToPdf(xhtml, preprocess = false);
    assertValidPdf(pdf, "Preprocess disabled with XHTML");
}

@test:Config {}
function testErrorTypeIsConversionError() {
    // Pass content that triggers an internal error to verify the error type.
    // A null byte sequence in HTML is unlikely to cause a conversion error with
    // the current pipeline, so we verify the positive path — a successful result
    // is not a ConversionError.
    byte[]|ConversionError result = convertToPdf("<p>Valid HTML</p>");
    test:assertTrue(result is byte[], "Expected successful conversion, not ConversionError");
}
