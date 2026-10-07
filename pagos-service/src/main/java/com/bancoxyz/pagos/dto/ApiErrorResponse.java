package com.bancoxyz.pagos.dto;

import java.time.Instant;

public record ApiErrorResponse(
                Instant timestamp,
                int status,
                String error,
                String mensaje,
                String path) {
}
