package com.mansurtiyes.newportwhaleapi.ingest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
public class HtmlFetcher {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; WhaleCountScraper/1.0)";

    public Document fetch(URI uri) throws IOException {
        return Jsoup.connect(uri.toString())
                .userAgent(USER_AGENT)      // looks like browser
                .timeout(10_000)         // 10s timeout
                .get();                    // fetch & parse
    }
}
