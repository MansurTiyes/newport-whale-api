package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.model.Species;
import com.mansurtiyes.newportwhaleapi.repository.SpeciesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(InMemorySpeciesResolver.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class InMemorySpeciesResolverIntegrationTesting {

    @Autowired SpeciesRepository speciesRepository;
    @Autowired InMemorySpeciesResolver resolver;

    @BeforeEach
    void setUp() {
        resolver.reload();
    }

    @Test
    void reloadRetrievesAllAliases() {
        List<Species> species = speciesRepository.findAll();
        long expectedAliasCount = species.stream()
                .filter(s -> s.getAliases() != null)
                .mapToLong(s -> s.getAliases().stream().filter(a -> a != null && !a.isBlank()).count())
                .sum();


        assertThat(expectedAliasCount).isEqualTo(61L);
    }

    @Test
    void resolves_allWhaleAliases_toCorrectIds() {
        // Map of expected species IDs to all their aliases from DB schema
        Map<String, List<String>> whaleAliases = Map.of(
                "gray-whale", List.of("gray", "gray whale", "gray whales", "grey", "grey whales"),
                "blue-whale", List.of("blue", "blue whale", "blue whales"),
                "fin-whale", List.of("fin whale", "fin whales", "fin"),
                "humpback-whale", List.of("humpback", "humpback whale", "humpback whales", "humpbacks"),
                "minke-whale", List.of("minke", "minke whale", "minkes", "minke whales"),
                "orca", List.of("orca", "killer whale", "killer whales"),
                "brydes-whale", List.of("bryde", "bryde's whale", "brydes whale", "bryde's whales")
        );

        whaleAliases.forEach((expectedId, aliases) -> {
            for (String alias : aliases) {
                Optional<String> resolved = resolver.resolve(alias);
                assertThat(resolved)
                        .as("Alias '%s' should resolve to '%s'", alias, expectedId)
                        .isPresent()
                        .hasValue(expectedId);
            }
        });
    }

    @Test
    void resolves_allDolphinAliases_toCorrectIds() {
        Map<String, List<String>> dolphinAliases = Map.of(
                "common-dolphin", List.of(
                        "common dolphin", "common dolphins", "common"
                ),
                "bottlenose-dolphin", List.of(
                        "bottlenose dolphin", "bottlenose dolphins", "bottlenose"
                ),
                "pacific-white-sided-dolphin", List.of(
                        "pws dolphin",
                        "pacific white-sided dolphin",
                        "pacific white sided dolphin",
                        "white-sided dolphin",
                        "white sided dolphin"
                ),
                "rissos-dolphin", List.of(
                        "risso",
                        "risso's dolphin",
                        "risso's dolphins",
                        "rissos dolphin",
                        "risso dolphin"
                )
        );

        dolphinAliases.forEach((expectedId, aliases) -> {
            for (String alias : aliases) {
                assertThat(resolver.resolve(alias))
                        .as("Alias '%s' should resolve to '%s'", alias, expectedId)
                        .isPresent()
                        .hasValue(expectedId);
            }
        });
    }

    @Test
    void resolves_allSharkAliases_toCorrectIds() {
        Map<String, List<String>> sharkAliases = Map.of(
                "mako-shark", List.of(
                        "mako shark", "mako", "mako sharks"
                ),
                "thresher-shark", List.of(
                        "thresher shark", "thresher", "thresher sharks"
                ),
                "hammerhead-shark", List.of(
                        "hammerhead shark", "hammerhead", "hammerhead sharks"
                ),
                "white-shark", List.of(
                        "white shark", "white sharks", "great white", "great white shark"
                )
        );

        sharkAliases.forEach((expectedId, aliases) -> {
            for (String alias : aliases) {
                assertThat(resolver.resolve(alias))
                        .as("Alias '%s' should resolve to '%s'", alias, expectedId)
                        .isPresent()
                        .hasValue(expectedId);
            }
        });
    }

    @Test
    void resolves_allFishAliases_toCorrectIds() {
        Map<String, List<String>> fishAliases = Map.of(
                "sunfish", List.of(
                        "sunfish", "ocean sunfish", "mola mola"
                )
        );

        fishAliases.forEach((expectedId, aliases) -> {
            for (String alias : aliases) {
                assertThat(resolver.resolve(alias))
                        .as("Alias '%s' should resolve to '%s'", alias, expectedId)
                        .isPresent()
                        .hasValue(expectedId);
            }
        });
    }

    @Test
    void resolves_allOtherGroupAliases_toCorrectIds() {
        Map<String, List<String>> otherAliases = Map.of(
                "false-killer-whale", List.of(
                        "false killer whale", "false-killer whale", "false killer"
                )
        );

        otherAliases.forEach((expectedId, aliases) -> {
            for (String alias : aliases) {
                assertThat(resolver.resolve(alias))
                        .as("Alias '%s' should resolve to '%s'", alias, expectedId)
                        .isPresent()
                        .hasValue(expectedId);
            }
        });
    }


}
