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

# Supported page sizes for PDF output.
public enum PageSize {
    A4,
    LETTER,
    LEGAL
}

# Page margins in points (1 point = 1/72 inch). 
# This is the default margin for the PDF output.
#
# + top - Top margin
# + right - Right margin
# + bottom - Bottom margin
# + left - Left margin
public type PageMargins record {|
    float top;
    float right;
    float bottom;
    float left;
|};

# Options controlling HTML-to-PDF conversion behavior.
#
# + fontSizePt - Base font size in points (default: 12.0, per CSS spec "medium" keyword).
#                Used when CSS does not specify a font-size for an element. CSS `font-size`
#                declarations in the HTML take precedence over this value.
# + pageSize - Page size for the PDF output. Default: A4.
# + margins - Page margins in points (top, right, bottom, left). Default: 0 (no page margin; CSS controls spacing).
# + additionalCss - Additional CSS to inject into the document before conversion.
#                   Use this for consumer-specific style overrides without modifying the HTML.
# + customFonts - Custom fonts to register for the conversion. Each entry maps a font name to TTF
#                 file content as bytes. The font name is used for CSS font-family matching.
#                 To register weight/style variants, use suffixes: "MyFont Bold", "MyFont Italic",
#                 "MyFont BoldItalic". CSS then references just `font-family: MyFont`.
# + maxPages - Maximum number of pages in the output PDF. Content is scaled to fit within
#              this page limit. Must be greater than 0 when provided.
public type ConversionOptions record {|
    float fontSizePt = 12.0;
    PageSize pageSize = A4;
    PageMargins margins = {top: 0, right: 0, bottom: 0, left: 0};
    string? additionalCss = ();
    map<byte[]>? customFonts = ();
    int? maxPages = ();
|};
