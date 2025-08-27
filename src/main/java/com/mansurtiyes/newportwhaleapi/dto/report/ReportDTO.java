package com.mansurtiyes.newportwhaleapi.dto.report;

import com.mansurtiyes.newportwhaleapi.model.ReportStatus;

import java.time.LocalDate;
import java.util.List;

public class ReportDTO {

    private LocalDate date;
    private int tours;
    private List<ObservationDto> observations;
    private ReportStatus status;
    private SourceDto source;

    public ReportDTO() {
    }

    public ReportDTO(LocalDate date, int tours, List<ObservationDto> observations, ReportStatus status, SourceDto source) {
        this.date = date;
        this.tours = tours;
        this.observations = observations;
        this.status = status;
        this.source = source;
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

    public SourceDto getSource() {
        return source;
    }

    public void setSource(SourceDto source) {
        this.source = source;
    }
}
