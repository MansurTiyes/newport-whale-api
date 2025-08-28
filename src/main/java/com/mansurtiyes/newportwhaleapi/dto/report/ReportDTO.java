package com.mansurtiyes.newportwhaleapi.dto.report;

import com.mansurtiyes.newportwhaleapi.model.ReportStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class ReportDTO {

    private LocalDate date;
    private int tours;
    private List<ObservationDto> observations;
    private ReportStatus status;
    private String sourceUrl;
    private OffsetDateTime fetchedAt;

    public ReportDTO() {
    }

    public ReportDTO(LocalDate date, int tours, List<ObservationDto> observations, ReportStatus status, String sourceUrl, OffsetDateTime fetchedAt) {
        this.date = date;
        this.tours = tours;
        this.observations = observations;
        this.status = status;
        this.sourceUrl = sourceUrl;
        this.fetchedAt = fetchedAt;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getTours() {
        return tours;
    }

    public void setTours(int tours) {
        this.tours = tours;
    }

    public List<ObservationDto> getObservations() {
        return observations;
    }

    public void setObservations(List<ObservationDto> observations) {
        this.observations = observations;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
