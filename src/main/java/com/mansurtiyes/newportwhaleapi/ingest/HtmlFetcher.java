package com.mansurtiyes.newportwhaleapi.ingest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Component
public class HtmlFetcher {

    private static final List<String> USER_AGENTS = List.of(
            // rotate a couple of realistic UAs
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15"
    );

    public Document fetch(URI uri) throws IOException {
        String ua = USER_AGENTS.get((int) (System.nanoTime() % USER_AGENTS.size()));
        return Jsoup.connect(uri.toString())
                .userAgent(ua)
                .referrer("https://www.google.com/")                   // looks like a normal nav
                .timeout(10_000)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .followRedirects(true)
                .ignoreHttpErrors(false)                                // throw on 4xx/5xx
                .get();
    }
}
