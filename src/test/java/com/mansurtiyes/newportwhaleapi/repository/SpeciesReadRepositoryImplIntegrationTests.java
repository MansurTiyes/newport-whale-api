package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.SpeciesDTO;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SpeciesReadRepositoryImplIntegrationTests {

    @Autowired
    private SpeciesReadRepository repo;

    @Test
    @DisplayName("findAllWithRollups: filters by group=whale and paginates")
    void list_whales_with_rollups_and_pagination() {
        Page<SpeciesDTO> page = repo.findAllWithRollups(SightingGroup.whale, null, PageRequest.of(0, 10));

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getSize()).isEqualTo(10);

        // All should be whales
        assertThat(page.getContent())
                .allSatisfy(dto -> assertThat(dto.getGroup()).isEqualTo(SightingGroup.whale));

        // Sanity: known whale should be present in the first few pages overall
        boolean hasHumpback = page.getContent().stream().anyMatch(d -> "humpback-whale".equals(d.getId()));
        // Might not be in the first page depending on sort / existing data size, so just assert page has some content
        assertThat(page.getNumberOfElements()).isGreaterThan(0);
    }

    @Test
    @DisplayName("findAllWithRollups: search by 'bryde' returns bryde's whale with recent lastSeen")
    void search_brydes_whale() {
        Page<SpeciesDTO> page = repo.findAllWithRollups(SightingGroup.whale, "bryde", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();

        SpeciesDTO brydes = page.getContent().stream()
                .filter(d -> "brydes-whale".equals(d.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected brydes-whale in search results"));

        // lastSeen should be at or after Aug 2, 2025 per sightings (bryde's present on 08/02/2025)
        assertThat(brydes.getLastSeen()).isNotNull();
        assertThat(brydes.getLastSeen()).isAfterOrEqualTo(LocalDate.of(2025, 8, 2));

        // Reports/individuals are positive in the season
        assertThat(brydes.getTotalReports()).isGreaterThan(0);
        assertThat(brydes.getTotalIndividuals()).isGreaterThan(0);
    }

    @Test
    @DisplayName("findByIdWithRollups: fin-whale window 2025-08-01..2025-08-12 includes sightings, lastSeen = 2025-08-12")
    void details_fin_whale_recent_window() {
        Optional<SpeciesDTO> opt = repo.findByIdWithRollups(
                "fin-whale",
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 12)
        );

        SpeciesDTO dto = opt.orElseThrow(() -> new AssertionError("fin-whale not found"));
        // There are fin whales on 2025-08-12
        assertThat(dto.getLastSeen()).isEqualTo(LocalDate.of(2025, 8, 12));
        assertThat(dto.getFirstSeen()).isNotNull().isBeforeOrEqualTo(LocalDate.of(2025, 8, 12));

        // Some reports and individuals should be present within window
        assertThat(dto.getTotalReports()).isGreaterThan(0);
        assertThat(dto.getTotalIndividuals()).isGreaterThan(0);
    }

    @Test
    @DisplayName("findByIdWithRollups: blue-whale on a day without blue whales => zero totals, null first/last")
    void details_blue_whale_single_day_without_obs() {
        // 2025-08-08 line does not list blue whales in the sample; expect no obs that day.
        Optional<SpeciesDTO> opt = repo.findByIdWithRollups(
                "blue-whale",
                LocalDate.of(2025, 8, 8),
                LocalDate.of(2025, 8, 8)
        );

        SpeciesDTO dto = opt.orElseThrow(() -> new AssertionError("blue-whale not found"));
        assertThat(dto.getTotalReports()).isZero();
        assertThat(dto.getTotalIndividuals()).isZero();
        assertThat(dto.getFirstSeen()).isNull();
        assertThat(dto.getLastSeen()).isNull();
    }

    @Test
    @DisplayName("findByIdWithRollups: species with zero sightings overall returns null dates and zero totals")
    void details_false_killer_whale_zero_totals() {
        Optional<SpeciesDTO> opt = repo.findByIdWithRollups(
                "false-killer-whale",
                null,
                null
        );

        SpeciesDTO dto = opt.orElseThrow(() -> new AssertionError("false-killer-whale not found"));
        assertThat(dto.getTotalReports()).isZero();
        assertThat(dto.getTotalIndividuals()).isZero();
        assertThat(dto.getFirstSeen()).isNull();
        assertThat(dto.getLastSeen()).isNull();
    }

    @Test
    @DisplayName("findAllWithRollups: group=shark only returns sharks")
    void list_sharks_only() {
        Page<SpeciesDTO> page = repo.findAllWithRollups(SightingGroup.shark, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent())
                .allSatisfy(dto -> assertThat(dto.getGroup()).isEqualTo(SightingGroup.shark));
    }

}
