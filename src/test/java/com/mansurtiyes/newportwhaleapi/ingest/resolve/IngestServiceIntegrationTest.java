package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.ingest.HtmlFetcher;
import com.mansurtiyes.newportwhaleapi.ingest.IngestService;
import com.mansurtiyes.newportwhaleapi.model.DailyReport;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.repository.DailyReportRepository;
import com.mansurtiyes.newportwhaleapi.repository.ObservationRepository;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestServiceIntegrationTest {

    @Autowired private IngestService ingestService;
    @Autowired private DailyReportRepository dailyReportRepo;
    @Autowired private ObservationRepository observationRepo; // not used directly, but ensures bean is wired
    @Autowired private InMemorySpeciesResolver speciesResolver;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private HtmlFetcher fetcher; // we stub network

    private static final LocalDate OK_DATE   = LocalDate.of(2025, 8, 12);
    private static final LocalDate BAD_DATE  = LocalDate.of(2025, 4, 26);

    @BeforeEach
    void setUp() throws Exception {
        // Clean tables for test isolation (FK requires deleting observations first)
        jdbc.update("DELETE FROM observation");
        jdbc.update("DELETE FROM daily_report");

        // Ensure resolver builds alias map from DB (Flyway already seeded species)
        speciesResolver.reload();

        // Load fixture HTML and parse with base URL matching the site
        String html = readResource("/fixtures/whalecount.html");
        Document fixtureDoc = Jsoup.parse(html, "https://newportwhales.com/whalecount.html");

        // Stub network: fetch(any URI) -> our fixture document
        Mockito.when(fetcher.fetch(any())).thenReturn(fixtureDoc);
    }

    @Test
    @Transactional
    void ingest_firstRun_inserts_dailyReports_and_observations() throws Exception {
        // When
        ingestService.ingest();

        // Then: OK row exists with tours and observations
        DailyReport ok = dailyReportRepo.findById(OK_DATE)
                .orElseThrow(() -> new AssertionError("Missing daily_report for " + OK_DATE));
        assertThat(ok.getStatus()).isEqualTo(ReportStatus.ok);
        assertThat(ok.getTours()).isEqualTo(14);
        assertThat(ok.getChecksum()).isNotNull();

        int okObs = countObsForDate(OK_DATE);
        assertThat(okObs).isEqualTo(4); // e.g., 4 items parsed on that date

        // Bad weather row exists with zero observations
        DailyReport bad = dailyReportRepo.findById(BAD_DATE)
                .orElseThrow(() -> new AssertionError("Missing daily_report for " + BAD_DATE));
        assertThat(bad.getStatus()).isEqualTo(ReportStatus.bad_weather);
        int badObs = countObsForDate(BAD_DATE);
        assertThat(badObs).isZero();
    }

    @Test
    @Transactional
    void ingest_secondRun_sameData_isNoop() throws Exception {
        // First run
        ingestService.ingest();

        // Capture state after first run
        OffsetDateTime okUpdatedAt1 = dailyReportRepo.findById(OK_DATE).get().getUpdatedAt();
        int okObs1  = countObsForDate(OK_DATE);
        int badObs1 = countObsForDate(BAD_DATE);
        long dailyCount1 = dailyReportRepo.count();

        // Second run with SAME fixture (same stub)
        ingestService.ingest();

        // Assert nothing changed
        DailyReport ok2 = dailyReportRepo.findById(OK_DATE).get();
        assertThat(ok2.getUpdatedAt()).isEqualTo(okUpdatedAt1); // no update occurred
        assertThat(countObsForDate(OK_DATE)).isEqualTo(okObs1);
        assertThat(countObsForDate(BAD_DATE)).isEqualTo(badObs1);
        assertThat(dailyReportRepo.count()).isEqualTo(dailyCount1);
    }



    // --- helpers ---

    private int countObsForDate(LocalDate date) {
        Integer n = jdbc.queryForObject(
                "select count(*) from observation where report_date = ?",
                Integer.class, date);
        return (n == null) ? 0 : n;
    }

    private static String readResource(String path) throws IOException {
        try (InputStream is = IngestServiceIntegrationTest.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Fixture not found: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }


}
