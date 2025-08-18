package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.ingest.ParsedObservation;

import java.time.LocalDate;
import java.util.List;

public interface ObservationBulkRepository {
    /** Replace all observations for the given date with the provided snapshot. */
    void replaceAllForDate(LocalDate date, List<ParsedObservation> observations);
}
