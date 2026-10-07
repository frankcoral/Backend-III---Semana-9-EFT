package com.bancoxyz.cuentas.dto;

import com.bancoxyz.cuentas.model.TipoCuenta;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CrearCuentaRequest(
                @NotNull Long clienteId,
                @NotNull TipoCuenta tipo,
                @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal saldoInicial,
                @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String moneda) {
}
