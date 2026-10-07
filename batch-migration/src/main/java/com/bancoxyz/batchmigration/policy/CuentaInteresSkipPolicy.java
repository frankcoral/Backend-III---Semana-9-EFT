package com.bancoxyz.batchmigration.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.exception.CuentaInteresInvalidaException;

/**
 * Politica personalizada para errores controlados de calidad de datos del
 * archivo de intereses.
 */
@Component
public class CuentaInteresSkipPolicy implements SkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(CuentaInteresSkipPolicy.class);

    @Value("${batch.skip.max-errors:1000}")
    private long limiteOmisiones;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {

        boolean errorControlado = throwable instanceof CuentaInteresInvalidaException
                || throwable instanceof FlatFileParseException;

        boolean dentroDelLimite = skipCount < 0
                || skipCount < Math.max(1, limiteOmisiones);

        if (errorControlado && dentroDelLimite) {
            log.warn(
                    "[{}] SKIP POLICY INTERESES -> Registro omitido. Omisiones actuales: {}. Motivo: {}",
                    Thread.currentThread().getName(),
                    Math.max(skipCount, 0),
                    throwable.getMessage());
            return true;
        }

        if (errorControlado) {
            log.error(
                    "[{}] SKIP POLICY INTERESES -> Se alcanzo el limite configurado de {} omisiones.",
                    Thread.currentThread().getName(),
                    limiteOmisiones);
        }

        return false;
    }
}
