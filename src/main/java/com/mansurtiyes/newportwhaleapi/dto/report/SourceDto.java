package com.mansurtiyes.newportwhaleapi.dto.report;

import java.time.OffsetDateTime;

public record SourceDto(String url, OffsetDateTime fetchedAt) {
}
