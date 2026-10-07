package com.bancoxyz.bffatm.dto;

import java.math.BigDecimal;

public record PagoRetiroRequestDto(
        Long cuentaId,
        BigDecimal monto,
        String descripcion) {
}
