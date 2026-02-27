/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dev/test entry point for manual HTML-to-PDF conversion.
 */
public class Main {

    private static final String[] DEFAULT_INPUTS = {
            "src/test/resources/smoke-test.html",
            "src/test/resources/49511893_Cons_CIR_NAMRATA.html"
    };

    public static void main(String[] args) throws Exception {
        String[] inputs = args.length > 0 ? args : DEFAULT_INPUTS;

        HtmlPreprocessor preprocessor = new HtmlPreprocessor();
        HtmlToPdfConverter converter = new HtmlToPdfConverter();
        ConverterOptions options = new ConverterOptions();

        for (String inputPath : inputs) {
            convertFile(inputPath, preprocessor, converter, options);
        }
    }

    private static void convertFile(String inputPath, HtmlPreprocessor preprocessor,
                                     HtmlToPdfConverter converter,
                                     ConverterOptions options) throws Exception {
        Path input = Path.of(inputPath);
        if (!Files.exists(input)) {
            System.err.println("Not found: " + input.toAbsolutePath());
            return;
        }

        // Derive output path: output/<name>.pdf
        String inputName = input.getFileName().toString();
        String pdfName = inputName.replaceFirst("\\.[^.]+$", "") + ".pdf";
        Path outputPath = Path.of("output", pdfName);

        System.out.println("Converting: " + inputPath);

        // Read and parse HTML
        String rawHtml = Files.readString(input, StandardCharsets.UTF_8);
        org.w3c.dom.Document w3cDoc = preprocessor.preprocess(rawHtml);

        // Dump cleaned XHTML for debugging
        String debugName = inputName.replaceFirst("\\.[^.]+$", "") + "-debug.xhtml";
        Path debugPath = Path.of("output", debugName);
        Files.createDirectories(debugPath.getParent());
        Files.writeString(debugPath, preprocessor.preprocessToString(rawHtml), StandardCharsets.UTF_8);

        // Convert to PDF
        byte[] pdfBytes = converter.convert(w3cDoc, options);

        // Write output
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, pdfBytes);

        System.out.printf("  → %s (%,d bytes)%n", outputPath, pdfBytes.length);
    }
}
