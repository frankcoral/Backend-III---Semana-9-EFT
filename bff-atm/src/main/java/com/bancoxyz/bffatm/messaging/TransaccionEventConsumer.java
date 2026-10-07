package com.bancoxyz.bffatm.messaging;

import com.bancoxyz.bffatm.event.TransaccionEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransaccionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransaccionEventConsumer.class);

    @KafkaListener(
            topics = "banco.transacciones.completadas",
            concurrency = "${app.kafka.transacciones.concurrency:3}")
    public void consumir(ConsumerRecord<String, TransaccionEvent> record) {
        TransaccionEvent event = record.value();
        log.info(
                "ATM_TRANSACCION_RECIBIDA -> topic={} partition={} offset={} key={} eventId={} "
                        + "transaccionId={} referencia={} tipo={} estado={} cuentaOrigenId={} monto={}",
                record.topic(), record.partition(), record.offset(), record.key(), event.eventId(),
                event.transaccionId(), event.referencia(), event.tipoTransaccion(), event.estado(),
                event.cuentaOrigenId(), event.monto());
    }
}
