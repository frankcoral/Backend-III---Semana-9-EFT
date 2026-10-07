package com.bancoxyz.clientes.exception;

public class OperacionClienteException extends RuntimeException {
    public OperacionClienteException(String message) {
        super(message);
    }

    public OperacionClienteException(String message, Throwable cause) {
        super(message, cause);
    }
}
