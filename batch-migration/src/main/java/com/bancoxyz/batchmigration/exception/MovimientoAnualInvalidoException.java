package com.bancoxyz.batchmigration.exception;

public class MovimientoAnualInvalidoException
        extends RuntimeException {

    public MovimientoAnualInvalidoException(
            String mensaje) {

        super(mensaje);
    }

    public MovimientoAnualInvalidoException(
            String mensaje,
            Throwable causa) {

        super(mensaje, causa);
    }
}