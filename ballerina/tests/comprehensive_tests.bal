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
import ballerina/lang.regexp;
import ballerina/test;

const string COMPREHENSIVE_HTML_DIR = "../examples/comprehensive-html-tests/";

// Converts an HTML file, validates PDF output, and verifies expected text content.
function convertAndVerifyFile(string filename, int minPages, string[] expectedContent,
        int maxPages = 0) returns error? {
    string html = check io:fileReadString(COMPREHENSIVE_HTML_DIR + filename);

    byte[] pdf;
    if maxPages > 0 {
        pdf = check convertToPdf(html, maxPages = maxPages);
    } else {
        pdf = check convertToPdf(html);
    }

    assertValidPdf(pdf, filename);

    string[] pages = check extractText(pdf);
    test:assertTrue(pages.length() >= minPages,
        filename + ": Expected at least " + minPages.toString() + " page(s), got " + pages.length().toString());

    if maxPages > 0 {
        test:assertEquals(pages.length(), maxPages,
            filename + ": maxPages=" + maxPages.toString() + " but got " + pages.length().toString());
    }

    string text = regexp:replaceAll(re `\s+`, joinPages(pages), " ").toLowerAscii();
    foreach string expected in expectedContent {
        test:assertTrue(text.includes(expected.toLowerAscii()),
            filename + ": Missing expected content '" + expected + "'");
    }
}

// --- Comprehensive HTML document tests ---

@test:Config {}
function testComprehensiveBlogArticle() returns error? {
    check convertAndVerifyFile("blog-article.html", 1, [
        "The Future of Distributed Systems",
        "Dr. Sarah Chen",
        "Consensus algorithms",
        "CAP theorem",
        "Observability",
        "Leslie Lamport"
    ]);
}

@test:Config {}
function testComprehensiveFinancialReport() returns error? {
    check convertAndVerifyFile("financial-report.html", 1, [
        "Meridian Holdings",
        "Annual Financial Report",
        "Product Revenue",
        "Service Revenue",
        "North America",
        "Enterprise Cloud Platform"
    ]);
}

@test:Config {}
function testComprehensiveInvoice() returns error? {
    check convertAndVerifyFile("invoice.html", 1, [
        "Nexus Digital Solutions",
        "INV-2024-0847",
        "Stratton Manufacturing",
        "Enterprise Cloud Platform",
        "Payment is due within 30 days",
        "Pending"
    ]);
}

@test:Config {}
function testComprehensiveResume() returns error? {
    check convertAndVerifyFile("resume.html", 1, [
        "Alexandra Petrov",
        "Senior Software Engineer",
        "Apex Cloud Technologies",
        "Master of Science",
        "Massachusetts Institute of Technology"
    ]);
}

@test:Config {}
function testComprehensiveProductCards() returns error? {
    check convertAndVerifyFile("product-cards.html", 1, [
        "TechGear Pro",
        "AeroSound Pro X1",
        "Noise Cancelling Headphones",
        "SonicBuds Ultra",
        "Wireless Audio Comparison"
    ]);
}

@test:Config {}
function testComprehensiveEmailNewsletter() returns error? {
    check convertAndVerifyFile("email-newsletter.html", 1, [
        "CloudPulse",
        "PostgreSQL 17 Performance Deep Dive",
        "PostgreSQL 17",
        "Kubernetes 1.30",
        "Git Worktrees",
        "KubeCon EU 2025"
    ]);
}

@test:Config {}
function testComprehensiveDashboard() returns error? {
    check convertAndVerifyFile("dashboard.html", 1, [
        "SaaS Platform Dashboard",
        "Monthly Performance Overview",
        "Churn Rate",
        "Acme Corporation",
        "System Health"
    ]);
}

@test:Config {}
function testComprehensiveMultiPageReport() returns error? {
    check convertAndVerifyFile("multi-page-report.html", 3, [
        "State of Cloud Infrastructure 2025",
        "Horizon Research Group",
        "platform engineering",
        "Amazon Web Services",
        "Kubernetes"
    ]);
}

@test:Config {}
function testComprehensiveTechnicalDocs() returns error? {
    check convertAndVerifyFile("technical-docs.html", 1, [
        "Ballerina HTTP Client",
        "Type-safe payload binding",
        "Circuit breaker",
        "Retry Configuration"
    ]);
}

@test:Config {}
function testComprehensiveCertificate() returns error? {
    check convertAndVerifyFile("certificate.html", 1, [
        "outstanding performance",
        "Dr. Elena Vasquez",
        "Marcus J. Richardson",
        "Advanced Cloud Architecture",
        "Microservices Architecture"
    ], maxPages = 1);
}
