package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import java.util.Optional;

public interface SpeciesResolver {
    // Returns canonical speciesId for a raw label if resolvable, else empty
    Optional<String> resolve(String rawLabel);

}
