package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.ingest.ParsedObservation;
import com.mansurtiyes.newportwhaleapi.model.DailyReport;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ObservationBulkRepositoryImplIntegrationTests {

    @Autowired private ObservationRepository observationRepo; // extends ObservationBulkRepository
    @Autowired private DailyReportRepository dailyReportRepo;
    @Autowired private JdbcTemplate jdbc;

    private final LocalDate D = LocalDate.of(2025, 8, 12);

    @BeforeEach
    @Transactional
    void ensureDailyReportRow() {
        // Make sure the FK target exists (species come from Flyway already)
        if (!dailyReportRepo.existsById(D)) {
            DailyReport dr = new DailyReport();
            dr.setReportDate(D);
            dr.setTours(14);
            dr.setStatus(ReportStatus.ok);
            dr.setFetchedAt(OffsetDateTime.now());
            dr.setSourceUrl("https://newportwhales.com/whalecount.html");
            dr.setChecksum(UUID.nameUUIDFromBytes(("daily:" + D).getBytes(StandardCharsets.UTF_8)));
            dailyReportRepo.insert(dr);
        }
    }

    @Test
    @Transactional
    void replaceAll_inserts_snapshot_for_date() {
        // Given a snapshot (3 species)
        List<ParsedObservation> snapshot = List.of(
                new ParsedObservation("humpback-whale", 3),
                new ParsedObservation("common-dolphin", 2800),
                new ParsedObservation("bottlenose-dolphin", 50)
        );

        // When
        observationRepo.replaceAllForDate(D, snapshot);

        // Then
        Map<String, Integer> rows = selectSpeciesCounts(D);
        assertThat(rows).containsOnlyKeys("humpback-whale", "common-dolphin", "bottlenose-dolphin");
        assertThat(rows.get("humpback-whale")).isEqualTo(3);
        assertThat(rows.get("common-dolphin")).isEqualTo(2800);
        assertThat(rows.get("bottlenose-dolphin")).isEqualTo(50);
    }

    @Test
    @Transactional
    void replaceAll_replaces_previous_snapshot_and_prunes_missing() {
        // First snapshot
        observationRepo.replaceAllForDate(D, List.of(
                new ParsedObservation("humpback-whale", 3),
                new ParsedObservation("common-dolphin", 2800),
                new ParsedObservation("bottlenose-dolphin", 50)
        ));

        // Second snapshot: update counts and remove humpback, add minke
        observationRepo.replaceAllForDate(D, List.of(
                new ParsedObservation("common-dolphin", 5130),
                new ParsedObservation("bottlenose-dolphin", 42),
                new ParsedObservation("minke-whale", 2)
        ));

        Map<String, Integer> rows = selectSpeciesCounts(D);

        // humpback must be gone; counts must be updated; minke added
        assertThat(rows).containsOnlyKeys("common-dolphin", "bottlenose-dolphin", "minke-whale");
        assertThat(rows.get("common-dolphin")).isEqualTo(5130);
        assertThat(rows.get("bottlenose-dolphin")).isEqualTo(42);
        assertThat(rows.get("minke-whale")).isEqualTo(2);
    }

    @Test
    @Transactional
    void replaceAll_with_empty_list_wipes_all_rows_for_date() {
        // Seed some rows
        observationRepo.replaceAllForDate(D, List.of(
                new ParsedObservation("common-dolphin", 1000),
                new ParsedObservation("bottlenose-dolphin", 10)
        ));

        // Now wipe
        observationRepo.replaceAllForDate(D, List.of());

        Map<String, Integer> rows = selectSpeciesCounts(D);
        assertThat(rows).isEmpty();
    }

    // ---- helpers ----

    private Map<String, Integer> selectSpeciesCounts(LocalDate date) {
        // Query the observation table for this date and return species_id -> individuals
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT species_id, individuals FROM observation WHERE report_date = ? ORDER BY species_id",
                date
        );
        return list.stream().collect(Collectors.toMap(
                r -> (String) r.get("species_id"),
                r -> ((Number) r.get("individuals")).intValue()
        ));
    }
}
