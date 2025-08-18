package com.mansurtiyes.newportwhaleapi.ingest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class CheckSumUtil {

    private CheckSumUtil() {
        // utility class, no instances
    }

    /**
     * Generate a stable UUID checksum from a ParsedReport's canonical string.
     *
     * @param canonical the canonical JSON string of the report
     * @return deterministic UUID checksum
     */
    public static UUID checksumFromCanonical(String canonical) {
        if (canonical == null || canonical.isBlank()) {
            throw new IllegalArgumentException("Canonical string cannot be null or blank");
        }
        return UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }
}
