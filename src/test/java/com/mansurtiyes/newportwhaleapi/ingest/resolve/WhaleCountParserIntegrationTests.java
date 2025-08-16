package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.ingest.ParsedObservation;
import com.mansurtiyes.newportwhaleapi.ingest.WhaleCountParser;
import com.mansurtiyes.newportwhaleapi.model.Species;
import com.mansurtiyes.newportwhaleapi.repository.SpeciesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
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

    // --- helper: print the split list for reference (matches parser's behavior) ---
    private static void printSplit(String label, String input) {
        String norm = TextNormalizer.norm(input);
        List<String> parts = Arrays.asList(ITEM_SPLIT.split(norm));
        System.out.println("split(" + label + "): " + parts);
    }
}
