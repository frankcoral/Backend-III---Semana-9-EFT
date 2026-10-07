package com.bancoxyz.cuentas.exception;

public class OperacionCuentaException extends RuntimeException {
    public OperacionCuentaException(String message) {
        super(message);
    }

    public OperacionCuentaException(String message, Throwable cause) {
        super(message, cause);
    }
}
