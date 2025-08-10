package com.mansurtiyes.newportwhaleapi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "observation",
        indexes = {
                @Index(name = "ix_observation_species", columnList = "species_id"),
                @Index(name = "ix_observation_species_date", columnList = "species_id,report_date")
        }
)
public class Observation {

    @EmbeddedId
    private ObservationId id;

    @MapsId("reportDate")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_date", nullable = false)
    // Optional: DB cascades deletes via DDL (Flyway already created it)
    // @OnDelete(action = OnDeleteAction.CASCADE)
    private DailyReport dailyReport;

    @MapsId("speciesId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    // DB has ON DELETE RESTRICT; we don't mirror that here — DB enforces it
    private Species species;

    @NotNull
    @Min(0)
    @Column(name = "individuals", nullable = false)
    private Integer individuals;

    public Observation() {
    }

    public Observation(DailyReport dailyReport, Species species, Integer individuals) {
        setDailyReport(dailyReport);
        setSpecies(species);
        this.individuals = individuals;
        if (this.id == null) {
            this.id = new ObservationId(dailyReport.getReportDate(), species.getId());
        }
    }

    public ObservationId getId() { return id; }

    public DailyReport getDailyReport() { return dailyReport; }

    // when changing dailyReport reference, make sure the embedded primary key (ObservationId) is updated so that its
    // reportDate matches the date from the new dailyReport
    public void setDailyReport(DailyReport dailyReport) {
        // dailyReport is a required field, allowing null would cause persistence error.
        // failing wast with exception makes the bug obvious
        if (dailyReport == null) throw new IllegalArgumentException("dailyReport cannot be null");
        this.dailyReport = dailyReport;

        // Observation uses a composite primary key: ObservationId (reportDate, speciesId)
        // that composite key object (id) might be null if this entity was just created via constructor that didn't set it yet or via JPA when building relationships
        // if it's null, create a new instance so we can populate it
        if (id == null) id = new ObservationId();

        // because of @MapsId("reportDate"), JPA uses ObservationId.reportDate to store the FK to DailyReport.reportDate
        // if not set here, id and dailyReport go out of sync
        id.setReportDate(dailyReport.getReportDate());
    }

    public Species getSpecies() { return species; }


    public void setSpecies(Species species) {
        if (species == null) throw new IllegalArgumentException("species cannot be null");
        this.species = species;
        if (id == null) id = new ObservationId();
        id.setSpeciesId(species.getId());
    }

    public Integer getIndividuals() { return individuals; }
    public void setIndividuals(Integer individuals) { this.individuals = individuals; }
}
