## Overview

The `ballerina/pdf` module provides functionality to convert HTML content to PDF documents. It processes HTML strings — including full documents, fragments, and messy real-world markup — and produces PDF byte arrays suitable for writing to files or sending over the network.

All processing is done locally with no external service dependencies. The module handles HTML cleanup, CSS injection, and PDF rendering in a single pipeline.

## Quickstart

### Convert HTML to PDF

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check pdf:parseHtml("<h1>Hello World</h1><p>Generated with Ballerina.</p>");
    check io:fileWriteBytes("output.pdf", pdfBytes);
}
```

### Convert with custom options

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("report.html");

    byte[] pdfBytes = check pdf:parseHtml(html,
        fontSizePt = 10.0,
        pageSize = pdf:LETTER,
        margins = {top: 72, right: 54, bottom: 72, left: 54},
        additionalCss = "body { font-family: sans-serif; } .container { width: 100% !important; }"
    );

    check io:fileWriteBytes("report.pdf", pdfBytes);
}
```

## Examples

The `pdf` module provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/).

1. [CIBIL credit report conversion](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/cibil-report/) — Converts a complex, table-heavy CIBIL credit report HTML file to PDF with consumer-specific CSS overrides and custom page options.
