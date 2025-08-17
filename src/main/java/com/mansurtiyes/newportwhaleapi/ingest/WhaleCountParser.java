package com.mansurtiyes.newportwhaleapi.ingest;

import com.mansurtiyes.newportwhaleapi.ingest.resolve.InMemorySpeciesResolver;
import com.mansurtiyes.newportwhaleapi.ingest.resolve.TextNormalizer;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WhaleCountParser implements HtmlParser {

    private final InMemorySpeciesResolver speciesResolver;

    private static final DateTimeFormatter M_D_YYYY =
            DateTimeFormatter.ofPattern("M/d/yyyy");
    // Split on commas that are followed by a digit (item separators), so we DON'T split "5,130"
    private static final Pattern ITEM_SPLIT = Pattern.compile("(?<!\\d)\\s*,\\s*(?=\\d)");

    // One item looks like: "<number with optional commas><space><species label>"
    // e.g., "4 fin whales", "5,130 common dolphin"
    private static final Pattern OBS_PATTERN = Pattern.compile("^([\\d,]+)\\s+(.+)$");

    public WhaleCountParser(InMemorySpeciesResolver speciesResolver) {
        this.speciesResolver = speciesResolver;
    }

    @Override
    public List<ParsedReport> parse(Document doc, String sourceUrl) {
        Element table = selectRecentCountsTable(doc);
        if (table == null) return List.of();

        List<ParsedReport> out = new ArrayList<>();

        // Only real data rows (skip header rows with <th>)
        for (Element row : table.select("tbody > tr:has(td)")) {
            Elements tds = row.select("td");
            if (tds.size() < 3) continue;

            String dateText = tds.get(0).text();
            String toursText = tds.get(1).text();
            String details   = tds.get(2).text();

            // Parse date & tours; if either fails, skip this row
            final LocalDate date;
            final Integer tours;
            try {
                date  = parseDate(dateText);
                tours = parseTours(toursText);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            // 1) Bad weather handling
            ReportStatus status = parseStatus(details);
            if (status == ReportStatus.bad_weather) {
                out.add(new ParsedReport(date, tours, status, List.of(), sourceUrl));
                continue;
            }

            // 2) Parse observations from details
            List<ParsedObservation> observations = parseObservations(details);

            // 3) Add report
            out.add(new ParsedReport(date, tours, ReportStatus.ok, observations, sourceUrl));

        }

        return out;
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

    // if description is "bad weather" -> returns ReportStatus.bad_weather
    // otherwise returns ReportStatus.ok
    public ReportStatus parseStatus(String description) {
        if (description != null && description.trim().equalsIgnoreCase("Bad Weather")) {
            return ReportStatus.bad_weather;
        }
        return ReportStatus.ok;
    }

    public List<ParsedObservation> parseObservations(String description) {
        if (description == null) return List.of();

        // normalize accents/dashes/quotes, collapse spaces, lowercase, trim
        final String norm = TextNormalizer.norm(description);
        if (norm.isBlank()) return List.of();

        List<ParsedObservation> out = new ArrayList<>();

        // split into chunks: "4 fin whales", "1 mola mola", "2855 common dolphin", ...
        String[] parts = ITEM_SPLIT.split(norm);

        for (String part : parts) {
            if (part.isBlank()) continue;

            Matcher m = OBS_PATTERN.matcher(part);
            if (!m.matches()) {
                // e.g., "bad weather" or malformed segment — skip
                continue;
            }

            // 1) parse leading integer (allow thousands separators)
            String countStr = m.group(1).replace(",", "");
            final int individuals;
            try {
                individuals = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                // bad number -> skip this chunk
                continue;
            }

            // 2) remaining text is the species label
            String label = m.group(2).trim();

            // minor cleanup (rare trailing punctuation)
            label = label.replaceAll("[\\p{Punct}]+$", "").trim();

            // 3) resolve to speciesId via resolver (aliases are already accounted for)
            Optional<String> speciesId = speciesResolver.resolve(label);
            if (speciesId.isEmpty()) {
                // couldn’t resolve; skip this chunk (or log if added later)
                continue;
            }

            out.add(new ParsedObservation(speciesId.get(), individuals));
        }

        return out;
    }


}
