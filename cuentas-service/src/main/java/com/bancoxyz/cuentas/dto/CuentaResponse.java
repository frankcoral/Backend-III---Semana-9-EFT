package com.bancoxyz.cuentas.dto;

import com.bancoxyz.cuentas.model.EstadoCuenta;
import com.bancoxyz.cuentas.model.TipoCuenta;
import java.math.BigDecimal;
import java.time.Instant;

public record CuentaResponse(
                Long id,
                String numeroCuenta,
                Long clienteId,
                TipoCuenta tipo,
                EstadoCuenta estado,
                BigDecimal saldo,
                String moneda,
                Instant creadaEn,
                Instant actualizadaEn) {
}
