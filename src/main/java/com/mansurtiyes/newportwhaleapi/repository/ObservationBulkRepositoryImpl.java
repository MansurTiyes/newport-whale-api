package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.ingest.ParsedObservation;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
class ObservationBulkRepositoryImpl implements ObservationBulkRepository {

    private final JdbcTemplate jdbc;

    ObservationBulkRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    @Override
    public void replaceAllForDate(LocalDate date, List<ParsedObservation> observations) {
        // 1) delete snapshot for that date
        jdbc.update("DELETE FROM observation WHERE report_date = ?", ps -> {
            // PG JDBC 42+ supports setObject(LocalDate); if not, use Date.valueOf(date)
            ps.setObject(1, date);
        });

        // 2) nothing else to insert?
        if (observations == null || observations.isEmpty()) return;

        // 3) batch INSERT (leverages FK to daily_report + species already existing)
        jdbc.batchUpdate(
                "INSERT INTO observation (report_date, species_id, individuals) VALUES (?, ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ParsedObservation o = observations.get(i);
                        ps.setObject(1, date);               // or ps.setDate(1, java.sql.Date.valueOf(date))
                        ps.setString(2, o.speciesId());
                        ps.setInt(3, o.individuals());
                    }
                    @Override public int getBatchSize() { return observations.size(); }
                }
        );
    }
}
