package com.mansurtiyes.newportwhaleapi.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;

import java.time.LocalDate;
import java.util.*;

public class ParsedReport {
    private final LocalDate date;
    private final int tours;
    private final ReportStatus status;
    private final List<ParsedObservation> observations;
    private final String sourceUrl;
    private final String canonical; // stable string used for checkum

    public ParsedReport(LocalDate date, int tours, ReportStatus status,
                        List<ParsedObservation> observations, String sourceUrl) {
        this.date = date;
        this.tours = tours;
        this.status = status;
        this.observations = new ArrayList<>(observations);
        // Canonicalize: sort obs by speciesId, then JSON-encode stable fields
        this.observations.sort(Comparator.comparing(ParsedObservation::speciesId));
        this.sourceUrl = sourceUrl;
        this.canonical = buildCanonical();
    }

    public LocalDate getDate() {
        return date;
    }

    public int getTours() {
        return tours;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public List<ParsedObservation> getObservations() {
        return observations;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getCanonical() {
        return canonical;
    }

    private String buildCanonical() {
        // Keep stable ordering, no whitespace quirks.
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("date", date.toString());
        map.put("tours", tours);
        map.put("status", status.name().toLowerCase());
        List<Map<String,Object>> obs = new ArrayList<>();
        for (ParsedObservation o : observations) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("speciesId", o.speciesId());
            m.put("count", o.individuals());
            obs.add(m);
        }
        map.put("observations", obs);
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            // Fallback, but this should basically never happen.
            return date + "|" + tours + "|" + status + "|" + obs.toString();
        }
    }
}
