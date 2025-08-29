package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.dto.report.ObservationDto;
import com.mansurtiyes.newportwhaleapi.dto.report.ReportDTO;
import com.mansurtiyes.newportwhaleapi.model.ReportStatus;
import com.mansurtiyes.newportwhaleapi.model.SightingGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReportReadRepositoryImplIntegrationTests {

    @Autowired
    private ReportReadRepository repo;

    // --- helpers ---
    private static Map<String, Integer> toMap(List<ObservationDto> list) {
        return list.stream().collect(Collectors.toMap(ObservationDto::speciesId, ObservationDto::count));
    }

    private static List<LocalDate> dates(Page<ReportDTO> page) {
        return page.getContent().stream().map(ReportDTO::getDate).toList();
    }

    // -------------------------------------------------------
    // 1) Basic window + paging + sort-by-date ascending
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: window + paging (Aug 1–12 2025) sorted by date ASC")
    void findReports_window_paging_sortedByDate() {
        LocalDate start = LocalDate.of(2025, 8, 1);
        LocalDate end   = LocalDate.of(2025, 8, 12);
        Pageable pg = PageRequest.of(0, 5, Sort.by(Sort.Order.asc("date")));

        Page<ReportDTO> page = repo.findReports(start, end, null, null, null, null, pg);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getContent()).isNotEmpty();

        // first page’s first record should be the window start (08-01) when sorted ASC
        assertThat(page.getContent().get(0).getDate()).isEqualTo(start);
    }

    // -------------------------------------------------------
    // 2) speciesId filter (Blue whale) within a window
    // Expect only dates that contain blue-whale sightings
    // (From HTML: 08/05 has 5 blue whales; 08/06 has 2 blue whales)
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: speciesId=blue-whale within Aug 1–10 returns only blue-whale days")
    void findReports_species_blueWhale_window() {
        LocalDate start = LocalDate.of(2025, 8, 1);
        LocalDate end   = LocalDate.of(2025, 8, 10);
        Pageable pg = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("date")));

        Page<ReportDTO> page = repo.findReports(start, end, "blue-whale", null, null, null, pg);

        // Expect 08/05 and 08/06 (per source)
        List<LocalDate> got = dates(page);
        assertThat(got).contains(LocalDate.of(2025, 8, 5), LocalDate.of(2025, 8, 6));

        // Every item’s observations should include blue-whale
        assertThat(page.getContent())
                .allSatisfy(r -> assertThat(r.getObservations())
                        .extracting(ObservationDto::speciesId)
                        .contains("blue-whale"));
    }

    // -------------------------------------------------------
    // 3) group=shark with hasSightings=false
    // Return days where sharks were NOT seen in June 2025.
    // (From HTML: 06/08 has 2 mako sharks -> should be excluded)
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: group=shark & hasSightings=false excludes shark days (e.g., 2025-06-08)")
    void findReports_groupShark_hasSightingsFalse() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end   = LocalDate.of(2025, 6, 30);
        Pageable pg = PageRequest.of(0, 100, Sort.by("date").ascending());

        Page<ReportDTO> page = repo.findReports(start, end, null, SightingGroup.shark, false, null, pg);
        List<LocalDate> got = dates(page);

        // Should NOT contain 2025-06-08 (2 Mako Sharks that day)
        assertThat(got).doesNotContain(LocalDate.of(2025, 6, 8));

        // But should include normal June days with no shark sightings, e.g. 2025-06-04
        assertThat(got).contains(LocalDate.of(2025, 6, 4));
    }

    // -------------------------------------------------------
    // 4) status=bad_weather in April 2025
    // (From HTML: 04/01 and 04/02 and 04/26 are Bad Weather days)
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: status=bad_weather returns April bad-weather dates with empty observations")
    void findReports_status_badWeather_window() {
        LocalDate start = LocalDate.of(2025, 4, 1);
        LocalDate end   = LocalDate.of(2025, 4, 30);
        Pageable pg = PageRequest.of(0, 100, Sort.by("date").ascending());

        Page<ReportDTO> page = repo.findReports(start, end, null, null, null, ReportStatus.bad_weather, pg);
        List<LocalDate> got = dates(page);

        assertThat(got).contains(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 2), LocalDate.of(2025, 4, 26));
        assertThat(page.getContent())
                .allSatisfy(r -> {
                    assertThat(r.getStatus()).isEqualTo(ReportStatus.bad_weather);
                    assertThat(r.getObservations()).isEmpty();
                });
    }

    // -------------------------------------------------------
    // 5) findByDate OK day with observations (08/12/2025)
    // Expect Fin=4, Sunfish(Mola mola)=1, Common-dolphin=2855, Bottlenose-dolphin=195
    // -------------------------------------------------------
    @Test
    @DisplayName("findByDate: 2025-08-12 returns expected OK report with observations")
    void findByDate_ok_withObservations() {
        Optional<ReportDTO> opt = repo.findByDate(LocalDate.of(2025, 8, 12));
        assertThat(opt).isPresent();

        ReportDTO r = opt.get();
        assertThat(r.getStatus()).isEqualTo(ReportStatus.ok);
        assertThat(r.getTours()).isEqualTo(14);

        Map<String,Integer> m = toMap(r.getObservations());
        assertThat(m).containsEntry("fin-whale", 4);
        assertThat(m).containsEntry("sunfish", 1);               // Mola mola
        assertThat(m).containsEntry("common-dolphin", 2855);
        assertThat(m).containsEntry("bottlenose-dolphin", 195);
    }

    // -------------------------------------------------------
    // 6) findByDate bad weather day (e.g., 04/02/2025)
    // -------------------------------------------------------
    @Test
    @DisplayName("findByDate: bad weather date returns empty observations")
    void findByDate_badWeather() {
        Optional<ReportDTO> opt = repo.findByDate(LocalDate.of(2025, 4, 2));
        assertThat(opt).isPresent();

        ReportDTO r = opt.get();
        assertThat(r.getStatus()).isEqualTo(ReportStatus.bad_weather);
        assertThat(r.getObservations()).isEmpty();
        // tours are typically 0 on bad-weather days, but assert >=0 for safety
        assertThat(r.getTours()).isGreaterThanOrEqualTo(0);
    }

    // -------------------------------------------------------
    // 7) hasSightings=true should exclude bad-weather (no obs) from a tiny window (Apr 1–3)
    // Expect to include a normal sighting day in that window (04/03) and exclude 04/01, 04/02
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: hasSightings=true excludes empty/bad-weather rows")
    void findReports_hasSightings_true_excludesEmptyDays() {
        LocalDate start = LocalDate.of(2025, 4, 1);
        LocalDate end   = LocalDate.of(2025, 4, 3);
        Pageable pg = PageRequest.of(0, 50, Sort.by("date").ascending());

        Page<ReportDTO> page = repo.findReports(start, end, null, null, true, null, pg);
        List<LocalDate> got = dates(page);

        assertThat(got).doesNotContain(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 2));
        assertThat(got).contains(LocalDate.of(2025, 4, 3));
    }

    // -------------------------------------------------------
    // 8) Sorting by tours DESC inside small window (07/25–07/27)
    // Tours per HTML: 07/25=14, 07/26=18, 07/27=20
    // -------------------------------------------------------
    @Test
    @DisplayName("findReports: sorting by tours DESC (2025-07-25..27)")
    void findReports_sortByTours_desc() {
        LocalDate start = LocalDate.of(2025, 7, 25);
        LocalDate end   = LocalDate.of(2025, 7, 27);
        Pageable pg = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("tours")));

        Page<ReportDTO> page = repo.findReports(start, end, null, null, null, null, pg);

        // Expected order: 07/27 (20), 07/26 (18), 07/25 (14)
        List<LocalDate> got = dates(page);
        assertThat(got).containsExactly(
                LocalDate.of(2025, 7, 27),
                LocalDate.of(2025, 7, 26),
                LocalDate.of(2025, 7, 25)
        );
    }

    // -------------------------------------------------------
    // 9) findByDate: missing date -> Optional.empty()
    // -------------------------------------------------------
    @Test
    @DisplayName("findByDate: missing date returns empty")
    void findByDate_missing_returnsEmpty() {
        Optional<ReportDTO> opt = repo.findByDate(LocalDate.of(1999, 1, 1));
        assertThat(opt).isEmpty();
    }

}
