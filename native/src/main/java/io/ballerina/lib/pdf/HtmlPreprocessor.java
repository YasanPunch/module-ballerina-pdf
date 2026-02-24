package io.ballerina.lib.pdf;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;

/**
 * Parses raw (potentially messy) HTML into a clean W3C DOM Document
 * suitable for the rendering pipeline.
 * <p>
 * Jsoup handles lenient parsing (self-closing divs, unclosed tags, encoding issues).
 * The only HTML-level fix applied is stripping hardcoded table width attributes,
 * which cause table overflow in fixed-width PDF output.
 */
public class HtmlPreprocessor {

    /**
     * Parses raw HTML and returns a W3C DOM Document for the rendering pipeline.
     */
    public org.w3c.dom.Document preprocess(String rawHtml) {
        // Parse with Jsoup (lenient — handles self-closing divs, unclosed tags)
        Document doc = Jsoup.parse(rawHtml);

        // Configure XHTML output
        doc.outputSettings()
                .syntax(OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");

        // Strip hardcoded table width attributes to prevent table overflow
        for (Element table : doc.select("table")) {
            table.removeAttr("width");
        }

        // Convert to W3C DOM
        W3CDom w3cDom = new W3CDom();
        return w3cDom.fromJsoup(doc);
    }

    /**
     * Returns the cleaned XHTML as a string.
     * Useful for debugging — write the output to disk for inspection.
     */
    public String preprocessToString(String rawHtml) {
        Document doc = Jsoup.parse(rawHtml);
        doc.outputSettings()
                .syntax(OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8");

        for (Element table : doc.select("table")) {
            table.removeAttr("width");
        }

        return doc.html();
    }
}
