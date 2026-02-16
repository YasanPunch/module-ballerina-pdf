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

import ballerina/io;
import ballerina/test;

// --- Text extraction tests ---

@test:Config {}
function testExtractText() returns error? {
    byte[] pdf = check convertToPdf("<p>Hello World</p>");
    string[] pages = check extractText(pdf);
    test:assertTrue(pages.length() > 0, "Expected at least one page");
    test:assertTrue(pages[0].includes("Hello World"), "Expected text content");
}

@test:Config {}
function testExtractTextFromFile() returns error? {
    byte[] pdf = check convertToPdf("<p>File Test</p>");
    check io:fileWriteBytes("tests/resources/test_output.pdf", pdf);
    string[] pages = check extractTextFromFile("tests/resources/test_output.pdf");
    test:assertTrue(pages.length() > 0, "Expected at least one page");
    test:assertTrue(pages[0].includes("File Test"), "Expected text content");
}

@test:Config {}
function testExtractTextMultiplePages() returns error? {
    // Generate a PDF with enough content to potentially span pages
    string html = "<p>Page content for text extraction test</p>";
    byte[] pdf = check convertToPdf(html);
    string[] pages = check extractText(pdf);
    test:assertTrue(pages.length() >= 1, "Expected at least one page of text");
}

// --- Image conversion tests ---

@test:Config {}
function testToImages() returns error? {
    byte[] pdf = check convertToPdf("<h1>Image Test</h1>");
    string[] images = check toImages(pdf);
    test:assertTrue(images.length() > 0, "Expected at least one image");
    test:assertTrue(images[0].length() > 100, "Expected non-trivial base64 image");
}

@test:Config {}
function testToImagesFromFile() returns error? {
    byte[] pdf = check convertToPdf("<p>File Image Test</p>");
    check io:fileWriteBytes("tests/resources/test_images.pdf", pdf);
    string[] images = check toImagesFromFile("tests/resources/test_images.pdf");
    test:assertTrue(images.length() > 0, "Expected at least one image");
    test:assertTrue(images[0].length() > 100, "Expected non-trivial base64 image");
}

// --- Error tests ---

@test:Config {}
function testExtractTextInvalidFile() {
    string[]|Error result = extractTextFromFile("/nonexistent/file.pdf");
    test:assertTrue(result is ReadError, "Expected ReadError for invalid file");
}

@test:Config {}
function testExtractTextInvalidBytes() {
    string[]|Error result = extractText("not a pdf".toBytes());
    test:assertTrue(result is ReadError, "Expected ReadError for invalid bytes");
}

@test:Config {}
function testToImagesInvalidBytes() {
    string[]|Error result = toImages("not a pdf".toBytes());
    test:assertTrue(result is ReadError, "Expected ReadError for invalid bytes");
}

@test:Config {}
function testToImagesInvalidFile() {
    string[]|Error result = toImagesFromFile("/nonexistent/file.pdf");
    test:assertTrue(result is ReadError, "Expected ReadError for invalid file");
}

// --- Round-trip tests ---

@test:Config {}
function testRoundTripTextExtraction() returns error? {
    string inputText = "Round trip verification content";
    byte[] pdf = check convertToPdf("<p>" + inputText + "</p>");
    string[] pages = check extractText(pdf);
    test:assertTrue(pages.length() > 0, "Expected at least one page");
    test:assertTrue(pages[0].includes(inputText), "Extracted text should contain input");
}
