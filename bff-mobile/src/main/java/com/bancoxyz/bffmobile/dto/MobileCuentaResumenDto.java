package com.bancoxyz.bffmobile.dto;

import java.math.BigDecimal;

public record MobileCuentaResumenDto(
        Long id,
        String numeroCuenta,
        String tipo,
        BigDecimal saldo,
        String moneda) {
}
