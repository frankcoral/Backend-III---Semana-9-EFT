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

import com.bancoxyz.batchmigration.model.CuentaInteres;

@Component
public class CuentaInteresSkipListener
        implements SkipListener<CuentaInteres, CuentaInteres> {

    private static final Logger log = LoggerFactory.getLogger(CuentaInteresSkipListener.class);

    private static final Path ERROR_FILE = Path.of(
            "output",
            "errores_intereses.csv");

    public CuentaInteresSkipListener() {
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

            log.info("ARCHIVO ERRORES INTERESES REINICIADO -> {}", ERROR_FILE);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo crear el archivo de errores de intereses.",
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
            CuentaInteres cuenta,
            Throwable throwable) {

        String id = cuenta == null || cuenta.getCuentaId() == null
                ? "SIN_ID"
                : cuenta.getCuentaId().toString();

        String registro = cuenta == null
                ? "NO_DISPONIBLE"
                : "nombre=" + cuenta.getNombre()
                        + ", saldo=" + cuenta.getSaldo()
                        + ", edad=" + cuenta.getEdad()
                        + ", tipo=" + cuenta.getTipo();

        escribirError(
                "PROCESS",
                id,
                registro,
                throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(
            CuentaInteres cuenta,
            Throwable throwable) {

        String id = cuenta == null || cuenta.getCuentaId() == null
                ? "SIN_ID"
                : cuenta.getCuentaId().toString();

        escribirError(
                "WRITE",
                id,
                "Error al persistir cuenta",
                throwable.getMessage());
    }

    private synchronized void escribirError(
            String etapa,
            String id,
            String registro,
            String motivo) {

        try {
            String linea = limpiar(etapa)
                    + ";" + limpiar(id)
                    + ";" + limpiar(registro)
                    + ";" + limpiar(motivo)
                    + System.lineSeparator();

            Files.writeString(
                    ERROR_FILE,
                    linea,
                    StandardOpenOption.APPEND);

            log.warn(
                    "[{}] SKIP LISTENER INTERESES -> Etapa={}, cuenta={}, motivo={}",
                    Thread.currentThread().getName(),
                    etapa,
                    id,
                    motivo);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo registrar el error omitido de intereses.",
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
