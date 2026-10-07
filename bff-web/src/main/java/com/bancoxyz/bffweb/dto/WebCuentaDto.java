package com.bancoxyz.bffweb.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WebCuentaDto(
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
