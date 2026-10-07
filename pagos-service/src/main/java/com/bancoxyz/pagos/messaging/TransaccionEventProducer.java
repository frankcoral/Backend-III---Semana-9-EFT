package com.bancoxyz.pagos.messaging;

import com.bancoxyz.pagos.event.TransaccionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class TransaccionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public TransaccionEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public boolean publicar(TransaccionEvent evento) {
        return circuitBreakerFactory.create("pagosKafka").run(
                () -> {
                    try {
                        kafkaTemplate.send(
                                KafkaTopics.TRANSACCIONES_COMPLETADAS,
                                clave(evento),
                                evento).get(5, TimeUnit.SECONDS);
                        log.info("TRANSACCION_EVENTO_PUBLICADO -> referencia={}, tipo={}, eventId={}",
                                evento.referencia(), evento.tipoTransaccion(), evento.eventId());
                        return true;
                    } catch (Exception ex) {
                        throw new IllegalStateException("No fue posible publicar evento Kafka", ex);
                    }
                },
                throwable -> {
                    log.warn("KAFKA_FALLBACK -> evento pendiente para reintento. referencia={}, causa={}",
                            evento.referencia(), throwable.getMessage());
                    return false;
                });
    }

    private String clave(TransaccionEvent evento) {
        if (evento.cuentaOrigenId() != null) {
            return String.valueOf(evento.cuentaOrigenId());
        }
        return String.valueOf(evento.cuentaDestinoId());
    }
}
