package com.mansurtiyes.newportwhaleapi.controller;

import com.mansurtiyes.newportwhaleapi.dto.SpeciesDTO;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import com.mansurtiyes.newportwhaleapi.service.SpeciesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/species")
public class SpeciesController {

    private final SpeciesService speciesService;

    public SpeciesController(SpeciesService speciesService) {
        this.speciesService = speciesService;
    }

    /**
     * GET /api/v1/species
     * List species with derived rollups (firstSeen/lastSeen, totalReports, totalIndividuals).
     *
     * @param group   optional group filter (whale|dolphin|shark|fish|other), case-insensitive
     * @param search  optional text search across id/common/binomial/aliases
     * @param pageable standard Spring pageable (page,size);
     */
    @GetMapping
    public ResponseEntity<Page<SpeciesDTO>> getAllSpecies(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        Page<SpeciesDTO> species = speciesService.getAllSpecies(
                group != null ? SightingGroup.valueOf(group.toLowerCase(Locale.ROOT)) : null,
                search,
                pageable
        );
        return ResponseEntity.ok(species);
    }

    /**
     * GET /api/v1/species/{id}
     * Details + rollups for a given species over an optional date range.
     * If both start and end are provided, service validates end >= start.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpeciesDTO> getSpeciesById(
            @PathVariable String id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        SpeciesDTO dto = speciesService.getSpeciesById(id, start, end);
        return ResponseEntity.ok(dto);
    }
}
