package com.mansurtiyes.newportwhaleapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class ObservationId implements Serializable {

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "species_id", nullable = false)
    private String speciesId;

    public ObservationId() {
    }

    public ObservationId(LocalDate reportDate, String speciesId) {
        this.reportDate = reportDate;
        this.speciesId = speciesId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(String speciesId) {
        this.speciesId = speciesId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObservationId that)) return false;
        return Objects.equals(reportDate, that.reportDate)
                && Objects.equals(speciesId, that.speciesId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportDate, speciesId);
    }
}
