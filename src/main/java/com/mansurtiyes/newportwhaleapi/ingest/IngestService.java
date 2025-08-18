package com.mansurtiyes.newportwhaleapi.ingest;

import com.mansurtiyes.newportwhaleapi.model.DailyReport;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.repository.DailyReportRepository;
import com.mansurtiyes.newportwhaleapi.repository.ObservationRepository;
import jakarta.transaction.Transactional;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class IngestService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    // Default feed URL (can externalize to config later)
    private static final URI FEED_URI = URI.create("https://newportwhales.com/whalecount.html");

    private final DailyReportRepository dailyReportRepo;
    private final ObservationRepository observationRepo;
    private final HtmlFetcher fetcher;
    private final WhaleCountParser parser;

    public IngestService(DailyReportRepository dailyReportRepo, ObservationRepository observationRepo, HtmlFetcher fetcher, WhaleCountParser parser) {
        this.dailyReportRepo = dailyReportRepo;
        this.observationRepo = observationRepo;
        this.fetcher = fetcher;
        this.parser = parser;
    }

    /**
     * Runs once when the application starts.
     */
    @Override
    public void run(String... args) {
        try {
            log.info("Bootstrap ingest starting…");
            ingest(); // uses default FEED_URI
            log.info("Bootstrap ingest finished.");
        } catch (Exception e) {
            // swallow errors so app still boots
            log.error("Bootstrap ingest failed", e);
        }
    }

    /**
     * Scheduled to run every day at 18:00 in Los Angeles time..
     */
    @Scheduled(cron = "0 0 18 * * *", zone = "America/Los_Angeles")
    public void scheduledIngest() {
        try {
            log.info("Scheduled ingest (America/Los_Angeles 18:00) starting…");
            ingest(); // uses default FEED_URI
            log.info("Scheduled ingest finished.");
        } catch (Exception e) {
            // swallow errors so scheduler keeps running
            log.error("Scheduled ingest failed", e);
        }
    }

    /**
     * Convenience entry point for runners/schedulers.
     */
    @Transactional
    public void ingest() throws Exception {
        ingest(FEED_URI);
    }

    /**
     * Reusable ingestion against any given URI. Parses the page and upserts reports+observations
     * only when the checksum for that date has changed (or is new).
     */
    @Transactional
    public void ingest(URI uri) throws Exception {
        // 1) Fetch & parse
        Document doc = fetcher.fetch(uri);
        List<ParsedReport> parsed = parser.parse(doc, uri.toString());
        if (parsed.isEmpty()) {
            log.info("Ingest: no parsed reports from {}", uri);
            return;
        }

        // 2) For each parsed daily report: compute checksum and conditionally write
        for (ParsedReport pr : parsed) {
            UUID newChecksum = CheckSumUtil.checksumFromCanonical(pr.getCanonical());

            Optional<UUID> existingOpt = dailyReportRepo.findCheckSumByDate(pr.getDate());
            if (existingOpt.isPresent() && existingOpt.get().equals(newChecksum)) {
                // No change for this date — skip any writes
                log.debug("Ingest: unchanged report for date {} — skipping", pr.getDate());
                continue;
            }

            // Either new date or changed content — (up)insert DailyReport first (FK parent)
            DailyReport dr = new DailyReport();
            dr.setReportDate(pr.getDate());
            dr.setTours(pr.getTours());
            dr.setStatus(pr.getStatus());
            dr.setFetchedAt(OffsetDateTime.now());
            dr.setSourceUrl(pr.getSourceUrl());
            dr.setChecksum(newChecksum);

            dailyReportRepo.insert(dr); // save parent row (required for FK)

            // Replace observations snapshot for that date
            if (pr.getStatus() == ReportStatus.bad_weather) {
                observationRepo.replaceAllForDate(pr.getDate(), List.of()); // keep table consistent
            } else {
                observationRepo.replaceAllForDate(pr.getDate(), pr.getObservations());
            }

            log.info("Ingest: processed {} daily reports from {}", parsed.size(), uri);
        }
    }
}
