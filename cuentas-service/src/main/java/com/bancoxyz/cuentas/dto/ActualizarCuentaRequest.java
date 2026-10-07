package com.bancoxyz.cuentas.dto;

import com.bancoxyz.cuentas.model.TipoCuenta;
import jakarta.validation.constraints.Pattern;

public record ActualizarCuentaRequest(
                TipoCuenta tipo,
                @Pattern(regexp = "[A-Za-z]{3}") String moneda) {
}
