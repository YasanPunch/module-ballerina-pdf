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

# Base error type for all PDF module errors.
public type Error distinct error;

# Error during HTML parsing or preprocessing.
# Returned when the input HTML cannot be parsed into a valid DOM.
public type HtmlParseError distinct Error;

# Error during the PDF rendering pipeline (layout, painting, or PDF generation).
public type RenderError distinct Error;

# Error during PDF reading operations (text extraction, image conversion).
# Returned when the input PDF is corrupted, inaccessible, or invalid.
public type ReadError distinct Error;
