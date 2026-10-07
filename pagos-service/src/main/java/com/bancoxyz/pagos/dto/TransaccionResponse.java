package com.bancoxyz.pagos.dto;

import com.bancoxyz.pagos.model.EstadoTransaccion;
import com.bancoxyz.pagos.model.TipoTransaccion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransaccionResponse(
                UUID id,
                String referencia,
                TipoTransaccion tipo,
                EstadoTransaccion estado,
                Long cuentaOrigenId,
                Long cuentaDestinoId,
                BigDecimal monto,
                String beneficiario,
                String descripcion,
                Instant creadaEn,
                Instant procesadaEn,
                boolean eventoPublicado,
                String detalleError) {
}
