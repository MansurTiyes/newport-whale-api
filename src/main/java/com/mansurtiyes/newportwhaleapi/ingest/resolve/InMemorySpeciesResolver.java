package com.mansurtiyes.newportwhaleapi.ingest.resolve;

import com.mansurtiyes.newportwhaleapi.model.Species;
import com.mansurtiyes.newportwhaleapi.repository.SpeciesRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemorySpeciesResolver implements SpeciesResolver {
    private final SpeciesRepository speciesRepository;

    // Volatile + copy-on-write for atomic refreshes
    private volatile Map<String, String> aliasToId = Map.of();

    public InMemorySpeciesResolver(SpeciesRepository speciesRepository) {
        this.speciesRepository = speciesRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadOnStartup() {
        reload();
    }

    public void reload() {
        List<Species> all = speciesRepository.findAll();
        Map<String, String> map = new HashMap<>();

        // for each species
        for (Species s : all) {
            String id = s.getId();      // get it's id (will be value for alias as key)

            if (s.getAliases() != null) { // if there are alias (should always be the case)
                for (String alias : s.getAliases()) {       // for each alias
                    if (alias == null || alias.isBlank()) continue;     // if alias is null or not present (should never happen)
                    String norm = TextNormalizer.norm(alias);           // normalize it
                    map.put(norm, id);                  // add entry in the map as a alias:id
                }
            }
        }

        aliasToId = Map.copyOf(map);
    }

    @Override
    public Optional<String> resolve(String rawLabel) {
        if (rawLabel == null) return Optional.empty();
        String key = TextNormalizer.norm(rawLabel);
        String id = aliasToId.get(key);
        return Optional.ofNullable(id);
    }
}
