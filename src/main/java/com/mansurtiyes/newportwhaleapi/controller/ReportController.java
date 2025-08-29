package com.mansurtiyes.newportwhaleapi.controller;

import com.mansurtiyes.newportwhaleapi.dto.report.ReportDTO;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import com.mansurtiyes.newportwhaleapi.service.ReportService;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/v1/reports
     * List daily reports with optional filters and pagination.
     *
     * Query params:
     * - start, end: ISO dates (e.g. 2025-05-01)
     * - speciesId: filter to days that include this species
     * - group: filter over group presence/absence (with hasSightings)
     * - hasSightings: when true => include days with sightings (of given group if provided),
     *                 when false => include days without sightings (of given group if provided)
     * - status: ok | bad_weather
     */
    @GetMapping
    public ResponseEntity<Page<ReportDTO>> getReports(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate end,
            @RequestParam(required = false) @Nullable String speciesId,
            @RequestParam(required = false) @Nullable SightingGroup group,
            @RequestParam(required = false) @Nullable Boolean hasSightings,
            @RequestParam(required = false) @Nullable ReportStatus status,
            Pageable pageable
    ) {
        Page<ReportDTO> page = reportService.getReports(
                start, end, speciesId, group, hasSightings, status, pageable
        );
        return ResponseEntity.ok(page);
    }

    /**
     * GET /api/v1/reports/{date}
     * Return a single report by calendar date (ISO yyyy-MM-dd).
     */
    @GetMapping("/{date}")
    public ResponseEntity<ReportDTO> getReportByDate(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        ReportDTO dto = reportService.getReportByDate(date);
        return ResponseEntity.ok(dto);
    }
}
