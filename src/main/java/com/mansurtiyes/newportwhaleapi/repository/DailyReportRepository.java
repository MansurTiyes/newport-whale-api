package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.model.DailyReport;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReport, LocalDate> {

    /**
     * Returns the checksum for the given report date, if present.
     */
    @Query("select d.checksum from DailyReport d where d.reportDate = :date")
    Optional<UUID> findCheckSumByDate(@Param("date") LocalDate date);

    /**
     * Inserts (or updates if same PK) a DailyReport.
     * You can also directly use JpaRepository#save, this is just a named convenience.
     */
    @Transactional
    default DailyReport insert(DailyReport dailyReport) {
        // saveAndFlush ensures the insert happens immediately (use save(...) if you prefer batched flush)
        return this.saveAndFlush(dailyReport);
    }
}
