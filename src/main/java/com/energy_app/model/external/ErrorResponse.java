package com.energy_app.model.external;

import java.time.OffsetDateTime;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int statusCode,
        String error,
        String message
) {
}
