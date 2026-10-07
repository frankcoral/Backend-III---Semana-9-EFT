package com.bancoxyz.bffatm.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransaccionEvent(
        UUID eventId,
        String tipoEvento,
        UUID transaccionId,
        String referencia,
        String tipoTransaccion,
        String estado,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        Instant fechaHora) {
}