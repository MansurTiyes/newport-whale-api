package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.ingest.HtmlFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class HtmlFetcherTest {

    private final HtmlFetcher fetcher = new HtmlFetcher();

    // testing what JSOUP reads from table rows
    // 1) Provide whale count html actual page
    // 2) test that document isn't empty (has a title)
    // 3) copy selectRecentCounts to obtain table from document
    // 4) copy HtmlParser parse code that takes each row and just print out
    @Test
    void rowsOutputLoggingProperStrings() {

        Document doc = null;
        try {
            doc = fetcher.fetch(URI.create("https://newportwhales.com/whalecount.html"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            ;
        }

        assertThat(doc.title()).isNotEmpty();

        Element recentCountsTable =
                doc.selectFirst("h3:containsOwn(Recent Counts) + table.data-table");

        for (Element row : recentCountsTable.select("tbody > tr:has(td)")) {
            Elements tds = row.select("td");

            System.out.println(tds.get(0).text());
            System.out.println(tds.get(1).text());
            System.out.println(tds.get(2).text());
            System.out.println("----------------------");
        }
    }


}
