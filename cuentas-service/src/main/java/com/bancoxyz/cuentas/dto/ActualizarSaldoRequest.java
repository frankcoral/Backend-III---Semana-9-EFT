package com.bancoxyz.cuentas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ActualizarSaldoRequest(
                @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal nuevoSaldo) {
}
