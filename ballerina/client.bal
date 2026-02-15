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

# Converts HTML content to PDF bytes.
#
# + html - The HTML string to convert (can be a full document or fragment)
# + options - Conversion options (all optional with defaults)
# + return - PDF file content as a byte array, or an error
public isolated function convertToPdf(string html, *ConversionOptions options)
    returns byte[]|Error = @java:Method {
    'class: "io.ballerina.lib.pdf.Native"
} external;
