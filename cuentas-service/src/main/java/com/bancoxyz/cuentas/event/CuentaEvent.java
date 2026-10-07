package com.bancoxyz.cuentas.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CuentaEvent(
                UUID eventId,
                String tipoEvento,
                Long cuentaId,
                String numeroCuenta,
                Long clienteId,
                BigDecimal saldo,
                String estado,
                Instant fechaHora) {
}
