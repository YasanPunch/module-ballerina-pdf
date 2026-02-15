# Examples

The `ballerina/pdf` module provides practical examples illustrating usage in various scenarios.

1. [CIBIL Credit Report](./cibil-report/) - Convert a CIBIL credit report HTML file to PDF with custom CSS overrides, font size, and margins.

   Produces `resources/cibil_output.pdf`. Open the input HTML (`resources/49511893_Cons_CIR_NAMRATA.html`) in a browser and compare side-by-side with the PDF to verify layout fidelity.

## Prerequisites

- Ballerina distribution 2201.13.1 or later

## Running examples during development

Examples depend on `ballerina/pdf` being available as a package. Since the module isn't published to Ballerina Central yet, you must use the provided build script to pack and push it locally before running examples.

From the `examples/` directory:

```bash
# Build all examples (packs the module locally first)
./build.sh build

# Build and run all examples
./build.sh run
```

**Warning**: The build script writes the module bala to your local Ballerina central repository as a workaround. This may modify your local Ballerina repositories.

## Running examples after the module is published

Once `ballerina/pdf` is available on Ballerina Central, you can run any example directly:

```bash
cd examples/cibil-report
bal run
```
