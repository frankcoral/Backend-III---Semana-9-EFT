package com.bancoxyz.bffmobile.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MobileTransaccionDto(
        UUID id,
        String referencia,
        String tipo,
        String estado,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String beneficiario,
        String descripcion,
        Instant creadaEn,
        Instant procesadaEn,
        boolean eventoPublicado,
        String detalleError) {
}
