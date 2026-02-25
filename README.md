# Ballerina PDF Module

[![Build](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/ci.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/actions/workflows/ci.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-pdf.svg)](https://github.com/ballerina-platform/module-ballerina-pdf/commits/main)
[![GitHub Issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-library/module/pdf.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-library/labels/module%2Fpdf)

## Overview

The `ballerina/pdf` module provides functionality for working with PDF documents in Ballerina. It supports:

- **HTML-to-PDF conversion** — Convert HTML content to PDF with configurable page sizes, margins, fonts, and CSS styling
- **Text extraction** — Extract text content from existing PDF documents (from bytes, files, or URLs)
- **Image export** — Convert PDF pages to Base64-encoded PNG images

All processing is performed locally — no external cloud services are used.

## Quickstart

### Convert HTML to PDF

```ballerina
import ballerina/pdf;
import ballerina/io;

public function main() returns error? {
    byte[] pdfBytes = check pdf:parseHtml(string `
        <html>
        <body>
            <h1>Hello, World!</h1>
            <p>This is a PDF generated from HTML.</p>
        </body>
        </html>
    `);
    check io:fileWriteBytes("output.pdf", pdfBytes);
}
```

### Extract text from a PDF

```ballerina
import ballerina/pdf;
import ballerina/io;

public function main() returns error? {
    string[] pages = check pdf:extractTextFromFile("document.pdf");
    foreach string page in pages {
        io:println(page);
    }
}
```

## Examples

The `ballerina/pdf` module provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/).

## Build from the source

### Setting up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 21. You can download it from either of the following sources:

    * [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
    * [OpenJDK](https://adoptium.net/)

   > **Note:** After installation, remember to set the `JAVA_HOME` environment variable to the directory where JDK was installed.

2. Download and install [Ballerina Swan Lake](https://ballerina.io/).

3. Download and install [Docker](https://www.docker.com/get-started).

   > **Note**: Ensure that the Docker daemon is running before executing any tests.

4. Export Github Personal access token with read package permissions as follows,

    ```bash
    export packageUser=<Username>
    export packagePAT=<Personal access token>
    ```

### Build options

Execute the commands below to build from the source.

1. To build the package:

   ```bash
   ./gradlew clean build
   ```

2. To run the tests:

   ```bash
   ./gradlew clean test
   ```

3. To build the without the tests:

   ```bash
   ./gradlew clean build -x test
   ```

4. To run tests against different environments:

   ```bash
   ./gradlew clean test -Pgroups=<Comma separated groups/test cases>
   ```

5. To debug the package with a remote debugger:

   ```bash
   ./gradlew clean build -Pdebug=<port>
   ```

6. To debug with the Ballerina language:

   ```bash
   ./gradlew clean build -PbalJavaDebug=<port>
   ```

7. Publish the generated artifacts to the local Ballerina Central repository:

    ```bash
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina Central repository:

   ```bash
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open-source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All the contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`pdf` package](https://central.ballerina.io/ballerina/pdf/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
