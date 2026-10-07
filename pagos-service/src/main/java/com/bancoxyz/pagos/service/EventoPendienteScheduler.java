package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.model.EstadoTransaccion;
import com.bancoxyz.pagos.repository.TransaccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventoPendienteScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventoPendienteScheduler.class);

    private final TransaccionRepository repository;
    private final PagoService pagoService;

    public EventoPendienteScheduler(TransaccionRepository repository, PagoService pagoService) {
        this.repository = repository;
        this.pagoService = pagoService;
    }

    @Scheduled(fixedDelayString = "${pagos.eventos.retry-ms:30000}", initialDelayString = "${pagos.eventos.initial-delay-ms:15000}")
    public void reenviarEventosPendientes() {
        repository.findTop50ByEstadoAndEventoPublicadoFalseOrderByCreadaEnAsc(EstadoTransaccion.APROBADA)
                .forEach(tx -> {
                    if (pagoService.reintentarEvento(tx)) {
                        log.info("EVENTO_REINTENTADO_OK -> referencia={}", tx.getReferencia());
                    }
                });
    }
}
