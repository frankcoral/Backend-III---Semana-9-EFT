package com.bancoxyz.bffmobile.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MobileCuentaDto(
        Long id,
        String numeroCuenta,
        Long clienteId,
        String tipo,
        String estado,
        BigDecimal saldo,
        String moneda,
        Instant creadaEn,
        Instant actualizadaEn) {
}
