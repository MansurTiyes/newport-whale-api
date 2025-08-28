package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.report.ReportDTO;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;


public interface ReportReadRepository {

    Page<ReportDTO> findReports(
            @Nullable LocalDate start,
            @Nullable LocalDate end,
            @Nullable String speciesId,
            @Nullable SightingGroup group,
            @Nullable Boolean hasSightings,
            @Nullable ReportStatus status,
            Pageable pageable
    );

    Optional<ReportDTO> findByDate(LocalDate date);
}
