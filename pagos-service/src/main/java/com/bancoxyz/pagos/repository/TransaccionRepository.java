package com.bancoxyz.pagos.repository;

import com.bancoxyz.pagos.model.EstadoTransaccion;
import com.bancoxyz.pagos.model.Transaccion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {
    List<Transaccion> findTop50ByEstadoAndEventoPublicadoFalseOrderByCreadaEnAsc(EstadoTransaccion estado);

    List<Transaccion> findByCuentaOrigenIdOrCuentaDestinoIdOrderByCreadaEnDesc(
            Long origen,
            Long destino,
            Pageable pageable);
}
