package com.bancoxyz.batchmigration.exception;

public class CuentaInteresInvalidaException
        extends RuntimeException {

    public CuentaInteresInvalidaException(
            String mensaje) {

        super(mensaje);
    }

    public CuentaInteresInvalidaException(
            String mensaje,
            Throwable causa) {

        super(mensaje, causa);
    }
}