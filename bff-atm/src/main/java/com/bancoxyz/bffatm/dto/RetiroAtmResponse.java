package com.bancoxyz.bffatm.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RetiroAtmResponse(
        String canal,
        UUID transaccionId,
        String referencia,
        String estado,
        Long cuentaId,
        BigDecimal monto,
        Instant procesadaEn,
        boolean eventoPublicado,
        String mensaje) {
}
