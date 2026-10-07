package com.bancoxyz.clientes.exception;

import com.bancoxyz.clientes.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(OperacionClienteException.class)
    ResponseEntity<ApiErrorResponse> conflicto(OperacionClienteException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(DependenciaNoDisponibleException.class)
    ResponseEntity<ApiErrorResponse> dependencia(DependenciaNoDisponibleException ex) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Datos de entrada invalidos", detalles);
    }

    private ResponseEntity<ApiErrorResponse> respuesta(HttpStatus status, String mensaje,
            Map<String, String> detalles) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensaje, detalles));
    }
}
