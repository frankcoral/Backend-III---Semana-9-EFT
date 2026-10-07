package com.bancoxyz.batchmigration.processor;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.exception.MovimientoAnualInvalidoException;
import com.bancoxyz.batchmigration.model.MovimientoAnual;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<MovimientoAnual, MovimientoAnual> {

    private static final Logger log = LoggerFactory.getLogger(
            MovimientoAnualProcessor.class);

    @Override
    public MovimientoAnual process(
            MovimientoAnual movimiento) {

        String hilo = Thread.currentThread().getName();

        log.info(
                "[{}] ENTRADA ESTADOS -> "
                        + "cuenta={}, fecha={}, transaccion={}, "
                        + "monto={}, descripcion={}",
                hilo,
                movimiento.getCuentaId(),
                movimiento.getFecha(),
                movimiento.getTransaccion(),
                movimiento.getMonto(),
                movimiento.getDescripcion());

        /*
         * NORMALIZACIÓN
         */
        if (movimiento.getTransaccion() != null) {

            movimiento.setTransaccion(
                    movimiento.getTransaccion()
                            .trim()
                            .toLowerCase());
        }

        if (movimiento.getDescripcion() != null) {

            movimiento.setDescripcion(
                    movimiento.getDescripcion()
                            .trim());
        }

        /*
         * ERRORES ESTRUCTURALES
         *
         * Estos registros se omiten mediante:
         * SkipPolicy + SkipListener.
         */
        if (movimiento.getFecha() == null) {

            throw new MovimientoAnualInvalidoException(
                    "Fecha faltante");
        }

        if (movimiento.getDescripcion() == null
                || movimiento.getDescripcion().isBlank()) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL ESTADOS -> "
                            + "cuenta={} sin descripcion",
                    hilo,
                    movimiento.getCuentaId());

            throw new MovimientoAnualInvalidoException(
                    "Descripcion faltante");
        }

        if (movimiento.getMonto() == null) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL ESTADOS -> "
                            + "cuenta={} sin monto",
                    hilo,
                    movimiento.getCuentaId());

            throw new MovimientoAnualInvalidoException(
                    "Monto faltante");
        }

        if (movimiento.getTransaccion() == null
                || movimiento.getTransaccion().isBlank()) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL ESTADOS -> "
                            + "cuenta={} sin tipo de transaccion",
                    hilo,
                    movimiento.getCuentaId());

            throw new MovimientoAnualInvalidoException(
                    "Tipo de transaccion faltante");
        }

        /*
         * VALIDACIONES DE NEGOCIO
         *
         * Estos casos NO se omiten.
         * Se persisten como inválidos para mantener
         * trazabilidad en MySQL.
         */
        StringBuilder errores = new StringBuilder();

        String tipo = movimiento.getTransaccion();

        if (!tipo.equals("deposito")
                && !tipo.equals("retiro")
                && !tipo.equals("compra")) {

            agregarError(
                    errores,
                    "Tipo de transaccion no valido");
        }

        if (movimiento.getMonto()
                .compareTo(BigDecimal.ZERO) == 0) {

            agregarError(
                    errores,
                    "Monto no puede ser cero");
        }

        /*
         * Coherencia entre tipo y signo.
         */
        if ("deposito".equals(tipo)
                && movimiento.getMonto()
                        .compareTo(BigDecimal.ZERO) < 0) {

            agregarError(
                    errores,
                    "Deposito debe tener monto positivo");
        }

        if (("retiro".equals(tipo)
                || "compra".equals(tipo))
                && movimiento.getMonto()
                        .compareTo(BigDecimal.ZERO) > 0) {

            agregarError(
                    errores,
                    "Retiro o compra debe tener monto negativo");
        }

        /*
         * RESULTADO
         */
        if (errores.length() > 0) {

            movimiento.setValido(false);

            movimiento.setMotivoError(
                    errores.toString());

            log.warn(
                    "[{}] TRANSFORMACION ESTADOS -> "
                            + "cuenta={} marcada INVALIDA: {}",
                    hilo,
                    movimiento.getCuentaId(),
                    errores);

        } else {

            movimiento.setValido(true);

            movimiento.setMotivoError(null);

            log.info(
                    "[{}] TRANSFORMACION ESTADOS -> "
                            + "cuenta={} VALIDADA",
                    hilo,
                    movimiento.getCuentaId());
        }

        log.info(
                "[{}] SALIDA ESTADOS -> "
                        + "cuenta={}, transaccion={}, monto={}, valido={}",
                hilo,
                movimiento.getCuentaId(),
                movimiento.getTransaccion(),
                movimiento.getMonto(),
                movimiento.isValido());

        return movimiento;
    }

    private void agregarError(
            StringBuilder errores,
            String mensaje) {

        if (errores.length() > 0) {

            errores.append("; ");
        }

        errores.append(mensaje);
    }
}