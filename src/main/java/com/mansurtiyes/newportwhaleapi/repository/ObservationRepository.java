package com.mansurtiyes.newportwhaleapi.repository;

import com.mansurtiyes.newportwhaleapi.model.Observation;
import com.mansurtiyes.newportwhaleapi.model.ObservationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObservationRepository
        extends JpaRepository<Observation, ObservationId>, ObservationBulkRepository {

    // you still get find/save/saveAll from JpaRepository if needed
}
