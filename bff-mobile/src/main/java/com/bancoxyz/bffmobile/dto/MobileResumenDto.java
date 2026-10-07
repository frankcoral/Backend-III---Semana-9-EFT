package com.bancoxyz.bffmobile.dto;

import java.util.List;

public record MobileResumenDto(
        String canal,
        String mensaje,
        MobileClienteResumenDto cliente,
        List<MobileCuentaResumenDto> cuentas,
        List<MobileTransaccionResumenDto> ultimasTransacciones) {
}
