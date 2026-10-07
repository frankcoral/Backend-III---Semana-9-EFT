package com.bancoxyz.bffatm.error;

public class DependenciaNoDisponibleException extends RuntimeException {
    public DependenciaNoDisponibleException(String message) {
        super(message);
    }

    public DependenciaNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
