package com.bancoxyz.pagos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PagoRequest(
                @NotNull Long cuentaId,
                @NotNull @DecimalMin(value = "0.01") BigDecimal monto,
                @NotBlank @Size(max = 120) String beneficiario,
                @Size(max = 255) String descripcion) {
}
