package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.model.Species;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeciesRepository extends JpaRepository<Species, String> {

    @Override
    List<Species> findAll();

}
