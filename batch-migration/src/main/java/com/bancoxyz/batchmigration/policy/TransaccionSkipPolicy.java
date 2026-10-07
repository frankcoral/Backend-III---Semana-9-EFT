package com.bancoxyz.batchmigration.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.exception.TransaccionInvalidaException;

/**
 * Política personalizada para omitir errores de calidad de datos controlados.
 *
 * En Semana 3 el archivo contiene 1000 registros y una cantidad importante de
 * datos legacy inconsistentes. El límite es configurable para que el Job pueda
 * recorrer el archivo completo sin convertir un problema esperado de calidad
 * de datos en un fallo técnico del proceso.
 */
@Component
public class TransaccionSkipPolicy implements SkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(TransaccionSkipPolicy.class);

    @Value("${batch.skip.max-errors:1000}")
    private long limiteOmisiones;

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {

        boolean errorControlado = throwable instanceof TransaccionInvalidaException
                || throwable instanceof FlatFileParseException;

        boolean dentroDelLimite = skipCount < 0
                || skipCount < Math.max(1, limiteOmisiones);

        if (errorControlado && dentroDelLimite) {

            log.warn(
                    "[{}] SKIP POLICY -> Registro omitido. Omisiones actuales: {}. Motivo: {}",
                    Thread.currentThread().getName(),
                    Math.max(skipCount, 0),
                    throwable.getMessage());

            return true;
        }

        if (errorControlado) {
            log.error(
                    "[{}] SKIP POLICY -> Se alcanzo el limite configurado de {} omisiones.",
                    Thread.currentThread().getName(),
                    limiteOmisiones);
        }

        // Excepciones técnicas no conocidas nunca se ocultan.
        return false;
    }
}
