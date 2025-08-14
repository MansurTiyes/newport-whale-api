package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.model.Species;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class SpeciesRepositoryTest {

    @Autowired
    SpeciesRepository speciesRepository;

    @Test
    void testFindAllReturnsSpecies() {
        List<Species> species = speciesRepository.findAll();

        // Sanity checks
        assertThat(species).isNotNull();
        assertThat(species).isNotEmpty();

        // Print ID, common name, and aliases
        species.forEach(s -> {
            System.out.println("ID: " + s.getId());
            System.out.println("Common Name: " + s.getCommonName());
            System.out.println("Aliases: " + String.join(", ", s.getAliases()));
            System.out.println("--------------------------------------------------");
        });
    }
}
