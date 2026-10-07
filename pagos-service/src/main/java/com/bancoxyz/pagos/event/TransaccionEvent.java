package com.bancoxyz.pagos.event;

import com.bancoxyz.pagos.model.EstadoTransaccion;
import com.bancoxyz.pagos.model.TipoTransaccion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransaccionEvent(
                UUID eventId,
                String tipoEvento,
                UUID transaccionId,
                String referencia,
                TipoTransaccion tipoTransaccion,
                EstadoTransaccion estado,
                Long cuentaOrigenId,
                Long cuentaDestinoId,
                BigDecimal monto,
                Instant fechaHora) {
}
