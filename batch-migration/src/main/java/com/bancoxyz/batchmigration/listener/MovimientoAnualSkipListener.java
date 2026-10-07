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

import com.bancoxyz.batchmigration.model.MovimientoAnual;

@Component
public class MovimientoAnualSkipListener
        implements SkipListener<MovimientoAnual, MovimientoAnual> {

    private static final Logger log = LoggerFactory.getLogger(MovimientoAnualSkipListener.class);

    private static final Path ERROR_FILE = Path.of(
            "output",
            "errores_estados_anuales.csv");

    public MovimientoAnualSkipListener() {
        reset();
    }

    /**
     * Se ejecuta antes del procesamiento para evitar que cada Worker trunque el
     * archivo de errores de los otros Workers.
     */
    public synchronized void reset() {

        try {
            Files.createDirectories(ERROR_FILE.getParent());

            Files.writeString(
                    ERROR_FILE,
                    "etapa;cuenta_id;registro;motivo\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.info("ARCHIVO ERRORES ESTADOS REINICIADO -> {}", ERROR_FILE);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo crear el archivo de errores de estados anuales.",
                    exception);
        }
    }

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

    @Override
    public void onSkipInProcess(
            MovimientoAnual movimiento,
            Throwable throwable) {

        String cuentaId = movimiento == null || movimiento.getCuentaId() == null
                ? "SIN_ID"
                : movimiento.getCuentaId().toString();

        String registro = movimiento == null
                ? "NO_DISPONIBLE"
                : "fecha=" + movimiento.getFecha()
                        + ", transaccion=" + movimiento.getTransaccion()
                        + ", monto=" + movimiento.getMonto()
                        + ", descripcion=" + movimiento.getDescripcion();

        escribirError(
                "PROCESS",
                cuentaId,
                registro,
                throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(
            MovimientoAnual movimiento,
            Throwable throwable) {

        String cuentaId = movimiento == null || movimiento.getCuentaId() == null
                ? "SIN_ID"
                : movimiento.getCuentaId().toString();

        escribirError(
                "WRITE",
                cuentaId,
                "Error al persistir movimiento anual",
                throwable.getMessage());
    }

    private synchronized void escribirError(
            String etapa,
            String cuentaId,
            String registro,
            String motivo) {

        try {
            String linea = limpiar(etapa)
                    + ";" + limpiar(cuentaId)
                    + ";" + limpiar(registro)
                    + ";" + limpiar(motivo)
                    + System.lineSeparator();

            Files.writeString(
                    ERROR_FILE,
                    linea,
                    StandardOpenOption.APPEND);

            log.warn(
                    "[{}] SKIP LISTENER ESTADOS -> Etapa={}, cuenta={}, motivo={}",
                    Thread.currentThread().getName(),
                    etapa,
                    cuentaId,
                    motivo);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo registrar el error omitido de estados anuales.",
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
