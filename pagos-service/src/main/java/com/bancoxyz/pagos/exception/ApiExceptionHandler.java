package com.bancoxyz.pagos.exception;

import com.bancoxyz.pagos.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ApiErrorResponse> notFound(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(OperacionPagoException.class)
    ResponseEntity<ApiErrorResponse> negocio(OperacionPagoException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler({ DependenciaNoDisponibleException.class, ConsistenciaDistribuidaException.class })
    ResponseEntity<ApiErrorResponse> dependencia(RuntimeException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Solicitud invalida");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    private ResponseEntity<ApiErrorResponse> respuesta(HttpStatus status, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), mensaje, request.getRequestURI()));
    }
}
