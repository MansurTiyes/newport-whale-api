package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.SpeciesDTO;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import jakarta.annotation.Nullable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class SpeciesReadRepositoryImpl implements SpeciesReadRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public SpeciesReadRepositoryImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- SQL (list) ----------

    private static final String WHERE_FILTER =
            " WHERE (:group IS NULL OR s.\"group\" = CAST(:group AS sighting_group)) " +
                    "   AND ( :q IS NULL " +
                    "         OR s.id ILIKE :like " +
                    "         OR s.common_name ILIKE :like " +
                    "         OR s.binomial_name ILIKE :like " +
                    "         OR EXISTS (SELECT 1 FROM unnest(s.aliases) a WHERE a ILIKE :like) " +
                    "       ) ";

    private static final String COUNT_SQL =
            "SELECT COUNT(*) " +
                    "FROM species s " +
                    WHERE_FILTER;

    private static final String PAGE_SQL =
            "SELECT " +
                    "  s.id, " +
                    "  s.\"group\", " +
                    "  s.common_name, " +
                    "  s.binomial_name, " +
                    "  s.aliases, " +
                    "  MIN(o.report_date)                AS first_seen, " +
                    "  MAX(o.report_date)                AS last_seen, " +
                    "  COUNT(DISTINCT o.report_date)     AS total_reports, " +
                    "  COALESCE(SUM(o.individuals), 0)   AS total_individuals " +
                    "FROM species s " +
                    "LEFT JOIN observation o ON o.species_id = s.id " +
                    WHERE_FILTER +
                    "GROUP BY s.id, s.\"group\", s.common_name, s.binomial_name, s.aliases " +
                    "ORDER BY MAX(o.report_date) DESC NULLS LAST, s.id ASC " +   // default sort
                    "LIMIT :limit OFFSET :offset";

    // ---------- SQL (by id + range) ----------

    private static final String BY_ID_SQL =
            "SELECT " +
                    "  s.id, " +
                    "  s.\"group\", " +
                    "  s.common_name, " +
                    "  s.binomial_name, " +
                    "  s.aliases, " +
                    "  MIN(CASE WHEN (:start IS NULL OR o.report_date >= :start) " +
                    "            AND (:end   IS NULL OR o.report_date <= :end) " +
                    "            THEN o.report_date END)                        AS first_seen, " +
                    "  MAX(CASE WHEN (:start IS NULL OR o.report_date >= :start) " +
                    "            AND (:end   IS NULL OR o.report_date <= :end) " +
                    "            THEN o.report_date END)                        AS last_seen, " +
                    "  COUNT(DISTINCT CASE WHEN (:start IS NULL OR o.report_date >= :start) " +
                    "                       AND (:end   IS NULL OR o.report_date <= :end) " +
                    "                       THEN o.report_date END)             AS total_reports, " +
                    "  COALESCE(SUM(CASE WHEN (:start IS NULL OR o.report_date >= :start) " +
                    "                      AND (:end   IS NULL OR o.report_date <= :end) " +
                    "                      THEN o.individuals ELSE 0 END), 0)   AS total_individuals " +
                    "FROM species s " +
                    "LEFT JOIN observation o ON o.species_id = s.id " +
                    "WHERE s.id = :id " +
                    "GROUP BY s.id, s.\"group\", s.common_name, s.binomial_name, s.aliases";

    @Override
    public Page<SpeciesDTO> findAllWithRollups(@Nullable SightingGroup group,
                                               @Nullable String search,
                                               Pageable pageable) {

        MapSqlParameterSource params = new MapSqlParameterSource();

        // group param: pass lowercase string for CAST(:group AS sighting_group), or null
        params.addValue("group", group != null ? group.name() : null, Types.VARCHAR);

        // search param
        String q = (search != null && !search.isBlank()) ? search.trim() : null;
        params.addValue("q", q, Types.VARCHAR);
        params.addValue("like", q != null ? "%" + q + "%" : null,  Types.VARCHAR);

        // count first for pagination (can't return null, can ignore warning)
        long total = jdbc.queryForObject(COUNT_SQL, params, Long.class);

        if (total == 0) {
            return Page.empty(pageable);
        }

        // paging params
        params.addValue("limit", pageable.getPageSize());
        params.addValue("offset", (long) pageable.getPageNumber() * pageable.getPageSize());

        List<SpeciesDTO> content = jdbc.query(PAGE_SQL, params, new SpeciesRowMapper());
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<SpeciesDTO> findByIdWithRollups(String id,
                                                    @Nullable LocalDate start,
                                                    @Nullable LocalDate end) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", Objects.requireNonNull(id, "id"));
        params.addValue("start", start, Types.DATE);
        params.addValue("end", end, Types.DATE);

        try {
            SpeciesDTO dto = jdbc.queryForObject(BY_ID_SQL, params, new SpeciesRowMapper());
            return Optional.ofNullable(dto);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }



    /**
     * Maps the aggregate query rows into SpeciesDTO.
     * - Handles Postgres enum "group" -> Java enum (case-insensitive)
     * - Converts text[] aliases -> List<String>
     * - first_seen / last_seen may be null
     */
    private static class SpeciesRowMapper implements RowMapper<SpeciesDTO> {
        @Override
        public SpeciesDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            SpeciesDTO dto = new SpeciesDTO();

            dto.setId(rs.getString("id"));

            String groupStr = rs.getString("group");
            dto.setGroup(groupStr != null
                    ? SightingGroup.valueOf(groupStr)
                    : null);

            dto.setCommonName(rs.getString("common_name"));
            dto.setBinomialName(rs.getString("binomial_name"));

            Array aliasesSql = rs.getArray("aliases");
            if (aliasesSql != null) {
                String[] arr = (String[]) aliasesSql.getArray();
                dto.setAliases(arr != null ? Arrays.asList(arr) : List.of());
            } else {
                dto.setAliases(List.of());
            }

            java.sql.Date first = rs.getDate("first_seen");
            dto.setFirstSeen(first != null ? first.toLocalDate() : null);

            java.sql.Date last = rs.getDate("last_seen");
            dto.setLastSeen(last != null ? last.toLocalDate() : null);

            dto.setTotalReports(rs.getLong("total_reports"));
            dto.setTotalIndividuals(rs.getLong("total_individuals"));

            return dto;
        }
    }
}
