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
# + defaultFontSizePt - Base font size in points. CSS "medium" maps to this value. Default: 12.0 (CSS spec).
# + pageSize - Page size for the PDF output. Default: A4.
# + margins - Page margins in points (top, right, bottom, left). Default: 36pt (0.5 inch) on all sides.
# + preprocess - Whether to run HTML preprocessing (Jsoup cleanup, CSS injection).
#                Disable only if input is already well-formed XHTML. Default: true.
# + additionalCss - Additional CSS to inject into the document before conversion.
#                   Use this for consumer-specific style overrides without modifying the HTML.
# + customFonts - Custom fonts to register for the conversion. Each entry maps a font name to TTF
#                 file content as bytes. The font name is used for CSS font-family matching.
#                 To register weight/style variants, use suffixes: "MyFont Bold", "MyFont Italic",
#                 "MyFont BoldItalic". CSS then references just `font-family: MyFont`.
public type ConversionOptions record {|
    float defaultFontSizePt = 12.0;
    PageSize pageSize = A4;
    PageMargins margins = {top: 36, right: 36, bottom: 36, left: 36};
    boolean preprocess = true;
    string? additionalCss = ();
    map<byte[]>? customFonts = ();
|};
