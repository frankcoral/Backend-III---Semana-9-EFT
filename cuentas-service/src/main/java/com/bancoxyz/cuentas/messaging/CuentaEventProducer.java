package com.bancoxyz.cuentas.messaging;

import com.bancoxyz.cuentas.event.CuentaEvent;
import com.bancoxyz.cuentas.exception.DependenciaNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class CuentaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(CuentaEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CuentaEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public void publicar(CuentaEvent evento) {
        circuitBreakerFactory.create("cuentasKafka").run(
                () -> {
                    try {
                        kafkaTemplate.send(
                                KafkaTopics.CUENTAS_EVENTOS,
                                String.valueOf(evento.cuentaId()),
                                evento).get(5, TimeUnit.SECONDS);
                        log.info("CUENTA_EVENTO_PUBLICADO -> tipo={}, cuentaId={}, eventId={}",
                                evento.tipoEvento(), evento.cuentaId(), evento.eventId());
                        return true;
                    } catch (Exception ex) {
                        throw new DependenciaNoDisponibleException("No fue posible publicar el evento de cuenta en Kafka", ex);
                    }
                },
                throwable -> {
                    throw new DependenciaNoDisponibleException(
                            "Kafka no disponible: la operacion de cuenta se revierte para mantener consistencia",
                            throwable);
                });
    }
}
