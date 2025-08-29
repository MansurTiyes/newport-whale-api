package com.mansurtiyes.newportwhaleapi.service;

import com.mansurtiyes.newportwhaleapi.dto.report.ReportDTO;
import com.mansurtiyes.newportwhaleapi.exception.NotFoundException;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import com.mansurtiyes.newportwhaleapi.repository.ReportReadRepository;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportReadRepository reportReadRepository;

    public ReportService(ReportReadRepository reportReadRepository) {
        this.reportReadRepository = reportReadRepository;
    }

    /**
     * List daily reports with filters and pagination.
     *
     * Validations:
     * - speciesId and group cannot both be present
     * - if both start and end are provided, end must be on/after start
     */
    public Page<ReportDTO> getReports(@Nullable LocalDate start,
                                      @Nullable LocalDate end,
                                      @Nullable String speciesId,
                                      @Nullable SightingGroup group,
                                      @Nullable Boolean hasSightings,
                                      @Nullable ReportStatus status,
                                      Pageable pageable) {

        // 1) Mutually exclusive filters: speciesId vs group
        final boolean hasSpecies = speciesId != null && !speciesId.isBlank();
        if (hasSpecies && group != null) {
            throw new IllegalArgumentException("Filters 'speciesId' and 'group' are mutually exclusive.");
        }

        // 2) Date window validation
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after start date.");
        }

        // Optional: normalize speciesId
        final String normalizedSpecies = hasSpecies ? speciesId.trim() : null;

        return reportReadRepository.findReports(
                start,
                end,
                normalizedSpecies,
                group,
                hasSightings,
                status,
                Objects.requireNonNull(pageable, "pageable")
        );
    }

    /**
     * Fetch a single daily report by date.
     * @throws NotFoundException when no report exists for the given date.
     */
    public ReportDTO getReportByDate(LocalDate date) {
        return reportReadRepository.findByDate(Objects.requireNonNull(date, "date"))
                .orElseThrow(() -> new NotFoundException("Report not found for date: " + date));
    }
}
