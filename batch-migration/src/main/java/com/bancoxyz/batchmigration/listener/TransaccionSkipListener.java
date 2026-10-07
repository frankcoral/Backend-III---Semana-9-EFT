package com.bancoxyz.batchmigration.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import com.bancoxyz.batchmigration.model.Transaccion;

/**
 * Listener encargado de registrar los elementos omitidos
 * durante la ejecución del Job de transacciones.
 *
 * Permite mantener trazabilidad de:
 *
 * READ -> problemas leyendo el CSV.
 * PROCESS -> problemas durante validación.
 * WRITE -> problemas al persistir.
 */
@Component
public class TransaccionSkipListener
        implements SkipListener<Transaccion, Transaccion> {

    private static final Logger log = LoggerFactory.getLogger(
            TransaccionSkipListener.class);

    private static final Path ERROR_FILE = Path.of(
            "output",
            "errores_transacciones.csv");

    public TransaccionSkipListener() {
        reset();
    }

    public synchronized void reset() {

        try {

            Files.createDirectories(
                    ERROR_FILE.getParent());

            Files.writeString(
                    ERROR_FILE,
                    "etapa;id_transaccion;registro;motivo\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("ARCHIVO ERRORES TRANSACCIONES REINICIADO -> {}", ERROR_FILE);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "No se pudo crear el archivo "
                            + "de errores de transacciones.",
                    exception);
        }
    }

    /**
     * Error producido durante la lectura del CSV.
     */
    @Override
    public void onSkipInRead(Throwable throwable) {

        String registro = "NO_DISPONIBLE";

        if (throwable instanceof FlatFileParseException parseException) {

            registro = parseException.getInput();
        }

        escribirError(
                "READ",
                "SIN_ID",
                registro,
                throwable.getMessage());
    }

    /**
     * Error producido durante ItemProcessor.
     */
    @Override
    public void onSkipInProcess(
            Transaccion transaccion,
            Throwable throwable) {

        String id = transaccion == null
                || transaccion.getId() == null
                        ? "SIN_ID"
                        : transaccion.getId().toString();

        String registro = transaccion == null
                ? "NO_DISPONIBLE"
                : "fecha="
                        + transaccion.getFecha()
                        + ", monto="
                        + transaccion.getMonto()
                        + ", tipo="
                        + transaccion.getTipo();

        escribirError(
                "PROCESS",
                id,
                registro,
                throwable.getMessage());
    }

    /**
     * Error producido durante ItemWriter.
     */
    @Override
    public void onSkipInWrite(
            Transaccion transaccion,
            Throwable throwable) {

        String id = transaccion == null
                || transaccion.getId() == null
                        ? "SIN_ID"
                        : transaccion.getId().toString();

        escribirError(
                "WRITE",
                id,
                "Error al persistir transaccion",
                throwable.getMessage());
    }

    /**
     * synchronized evita que los tres hilos escriban
     * simultáneamente sobre el archivo de errores.
     */
    private synchronized void escribirError(
            String etapa,
            String id,
            String registro,
            String motivo) {

        try {

            String linea = limpiar(etapa)
                    + ";"
                    + limpiar(id)
                    + ";"
                    + limpiar(registro)
                    + ";"
                    + limpiar(motivo)
                    + System.lineSeparator();

            Files.writeString(
                    ERROR_FILE,
                    linea,
                    StandardOpenOption.APPEND);

            log.warn(
                    "[{}] SKIP LISTENER -> "
                            + "Etapa={}, id={}, motivo={}",
                    Thread.currentThread().getName(),
                    etapa,
                    id,
                    motivo);

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "No se pudo registrar "
                            + "el error omitido.",
                    exception);
        }
    }

    private String limpiar(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", ",")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}