package com.bancoxyz.batchmigration.processor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.exception.TransaccionInvalidaException;
import com.bancoxyz.batchmigration.model.Transaccion;

@Component
public class TransaccionProcessor
                implements ItemProcessor<Transaccion, Transaccion> {

        private static final Logger log = LoggerFactory.getLogger(TransaccionProcessor.class);

        /*
         * Colección segura para ejecución con múltiples hilos.
         */
        private final Set<String> transaccionesProcesadas = ConcurrentHashMap.newKeySet();


        public void reset() {
                transaccionesProcesadas.clear();
                log.info("ESTADO PROCESSOR TRANSACCIONES REINICIADO para una nueva ejecucion del Job");
        }

        @Override
        public Transaccion process(Transaccion transaccion) {

                String hilo = Thread.currentThread().getName();

                log.info(
                                "[{}] ENTRADA -> id={}, fecha={}, monto={}, tipo={}",
                                hilo,
                                transaccion.getId(),
                                transaccion.getFecha(),
                                transaccion.getMonto(),
                                transaccion.getTipo());

                /*
                 * Normalización del tipo.
                 */
                if (transaccion.getTipo() != null) {
                        transaccion.setTipo(
                                        transaccion.getTipo()
                                                        .trim()
                                                        .toLowerCase());
                }

                /*
                 * Datos estructuralmente incorrectos.
                 *
                 * Estos registros serán gestionados por
                 * TransaccionSkipPolicy y TransaccionSkipListener.
                 */
                if (transaccion.getFecha() == null) {

                        throw new TransaccionInvalidaException(
                                        "Fecha faltante o invalida");
                }

                if (transaccion.getMonto() == null) {

                        throw new TransaccionInvalidaException(
                                        "Monto faltante");
                }

                if (transaccion.getTipo() == null
                                || transaccion.getTipo().isBlank()) {

                        throw new TransaccionInvalidaException(
                                        "Tipo de transaccion faltante");
                }

                if (!transaccion.getTipo().equals("debito")
                                && !transaccion.getTipo().equals("credito")) {

                        throw new TransaccionInvalidaException(
                                        "Tipo de transaccion no valido: "
                                                        + transaccion.getTipo());
                }

                /*
                 * Anomalías de negocio.
                 *
                 * Estas sí se persisten para conservar trazabilidad.
                 */
                StringBuilder motivos = new StringBuilder();

                if (transaccion.getMonto()
                                .compareTo(BigDecimal.ZERO) <= 0) {

                        agregarMotivo(
                                        motivos,
                                        "Monto negativo o cero");
                }

                /*
                 * Detección de duplicados compatible con
                 * procesamiento multihilo.
                 */
                String firma = transaccion.getFecha()
                                + "|"
                                + transaccion.getMonto()
                                                .stripTrailingZeros()
                                                .toPlainString()
                                + "|"
                                + transaccion.getTipo();

                if (!transaccionesProcesadas.add(firma)) {

                        agregarMotivo(
                                        motivos,
                                        "Transaccion duplicada");
                }

                if (motivos.length() > 0) {

                        transaccion.setAnomala(true);
                        transaccion.setMotivoAnomalia(
                                        motivos.toString());

                        log.warn(
                                        "[{}] TRANSFORMACION -> "
                                                        + "id={} marcada ANOMALA: {}",
                                        hilo,
                                        transaccion.getId(),
                                        motivos);

                } else {

                        transaccion.setAnomala(false);
                        transaccion.setMotivoAnomalia(null);

                        log.info(
                                        "[{}] TRANSFORMACION -> "
                                                        + "id={} VALIDADA",
                                        hilo,
                                        transaccion.getId());
                }

                log.info(
                                "[{}] SALIDA PROCESSOR -> "
                                                + "id={}, tipo={}, monto={}, anomala={}",
                                hilo,
                                transaccion.getId(),
                                transaccion.getTipo(),
                                transaccion.getMonto(),
                                transaccion.isAnomala());

                return transaccion;
        }

        private void agregarMotivo(
                        StringBuilder motivos,
                        String motivo) {

                if (motivos.length() > 0) {
                        motivos.append("; ");
                }

                motivos.append(motivo);
        }
}