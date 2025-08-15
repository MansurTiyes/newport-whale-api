package com.mansurtiyes.newportwhaleapi.ingest;

import com.mansurtiyes.newportwhaleapi.ingest.resolve.InMemorySpeciesResolver;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class WhaleCountParser implements HtmlParser {

    private final InMemorySpeciesResolver speciesResolver;

    private static final DateTimeFormatter M_D_YYYY =
            DateTimeFormatter.ofPattern("M/d/yyyy");

    public WhaleCountParser(InMemorySpeciesResolver speciesResolver) {
        this.speciesResolver = speciesResolver;
    }

    @Override
    public List<ParsedReport> parse(Document doc, String sourceUrl) {
        return List.of();
    }

    // given a Document, find and returns a table element from Newport whale count page
    private Element selectRecentCountsTable(Document doc) {
        Element byHeader = doc.selectFirst("h3:containsOwn(Recent Counts) + table.data-table");
        if (byHeader != null) return byHeader;

        // Fallback by unique THs for this table
        return doc.selectFirst(
                "table.data-table" +
                        ":has(th:matchesOwn(?i)^\\s*DATE\\s*$))" +
                        ":has(th:matchesOwn(?i)^\\s*TOURS\\s*$))" +
                        ":has(th:matchesOwn(?i)MAMMALS\\s+VIEWED)"
        );
    }

    public LocalDate parseDate(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Date string is null or blank");
        }
        try {
            return LocalDate.parse(input.trim(), M_D_YYYY);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unable to parse date: " + input, e);
        }
    }

    public Integer parseTours(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Tours string is null or blank");
        }
        try {
            return Integer.valueOf(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse tours count: " + input, e);
        }
    }


}
