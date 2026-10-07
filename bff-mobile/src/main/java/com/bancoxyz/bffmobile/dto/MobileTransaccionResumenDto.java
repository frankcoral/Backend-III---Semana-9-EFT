package com.bancoxyz.bffmobile.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MobileTransaccionResumenDto(
        String referencia,
        String tipo,
        String estado,
        BigDecimal monto,
        Instant procesadaEn) {
}
