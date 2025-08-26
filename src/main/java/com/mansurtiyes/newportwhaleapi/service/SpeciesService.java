package com.mansurtiyes.newportwhaleapi.service;

import com.mansurtiyes.newportwhaleapi.dto.SpeciesDTO;
import com.mansurtiyes.newportwhaleapi.exception.NotFoundException;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import com.mansurtiyes.newportwhaleapi.repository.SpeciesReadRepository;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SpeciesService {

    private final SpeciesReadRepository speciesReadRepository;

    public SpeciesService(SpeciesReadRepository speciesReadRepository) {
        this.speciesReadRepository = speciesReadRepository;
    }

    /**
     * Paged list of species with derived rollups.
     * @param group optional group filter
     * @param search optional text search (id/common/binomial/aliases)
     * @param pageable page+size (and later, sort if you add it)
     */
    public Page<SpeciesDTO> getAllSpecies(@Nullable SightingGroup group,
                                          @Nullable String search,
                                          Pageable pageable) {
        // normalize blank search -> null
        final String q = (search != null && !search.isBlank()) ? search.trim() : null;
        return speciesReadRepository.findAllWithRollups(group, q, pageable);
    }

    /**
     * Species details + rollups for an optional date window.
     * Returns 404-style error when species does not exist.
     *
     * Validates that if both start and end are provided, then end >= start.
     */
    public SpeciesDTO getSpeciesById(String id,
                                     @Nullable LocalDate start,
                                     @Nullable LocalDate end) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("species id must be provided");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("end date must be on or after start date");
        }

        return speciesReadRepository.findByIdWithRollups(id, start, end)
                .orElseThrow(() -> new NotFoundException("species '" + id + "' not found"));
    }
}
