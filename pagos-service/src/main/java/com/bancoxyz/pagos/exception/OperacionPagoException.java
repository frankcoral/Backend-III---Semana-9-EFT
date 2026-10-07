package com.bancoxyz.pagos.exception;

public class OperacionPagoException extends RuntimeException {
    public OperacionPagoException(String message) {
        super(message);
    }

    public OperacionPagoException(String message, Throwable cause) {
        super(message, cause);
    }
}
