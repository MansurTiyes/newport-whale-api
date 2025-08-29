package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.report.ObservationDto;
import com.mansurtiyes.newportwhaleapi.dto.report.ReportDTO;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ReportReadRepositoryImpl implements ReportReadRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ReportReadRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- Query builders ---

    private static final String BASE_COUNT = """
        SELECT COUNT(*) 
        FROM daily_report d
        """;

    private static final String BASE_PAGE = """
        SELECT d.report_date, d.tours, d.status, d.source_url, d.fetched_at
        FROM daily_report d
        """;

    private static final String OBS_FOR_DATES = """
        SELECT o.report_date, o.species_id, o.individuals
        FROM observation o
        WHERE o.report_date IN (:dates)
        ORDER BY o.report_date ASC, o.species_id ASC
        """;

    @Override
    public Page<ReportDTO> findReports(@Nullable LocalDate start,
                                       @Nullable LocalDate end,
                                       @Nullable String speciesId,
                                       @Nullable SightingGroup group,
                                       @Nullable Boolean hasSightings,
                                       @Nullable ReportStatus status,
                                       Pageable pageable) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> where = new ArrayList<>();

        // Window
        params.addValue("start", start, Types.DATE);
        params.addValue("end", end, Types.DATE);
        where.add("(:start IS NULL OR d.report_date >= :start)");
        where.add("(:end   IS NULL OR d.report_date <= :end)");

        // Status
        params.addValue("status", status != null ? status.name() : null, Types.VARCHAR);
        where.add("(:status IS NULL OR d.status = CAST(:status AS report_status))");

        // Species filter (EXISTS)
        params.addValue("speciesId", speciesId, Types.VARCHAR);
        if (speciesId != null && !speciesId.isBlank()) {
            where.add("""
                EXISTS (
                   SELECT 1 FROM observation o
                   WHERE o.report_date = d.report_date
                     AND o.species_id = :speciesId
                )
                """);
        }

        // Group + hasSightings logic (mutually exclusive with speciesId at service layer; repo supports both anyway)
        String existsGroup = """
            EXISTS (
               SELECT 1
               FROM observation o
               JOIN species s ON s.id = o.species_id
               WHERE o.report_date = d.report_date
                 AND (:group IS NULL OR s."group" = CAST(:group AS sighting_group))
            )
            """;
        String existsAny = """
            EXISTS (
               SELECT 1
               FROM observation o
               WHERE o.report_date = d.report_date
            )
            """;

        params.addValue("group", group != null ? group.name().toLowerCase(Locale.ROOT) : null, Types.VARCHAR);

        if (group != null) {
            if (hasSightings == null || hasSightings) {
                where.add(existsGroup);
            } else {
                where.add("NOT " + existsGroup);
            }
        } else if (hasSightings != null) {
            if (hasSightings) {
                where.add(existsAny);
            } else {
                where.add("NOT " + existsAny);
            }
        }

        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);

        // Count
        long total = jdbc.queryForObject(BASE_COUNT + whereSql, params, Long.class);
        if (total == 0) {
            return Page.empty(pageable);
        }

        // Order + paging; default by date asc if no explicit sort
        String orderBy = buildOrderBy(pageable.getSort());

        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", (long) pageable.getPageNumber() * pageable.getPageSize());

        String pageSql = BASE_PAGE + whereSql + " " + orderBy + " LIMIT :limit OFFSET :offset";

        List<ReportDTO> pageRows = jdbc.query(pageSql, params, new DayRowMapper());

        if (pageRows.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // Fetch observations for all page dates in one go
        List<LocalDate> dates = pageRows.stream().map(ReportDTO::getDate).toList();
        MapSqlParameterSource obsParams = new MapSqlParameterSource().addValue("dates", dates);

        List<ObsRow> obsRows = jdbc.query(OBS_FOR_DATES, obsParams, (rs, n) ->
                new ObsRow(
                        rs.getObject("report_date", LocalDate.class),
                        rs.getString("species_id"),
                        rs.getInt("individuals"))
        );

        Map<LocalDate, List<ObservationDto>> grouped = obsRows.stream()
                .collect(Collectors.groupingBy(
                        o -> o.date,
                        Collectors.mapping(o -> new ObservationDto(o.speciesId, o.individuals), Collectors.toList())
                ));

        // Attach observations
        for (ReportDTO r : pageRows) {
            List<ObservationDto> list = grouped.getOrDefault(r.getDate(), List.of());
            r.setObservations(list);
        }

        return new PageImpl<>(pageRows, pageable, total);
    }

    @Override
    public Optional<ReportDTO> findByDate(LocalDate date) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("date", Objects.requireNonNull(date), Types.DATE);

        String oneDaySql = BASE_PAGE + " WHERE d.report_date = :date";

        List<ReportDTO> rows = jdbc.query(oneDaySql, params, new DayRowMapper());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ReportDTO dto = rows.get(0);

        // Observations for one date
        MapSqlParameterSource obsParams = new MapSqlParameterSource().addValue("dates", List.of(date));
        List<ObsRow> obsRows = jdbc.query(OBS_FOR_DATES, obsParams, (rs, n) ->
                new ObsRow(
                        rs.getObject("report_date", LocalDate.class),
                        rs.getString("species_id"),
                        rs.getInt("individuals"))
        );
        List<ObservationDto> items = obsRows.stream()
                .map(o -> new ObservationDto(o.speciesId, o.individuals))
                .toList();
        dto.setObservations(items);
        return Optional.of(dto);
    }


    // --- helpers ---

    private static class DayRowMapper implements RowMapper<ReportDTO> {
        @Override
        public ReportDTO mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            LocalDate date = rs.getObject("report_date", LocalDate.class);
            int tours = rs.getInt("tours");
            String statusStr = rs.getString("status");
            ReportStatus status = statusStr != null ? ReportStatus.valueOf(statusStr) : null;
            String sourceUrl = rs.getString("source_url");

            OffsetDateTime fetchedAt = null;
            try {
                fetchedAt = rs.getObject("fetched_at", OffsetDateTime.class);
            } catch (Throwable ignore) {
                // Fallback for older drivers
                java.sql.Timestamp ts = rs.getTimestamp("fetched_at");
                if (ts != null) fetchedAt = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
            }

            ReportDTO dto = new ReportDTO();
            dto.setDate(date);
            dto.setTours(tours);
            dto.setStatus(status);
            dto.setSourceUrl(sourceUrl);
            dto.setFetchedAt(fetchedAt);
            dto.setObservations(List.of()); // will be filled later
            return dto;
        }
    }

    private record ObsRow(LocalDate date, String speciesId, int individuals) {}

    /**
     * Builds ORDER BY based on pageable sort, whitelisting sortable columns to avoid SQL injection.
     * Supported: date -> d.report_date, tours -> d.tours, status -> d.status, fetchedAt -> d.fetched_at
     * Default: ORDER BY d.report_date ASC
     */
    private String buildOrderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return "ORDER BY d.report_date ASC";
        }
        List<String> parts = new ArrayList<>();
        for (Sort.Order order : sort) {
            String col = switch (order.getProperty()) {
                case "date" -> "d.report_date";
                case "tours" -> "d.tours";
                case "status" -> "d.status";
                case "fetchedAt" -> "d.fetched_at";
                default -> null;
            };
            if (col != null) {
                parts.add(col + (order.isAscending() ? " ASC" : " DESC"));
            }
        }
        if (parts.isEmpty()) {
            return "ORDER BY d.report_date ASC";
        }
        return "ORDER BY " + String.join(", ", parts);
    }
}


