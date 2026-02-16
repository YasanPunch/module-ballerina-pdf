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

import ballerina/jballerina.java;

# Converts each page of a PDF document to a Base64-encoded PNG image.
#
# + pdf - The PDF document as a byte array
# + return - An array of Base64-encoded PNG strings (one per page), or an error
public isolated function toImages(byte[] pdf) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;

# Converts each page of a PDF file to a Base64-encoded PNG image.
#
# + filePath - Path to the PDF file
# + return - An array of Base64-encoded PNG strings (one per page), or an error
public isolated function toImagesFromFile(string filePath) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;

# Converts each page of a PDF at the given URL to a Base64-encoded PNG image.
#
# + url - URL pointing to a PDF document
# + return - An array of Base64-encoded PNG strings (one per page), or an error
public isolated function toImagesFromUrl(string url) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;

# Extracts text content from each page of a PDF document.
#
# + pdf - The PDF document as a byte array
# + return - An array of text strings (one per page), or an error
public isolated function extractText(byte[] pdf) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;

# Extracts text content from each page of a PDF file.
#
# + filePath - Path to the PDF file
# + return - An array of text strings (one per page), or an error
public isolated function extractTextFromFile(string filePath) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;

# Extracts text content from each page of a PDF at the given URL.
#
# + url - URL pointing to a PDF document
# + return - An array of text strings (one per page), or an error
public isolated function extractTextFromUrl(string url) returns string[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;
