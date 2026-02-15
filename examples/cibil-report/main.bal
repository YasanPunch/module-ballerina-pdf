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

// Converts a CIBIL credit report HTML file to PDF with consumer-specific
// CSS overrides and custom page options.
//
// Usage:
//   cd examples/cibil-report
//   bal run
//
// Output: resources/cibil_output.pdf
// Compare against: resources/49511893_Cons_CIR_NAMRATA.html (open in browser)

import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("resources/49511893_Cons_CIR_NAMRATA.html");

    // CSS overrides to fit the report layout to the PDF page width
    string css = string `.maincontainer { width: 100% !important; max-width: 100% !important; }
.head1, .headtitle1 { width: auto !important; }`;

    byte[] pdfBytes = check pdf:convertToPdf(html,
        defaultFontSizePt = 9.0,
        margins = {
            top: 42.52,    // 15mm
            right: 28.35,  // 10mm
            bottom: 42.52,
            left: 28.35
        },
        additionalCss = css
    );

    check io:fileWriteBytes("resources/cibil_output.pdf", pdfBytes);
    io:println("Wrote cibil_output.pdf (" + pdfBytes.length().toString() + " bytes)");
}
