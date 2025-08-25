package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.SpeciesDTO;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface SpeciesReadRepository {

    Page<SpeciesDTO> findAllWithRollups(
            SightingGroup group,
            String search,
            Pageable pageable
    );

    Optional<SpeciesDTO> findByIdWithRollups(
            String id,
            LocalDate start,
            LocalDate end
    );
}
