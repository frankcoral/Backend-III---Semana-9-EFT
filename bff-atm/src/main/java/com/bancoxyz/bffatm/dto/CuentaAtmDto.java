package com.bancoxyz.bffatm.dto;

import java.math.BigDecimal;

public record CuentaAtmDto(
        Long id,
        String numeroCuenta,
        String tipo,
        String estado,
        BigDecimal saldo,
        String moneda) {
}
