package com.bancoxyz.pagos.dto;

import java.math.BigDecimal;

public record CuentaResumen(
                Long id,
                String numeroCuenta,
                Long clienteId,
                String tipo,
                String estado,
                BigDecimal saldo,
                String moneda) {
}
