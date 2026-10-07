package com.bancoxyz.batchmigration.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.exception.CuentaInteresInvalidaException;
import com.bancoxyz.batchmigration.model.CuentaInteres;

@Component
public class CuentaInteresProcessor
        implements ItemProcessor<CuentaInteres, CuentaInteres> {

    private static final Logger log = LoggerFactory.getLogger(
            CuentaInteresProcessor.class);

    private static final BigDecimal TASA_AHORRO = new BigDecimal("0.01");

    private static final BigDecimal TASA_PRESTAMO = new BigDecimal("0.02");

    @Override
    public CuentaInteres process(
            CuentaInteres cuenta) {

        String hilo = Thread.currentThread().getName();

        log.info(
                "[{}] ENTRADA INTERESES -> "
                        + "cuenta={}, nombre={}, saldo={}, edad={}, tipo={}",
                hilo,
                cuenta.getCuentaId(),
                cuenta.getNombre(),
                cuenta.getSaldo(),
                cuenta.getEdad(),
                cuenta.getTipo());

        /*
         * NORMALIZACIÓN
         */
        if (cuenta.getNombre() != null) {

            cuenta.setNombre(
                    cuenta.getNombre().trim());
        }

        if (cuenta.getTipo() != null) {

            cuenta.setTipo(
                    cuenta.getTipo()
                            .trim()
                            .toLowerCase());
        }

        /*
         * ERRORES ESTRUCTURALES
         *
         * Los datos obligatorios faltantes son omitidos
         * mediante SkipPolicy + SkipListener.
         */
        if (cuenta.getNombre() == null
                || cuenta.getNombre().isBlank()) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL -> "
                            + "cuenta={} sin nombre",
                    hilo,
                    cuenta.getCuentaId());

            throw new CuentaInteresInvalidaException(
                    "Nombre faltante");
        }

        if (cuenta.getSaldo() == null) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL -> "
                            + "cuenta={} sin saldo",
                    hilo,
                    cuenta.getCuentaId());

            throw new CuentaInteresInvalidaException(
                    "Saldo faltante");
        }

        if (cuenta.getEdad() == null) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL -> "
                            + "cuenta={} sin edad",
                    hilo,
                    cuenta.getCuentaId());

            throw new CuentaInteresInvalidaException(
                    "Edad faltante");
        }

        if (cuenta.getTipo() == null
                || cuenta.getTipo().isBlank()) {

            log.warn(
                    "[{}] ERROR ESTRUCTURAL -> "
                            + "cuenta={} sin tipo",
                    hilo,
                    cuenta.getCuentaId());

            throw new CuentaInteresInvalidaException(
                    "Tipo de cuenta faltante");
        }

        /*
         * VALIDACIONES DE NEGOCIO
         *
         * Estos registros sí se persisten como inválidos
         * para mantener trazabilidad.
         */
        StringBuilder errores = new StringBuilder();

        if (cuenta.getSaldo()
                .compareTo(BigDecimal.ZERO) <= 0) {

            agregarError(
                    errores,
                    "Saldo debe ser mayor a cero");
        }

        if (cuenta.getEdad() <= 0
                || cuenta.getEdad() > 120) {

            agregarError(
                    errores,
                    "Edad no valida");
        }

        /*
         * DETERMINACIÓN DE TASA
         */
        if ("ahorro".equals(cuenta.getTipo())) {

            cuenta.setTasaInteres(
                    TASA_AHORRO);

        } else if ("prestamo".equals(
                cuenta.getTipo())) {

            cuenta.setTasaInteres(
                    TASA_PRESTAMO);

        } else {

            cuenta.setTasaInteres(
                    BigDecimal.ZERO);

            agregarError(
                    errores,
                    "Tipo de cuenta no valido "
                            + "para calculo de intereses");
        }

        /*
         * REGISTRO INVÁLIDO DE NEGOCIO
         *
         * Se persiste, pero no genera interés.
         */
        if (errores.length() > 0) {

            cuenta.setValida(false);

            cuenta.setMotivoError(
                    errores.toString());

            cuenta.setInteresCalculado(
                    BigDecimal.ZERO);

            cuenta.setSaldoFinal(
                    cuenta.getSaldo());

            log.warn(
                    "[{}] TRANSFORMACION INTERESES -> "
                            + "cuenta={} marcada INVALIDA: {}",
                    hilo,
                    cuenta.getCuentaId(),
                    errores);

            log.info(
                    "[{}] SALIDA INTERESES -> "
                            + "cuenta={}, valida=false, "
                            + "tasa={}, interes={}, saldoFinal={}",
                    hilo,
                    cuenta.getCuentaId(),
                    cuenta.getTasaInteres(),
                    cuenta.getInteresCalculado(),
                    cuenta.getSaldoFinal());

            return cuenta;
        }

        /*
         * CÁLCULO DE INTERÉS
         */
        BigDecimal interes = cuenta.getSaldo()
                .multiply(
                        cuenta.getTasaInteres())
                .setScale(
                        2,
                        RoundingMode.HALF_UP);

        BigDecimal saldoFinal = cuenta.getSaldo()
                .add(interes)
                .setScale(
                        2,
                        RoundingMode.HALF_UP);

        cuenta.setInteresCalculado(
                interes);

        cuenta.setSaldoFinal(
                saldoFinal);

        cuenta.setValida(true);

        cuenta.setMotivoError(null);

        log.info(
                "[{}] TRANSFORMACION INTERESES -> "
                        + "cuenta={} VALIDADA, "
                        + "tasa={}, interes={}",
                hilo,
                cuenta.getCuentaId(),
                cuenta.getTasaInteres(),
                interes);

        log.info(
                "[{}] SALIDA INTERESES -> "
                        + "cuenta={}, valida=true, "
                        + "saldoInicial={}, saldoFinal={}",
                hilo,
                cuenta.getCuentaId(),
                cuenta.getSaldo(),
                cuenta.getSaldoFinal());

        return cuenta;
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