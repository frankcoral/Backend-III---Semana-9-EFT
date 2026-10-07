package com.bancoxyz.batchmigration.exception;

/**
 * Excepción de negocio utilizada para datos de transacciones
 * que presentan inconsistencias controlables.
 *
 * Esta excepción será manejada mediante una SkipPolicy.
 */
public class TransaccionInvalidaException extends RuntimeException {

    public TransaccionInvalidaException(String mensaje) {
        super(mensaje);
    }

    public TransaccionInvalidaException(
            String mensaje,
            Throwable causa) {

        super(mensaje, causa);
    }
}