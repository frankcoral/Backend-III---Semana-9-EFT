package com.bancoxyz.bffweb.dto;

import java.util.List;

public record WebClienteDetalleDto(
        String canal,
        String mensaje,
        WebClienteDto cliente,
        List<WebCuentaDto> cuentas,
        List<WebTransaccionDto> transacciones) {
}
