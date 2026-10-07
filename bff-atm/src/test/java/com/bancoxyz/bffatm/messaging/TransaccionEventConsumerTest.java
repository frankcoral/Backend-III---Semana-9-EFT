package com.bancoxyz.bffatm.messaging;

import com.bancoxyz.bffatm.event.TransaccionEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransaccionEventConsumerTest {

    @Test
    void procesaEventoDeTransaccionCompletada() {
        TransaccionEventConsumer consumer = new TransaccionEventConsumer();
        TransaccionEvent event = new TransaccionEvent(
                UUID.randomUUID(), "TRANSACCION_COMPLETADA", UUID.randomUUID(), "TRX-001",
                "RETIRO", "APROBADA", 1L, null, new BigDecimal("1000"), Instant.now());

        ConsumerRecord<String, TransaccionEvent> record =
                new ConsumerRecord<>("banco.transacciones.completadas", 0, 0L, "1", event);

        assertDoesNotThrow(() -> consumer.consumir(record));
    }
}
