package com.mansurtiyes.newportwhaleapi.dto;

import com.mansurtiyes.newportwhaleapi.model.SightingGroup;

import java.time.LocalDate;
import java.util.List;

public class SpeciesDTO {

    private String id;
    private SightingGroup group;
    private String commonName;
    private String binomialName;
    private List<String> aliases;
    private LocalDate firstSeen;
    private LocalDate lastSeen;
    private long totalReports;
    private long totalIndividuals;

    public SpeciesDTO() {
    }

    public SpeciesDTO(String id, SightingGroup group, String commonName, String binomialName, List<String> aliases, LocalDate firstSeen, LocalDate lastSeen, long totalReports, long totalIndividuals) {
        this.id = id;
        this.group = group;
        this.commonName = commonName;
        this.binomialName = binomialName;
        this.aliases = aliases;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.totalReports = totalReports;
        this.totalIndividuals = totalIndividuals;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SightingGroup getGroup() {
        return group;
    }

    public void setGroup(SightingGroup group) {
        this.group = group;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getBinomialName() {
        return binomialName;
    }

    public void setBinomialName(String binomialName) {
        this.binomialName = binomialName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public LocalDate getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(LocalDate firstSeen) {
        this.firstSeen = firstSeen;
    }

    public LocalDate getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDate lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getTotalReports() {
        return totalReports;
    }

    public void setTotalReports(long totalReports) {
        this.totalReports = totalReports;
    }

    public long getTotalIndividuals() {
        return totalIndividuals;
    }

    public void setTotalIndividuals(long totalIndividuals) {
        this.totalIndividuals = totalIndividuals;
    }
}
