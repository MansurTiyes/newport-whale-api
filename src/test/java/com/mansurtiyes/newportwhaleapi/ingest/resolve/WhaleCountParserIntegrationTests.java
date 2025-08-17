package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.ingest.ParsedObservation;
import com.mansurtiyes.newportwhaleapi.ingest.ParsedReport;
import com.mansurtiyes.newportwhaleapi.ingest.WhaleCountParser;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.Species;
import com.mansurtiyes.newportwhaleapi.repository.SpeciesRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WhaleCountParserIntegrationTests {

    // same split rule as WhaleCountParser: split commas that are followed by a digit
    // and NOT part of a number like "5,130"
    // Used by print method only
    private static final Pattern ITEM_SPLIT = Pattern.compile("(?<!\\d)\\s*,\\s*(?=\\d)");

    @Autowired SpeciesRepository speciesRepository;
    @Autowired InMemorySpeciesResolver speciesResolver;
    @Autowired WhaleCountParser parser;

    @BeforeEach
    void ensureResolverLoadedFromDb() {
        // Flyway has seeded the DB; make sure resolver builds its alias map
        speciesResolver.reload();
        // Optional sanity check:
        assertThat(speciesRepository.count()).isGreaterThan(0);
    }

    @Test
    void parses_sample_1() {
        String input = "3 Humpback Whales, 2 Fin Whales, 2 Minke Whales, 3 Mola Mola, 2800 Common Dolphin, 50 Bottlenose";
        printSplit("sample_1", input);

        List<ParsedObservation> out = parser.parseObservations(input);

        assertThat(out)
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .containsExactly(
                        tuple("humpback-whale", 3),
                        tuple("fin-whale", 2),
                        tuple("minke-whale", 2),
                        tuple("sunfish", 3),               // "Mola Mola" => sunfish
                        tuple("common-dolphin", 2800),
                        tuple("bottlenose-dolphin", 50)
                );
    }

    @Test
    void parses_sample_2() {
        String input = "5 Minke Whales, 1 Bryde's Whale, 1 Mola Mola, 1 Blue Shark, 5,130 Common Dolphin";
        printSplit("sample_2", input);

        List<ParsedObservation> out = parser.parseObservations(input);

        assertThat(out)
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .containsExactly(
                        tuple("minke-whale", 5),
                        tuple("brydes-whale", 1),
                        tuple("sunfish", 1),
                        tuple("common-dolphin", 5130)      // comma handled
                );
    }

    @Test
    void parses_sample_3() {
        String input = "4 Humpback Whales, 7 Minke Whales, 6,000 Common Dolphin, 2 Bottlenose Dolphin, 3 Mola Mola";
        printSplit("sample_3", input);

        List<ParsedObservation> out = parser.parseObservations(input);

        assertThat(out)
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .containsExactly(
                        tuple("humpback-whale", 4),
                        tuple("minke-whale", 7),
                        tuple("common-dolphin", 6000),
                        tuple("bottlenose-dolphin", 2),
                        tuple("sunfish", 3)
                );
    }

    @Test
    void parses_sample_4() {
        String input = "4 Humpback Whales, 2 Gray Whales, 1,830 Common Dolphin, 150 Bottlenose Dolphin";
        printSplit("sample_4", input);

        List<ParsedObservation> out = parser.parseObservations(input);

        assertThat(out)
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .containsExactly(
                        tuple("humpback-whale", 4),
                        tuple("gray-whale", 2),
                        tuple("common-dolphin", 1830),
                        tuple("bottlenose-dolphin", 150)
                );
    }

    @Test
    void parses_sample_5() {
        String input = "1 Humpback Whale, 2,700 Common Dolphin, 85 Bottlenose Dolphin, 1 Thresher Shark";
        printSplit("sample_5", input);

        List<ParsedObservation> out = parser.parseObservations(input);

        assertThat(out)
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .containsExactly(
                        tuple("humpback-whale", 1),
                        tuple("common-dolphin", 2700),
                        tuple("bottlenose-dolphin", 85),
                        tuple("thresher-shark", 1)
                );
    }

    @Test
    void parse_endToEnd_fromFixtureHtml() throws IOException {
        // 1) Load the HTML fixture (place a real copy at src/test/resources/fixtures/whalecount.html)
        String html = readResource("/fixtures/whalecount.html");
        Document doc = Jsoup.parse(html, "https://newportwhales.com/whalecount.html");

        // 2) Parse
        List<ParsedReport> reports = parser.parse(doc, "https://newportwhales.com/whalecount.html");

        // 3) Basic sanity
        assertThat(reports).isNotEmpty();

        // 4) Assert a known OK row (08/12/2025 from your fixture)
        ParsedReport rAug12 = findByDate(reports, LocalDate.of(2025, 8, 12))
                .orElseThrow(() -> new AssertionError("Report for 2025-08-12 not found"));

        assertThat(rAug12.getTours()).isEqualTo(14);
        assertThat(rAug12.getStatus()).isEqualTo(ReportStatus.ok);

        // Observations: expect (order here is the parsed order; ParsedReport sorts only for canonical)
        assertThat(rAug12.getObservations())
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .contains(
                        // From fixture row: "4 Fin Whales, 1 Mola Mola, 2855 Common Dolphin, 195 Bottlenose"
                        tuple("fin-whale", 4),
                        tuple("sunfish", 1),              // "Mola Mola" -> sunfish
                        tuple("common-dolphin", 2855),
                        tuple("bottlenose-dolphin", 195)
                );

        // Canonical should be present and stable JSON
        assertThat(rAug12.getCanonical()).isNotBlank().startsWith("{").contains("\"date\":\"2025-08-12\"");

        // 5) Assert a known Bad Weather row (04/26/2025 in your fixture)
        ParsedReport rApr26 = findByDate(reports, LocalDate.of(2025, 4, 26))
                .orElseThrow(() -> new AssertionError("Report for 2025-04-26 not found"));
        assertThat(rApr26.getStatus()).isEqualTo(ReportStatus.bad_weather);
        assertThat(rApr26.getTours()).isEqualTo(0);
        assertThat(rApr26.getObservations()).isEmpty();

        // 6) (Optional) Spot-check another mixed row
        ParsedReport rAug11 = findByDate(reports, LocalDate.of(2025, 8, 11))
                .orElseThrow(() -> new AssertionError("Report for 2025-08-11 not found"));
        assertThat(rAug11.getStatus()).isEqualTo(ReportStatus.ok);
        assertThat(rAug11.getObservations())
                .extracting(ParsedObservation::speciesId, ParsedObservation::individuals)
                .contains(
                        tuple("humpback-whale", 3),
                        tuple("fin-whale", 2),
                        tuple("minke-whale", 2),
                        tuple("common-dolphin", 2800),
                        tuple("bottlenose-dolphin", 50)
                );
    }




    // --- helper: print the split list for reference (matches parser's behavior) ---
    private static void printSplit(String label, String input) {
        String norm = TextNormalizer.norm(input);
        List<String> parts = Arrays.asList(ITEM_SPLIT.split(norm));
        System.out.println("split(" + label + "): " + parts);
    }

    // --- helpers ---

    private static Optional<ParsedReport> findByDate(List<ParsedReport> reports, LocalDate date) {
        return reports.stream().filter(r -> r.getDate().equals(date)).findFirst();
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = WhaleCountParserIntegrationTests.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
