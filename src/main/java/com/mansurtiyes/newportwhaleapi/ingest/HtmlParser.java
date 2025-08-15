package com.mansurtiyes.newportwhaleapi.ingest;

import org.jsoup.nodes.Document;

import java.util.List;

public interface HtmlParser {
    List<ParsedReport> parse(Document doc, String sourceUrl);
}
