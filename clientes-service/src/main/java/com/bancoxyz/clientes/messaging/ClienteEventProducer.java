package com.bancoxyz.clientes.messaging;

import com.bancoxyz.clientes.event.ClienteEvent;
import com.bancoxyz.clientes.exception.DependenciaNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ClienteEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ClienteEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ClienteEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    /**
     * Si Kafka no está disponible la operación transaccional del cliente falla y
     * se revierte, evitando confirmar cambios cuyo evento obligatorio se perdió.
     */
    public void publicar(ClienteEvent evento) {
        circuitBreakerFactory.create("clientesKafka").run(
                () -> {
                    try {
                        kafkaTemplate.send(
                                KafkaTopics.CLIENTES_EVENTOS,
                                String.valueOf(evento.clienteId()),
                                evento).get(5, TimeUnit.SECONDS);
                        log.info("CLIENTE_EVENTO_PUBLICADO -> tipo={}, clienteId={}, eventId={}",
                                evento.tipoEvento(), evento.clienteId(), evento.eventId());
                        return true;
                    } catch (Exception ex) {
                        throw new DependenciaNoDisponibleException("No fue posible publicar el evento de cliente", ex);
                    }
                },
                throwable -> {
                    throw new DependenciaNoDisponibleException(
                            "Kafka no disponible: la operación de cliente se revierte para mantener consistencia",
                            throwable);
                });
    }
}
