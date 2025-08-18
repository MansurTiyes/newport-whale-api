package com.mansurtiyes.newportwhaleapi.ingest;

import com.mansurtiyes.newportwhaleapi.model.DailyReport;
import com.mansurtiyes.newportwhaleapi.repository.DailyReportRepository;
import com.mansurtiyes.newportwhaleapi.repository.ObservationRepository;
import jakarta.transaction.Transactional;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IngestService {

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

        // 2) For each parsed daily report: compute checksum and conditionally write
        for (ParsedReport pr : parsed) {
            UUID newChecksum = CheckSumUtil.checksumFromCanonical(pr.getCanonical());

            Optional<UUID> existingOpt = dailyReportRepo.findCheckSumByDate(pr.getDate());
            if (existingOpt.isPresent() && existingOpt.get().equals(newChecksum)) {
                // No change for this date — skip any writes
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
            observationRepo.replaceAllForDate(pr.getDate(), pr.getObservations());
        }
    }
}
